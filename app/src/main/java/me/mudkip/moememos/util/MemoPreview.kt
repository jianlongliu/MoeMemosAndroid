package me.mudkip.moememos.util

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

private const val PREVIEW_UNBREAKABLE_COST = 100

/**
 * Default preview budget, measured in display width units rather than characters.
 * See [extractPreviewContent].
 */
private const val PREVIEW_MAX_WIDTH = 500

// Parsing an entire long memo just to extract a 500-unit preview is wasted work on
// every card composition. One thousand characters is still beyond the
// 500-unit preview budget even for CJK text, while keeping the parser input small.
private const val PREVIEW_MAX_INPUT_CHARS = 1024

private enum class PreviewAppendKind {
    NONE,
    TEXT,
    UNBREAKABLE
}

/**
 * Extracts a preview of [markdownText] limited to [maxWidth] display width units, together with
 * whether any content was dropped.
 *
 * The budget is measured in *display width*, not in characters: a full-width character (CJK) takes
 * two units, so the same 500 unit budget yields roughly 500 Latin characters or 250 CJK ones. That
 * keeps a preview about the same number of lines whatever the language is, instead of letting CJK
 * memos — where every character is twice as wide — show a preview about three times as long.
 *
 * Characters are never split: the returned preview always ends on a code point boundary.
 */
fun extractPreviewContent(markdownText: String, maxWidth: Int = PREVIEW_MAX_WIDTH): Pair<String, Boolean> {
    val inputTruncated = markdownText.length > PREVIEW_MAX_INPUT_CHARS
    val source = if (inputTruncated) {
        markdownText.substring(0, PREVIEW_MAX_INPUT_CHARS).substringBeforeLast('\n').ifEmpty {
            markdownText.substring(0, PREVIEW_MAX_INPUT_CHARS)
        }
    } else {
        markdownText
    }
    val node = parseMarkdown(source)

    val result = StringBuilder()
    var remainingWidth = maxWidth
    var truncated = inputTruncated
    var lastAppendKind = PreviewAppendKind.NONE

    fun appendNodeText(child: ASTNode): Boolean {
        if (remainingWidth <= 0) {
            truncated = true
            return false
        }
        val content = source.substring(child.startOffset, child.endOffset)
        if (content.isEmpty()) {
            return true
        }
        val contentWidth = previewWidth(content)
        if (contentWidth <= remainingWidth) {
            result.append(content)
            remainingWidth -= contentWidth
            lastAppendKind = PreviewAppendKind.TEXT
            return true
        }
        result.append(previewPrefix(content, remainingWidth))
        remainingWidth = 0
        truncated = true
        lastAppendKind = PreviewAppendKind.TEXT
        return false
    }

    fun appendUnbreakableNode(child: ASTNode): Boolean {
        if (remainingWidth < PREVIEW_UNBREAKABLE_COST) {
            truncated = true
            return false
        }
        result.append(source.substring(child.startOffset, child.endOffset))
        remainingWidth -= PREVIEW_UNBREAKABLE_COST
        lastAppendKind = PreviewAppendKind.UNBREAKABLE
        return true
    }

    lateinit var extractNodeContent: (ASTNode) -> Boolean
    lateinit var extractBlockContent: (ASTNode) -> Boolean

    extractNodeContent = { child ->
        if (isUnbreakablePreviewNode(child)) {
            appendUnbreakableNode(child)
        } else if (child.children.isEmpty()) {
            appendNodeText(child)
        } else {
            var allSuccess = true
            for (grandChild in child.children) {
                val success = if (isBreakablePreviewBlock(grandChild.type)) {
                    extractBlockContent(grandChild)
                } else {
                    extractNodeContent(grandChild)
                }
                if (!success) {
                    allSuccess = false
                    break
                }
            }
            allSuccess
        }
    }

    extractBlockContent = { child ->
        var allSuccess = true
        val isParagraph = child.type == MarkdownElementTypes.PARAGRAPH
        for (grandChild in child.children) {
            val success = if (isParagraph) {
                extractNodeContent(grandChild)
            } else if (isBreakablePreviewBlock(grandChild.type)) {
                extractBlockContent(grandChild)
            } else if (isPreviewWhitespaceToken(grandChild) || grandChild.children.isEmpty()) {
                appendNodeText(grandChild)
            } else {
                appendUnbreakableNode(grandChild)
            }
            if (!success) {
                allSuccess = false
                break
            }
        }
        allSuccess
    }

    for (child in node.children) {
        val success = if (isBreakablePreviewBlock(child.type)) {
            extractBlockContent(child)
        } else if (isPreviewWhitespaceToken(child)) {
            appendNodeText(child)
        } else {
            appendUnbreakableNode(child)
        }
        if (!success) {
            break
        }
    }

    if (truncated && lastAppendKind == PreviewAppendKind.TEXT) {
        val preview = result.toString().trimEnd()
        val withEllipsis = if (preview.endsWith("…")) preview else "$preview…"
        return Pair(withEllipsis, true)
    }

    return Pair(result.toString(), truncated)
}

/**
 * Width of [text] in preview units: 2 for each full-width character, 1 otherwise.
 * Iterates over code points, so a surrogate pair counts once.
 */
internal fun previewWidth(text: String): Int {
    var width = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        width += codePointPreviewWidth(codePoint)
        index += Character.charCount(codePoint)
    }
    return width
}

/**
 * Longest prefix of [text] that fits into [maxWidth] preview units.
 * A full-width character that does not fit is left out entirely, and a surrogate pair is never split.
 */
internal fun previewPrefix(text: String, maxWidth: Int): String {
    var width = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val codePointWidth = codePointPreviewWidth(codePoint)
        if (width + codePointWidth > maxWidth) {
            break
        }
        width += codePointWidth
        index += Character.charCount(codePoint)
    }
    return text.substring(0, index)
}

private fun codePointPreviewWidth(codePoint: Int): Int = if (isFullWidthCodePoint(codePoint)) 2 else 1

private val FULL_WIDTH_CODE_POINT_RANGES = listOf(
    0x1100..0x115F,   // Hangul Jamo
    0x2E80..0x303E,   // CJK radicals, Kangxi radicals, CJK symbols and punctuation
    0x3041..0x33FF,   // Hiragana, Katakana, CJK compatibility
    0x3400..0x4DBF,   // CJK unified ideographs extension A
    0x4E00..0x9FFF,   // CJK unified ideographs
    0xA000..0xA4CF,   // Yi syllables and radicals
    0xAC00..0xD7A3,   // Hangul syllables
    0xF900..0xFAFF,   // CJK compatibility ideographs
    0xFE10..0xFE19,   // vertical forms
    0xFE30..0xFE6F,   // CJK compatibility forms, small form variants
    0xFF00..0xFF60,   // fullwidth forms
    0xFFE0..0xFFE6,   // fullwidth signs
    0x20000..0x3FFFD, // CJK unified ideographs extension B and beyond
)

/** Full-width and wide East Asian characters, following the usual East Asian Width "W"/"F" ranges. */
private fun isFullWidthCodePoint(codePoint: Int): Boolean =
    FULL_WIDTH_CODE_POINT_RANGES.any { codePoint in it }

private fun isBreakablePreviewBlock(type: IElementType): Boolean {
    return type == MarkdownElementTypes.MARKDOWN_FILE ||
        type == MarkdownElementTypes.PARAGRAPH ||
        type == MarkdownElementTypes.LIST_ITEM ||
        type == MarkdownElementTypes.BLOCK_QUOTE ||
        type == MarkdownElementTypes.ORDERED_LIST ||
        type == MarkdownElementTypes.UNORDERED_LIST
}

private fun isUnbreakablePreviewNode(node: ASTNode): Boolean {
    return node.type == MarkdownElementTypes.IMAGE ||
        node.type == MarkdownElementTypes.CODE_BLOCK ||
        node.type == MarkdownElementTypes.CODE_FENCE ||
        node.type == GFMElementTypes.TABLE ||
        node.type == MarkdownElementTypes.ATX_1 ||
        node.type == MarkdownElementTypes.ATX_2 ||
        node.type == MarkdownElementTypes.ATX_3 ||
        node.type == MarkdownElementTypes.ATX_4 ||
        node.type == MarkdownElementTypes.ATX_5 ||
        node.type == MarkdownElementTypes.ATX_6 ||
        node.type == MarkdownElementTypes.SETEXT_1 ||
        node.type == MarkdownElementTypes.SETEXT_2 ||
        node.type == MarkdownTokenTypes.HORIZONTAL_RULE ||
        node.type == MarkdownElementTypes.LINK_DEFINITION ||
        node.type.toString().contains("HTML")
}

private fun isPreviewWhitespaceToken(node: ASTNode): Boolean {
    return node.type == MarkdownTokenTypes.EOL || node.type == MarkdownTokenTypes.WHITE_SPACE
}

private val HEADING_PATTERN = Regex("(?m)^#{1,6}\\s*")
private val UNCHECKED_TASK_PATTERN = Regex("(?m)^\\s*[-*+]\\s+\\[ \\]\\s*")
private val CHECKED_TASK_PATTERN = Regex("(?m)^\\s*[-*+]\\s+\\[[xX]\\]\\s*")
private val BULLET_PATTERN = Regex("(?m)^\\s*[-*+]\\s+")
private val QUOTE_PATTERN = Regex("(?m)^>\\s*")
private val IMAGE_PATTERN = Regex("!\\[([^\\]]*)\\]\\([^)]*\\)")
private val LINK_PATTERN = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
private val INLINE_CODE_PATTERN = Regex("`([^`]*)`")
private val FENCE_PATTERN = Regex("```[^\\n]*\\n?")
private val EMPHASIS_PATTERN = Regex("[*_~]{1,3}")

/**
 * Lightweight fallback for list previews. The Markdown renderer is deliberately
 * kept for detail/explore views: invoking it for every off-screen list row is
 * what made long memo feeds janky.
 */
fun markdownPreviewText(markdown: String): String {
    return markdown
        .replace(FENCE_PATTERN, "")
        .replace(INLINE_CODE_PATTERN, "$1")
        .replace(IMAGE_PATTERN, "$1")
        .replace(LINK_PATTERN, "$1")
        .replace(HEADING_PATTERN, "")
        .replace(UNCHECKED_TASK_PATTERN, "☐ ")
        .replace(CHECKED_TASK_PATTERN, "☑ ")
        .replace(BULLET_PATTERN, "• ")
        .replace(QUOTE_PATTERN, "")
        .replace(EMPHASIS_PATTERN, "")
        .trim()
}
