package me.mudkip.moememos.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val selectedTint = colors.onSurface
    val unselectedTint = colors.onSurfaceVariant
    val shape = RoundedCornerShape(31.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 21.dp, vertical = 8.dp)
            .height(62.dp),
        shape = shape,
        color = dockColor,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(if (selected) lensColor else Color.Transparent)
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected) selectedTint else unselectedTint,
                    )
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) selectedTint else unselectedTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
