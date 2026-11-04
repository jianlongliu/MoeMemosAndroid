package me.mudkip.moememos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.hazeEffect
import me.mudkip.moememos.ui.theme.LocalHazeState
import me.mudkip.moememos.ui.theme.rememberMoeHazeStyle

data class DockItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating navigation dock sized after FlClash's bar: 62dp tall, 21dp side
 * margins, fully rounded, with a tonal lens under the selected destination.
 */
@Composable
fun FloatingDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val dockColor = if (dark) colors.surfaceContainerHigh else colors.surfaceBright
    val lensColor = if (dark) colors.surfaceContainerHighest else colors.surfaceContainer
    val selectedTint = colors.primary
    val unselectedTint = colors.onSurface
    val shape = RoundedCornerShape(31.dp)
    val hazeState = LocalHazeState.current
    val hazeStyle = rememberMoeHazeStyle()

    val maxDockWidth = (items.size * 86 + 16).dp

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.widthIn(max = maxDockWidth),
    ) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = dockColor.copy(alpha = if (hazeState != null) 0.62f else 1f),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            colors.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(4.dp),
        ) {
            val itemWidth = maxWidth / items.size
            val lensOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0),
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "dockLens",
            )

            if (selectedIndex >= 0) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .offset(x = lensOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(lensColor),
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        targetValue = if (selected) selectedTint else unselectedTint,
                        label = "dockTint",
                    )
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (pressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
                        label = "dockPress",
                    )
                    val selectedScale by animateFloatAsState(
                        targetValue = if (selected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                        label = "dockScale",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .clickable(
                                interactionSource = interaction,
                                indication = LocalIndication.current,
                                onClick = { onSelect(index) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(26.dp)
                                .scale(selectedScale * pressScale),
                            tint = tint,
                        )
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    }
    }
}
