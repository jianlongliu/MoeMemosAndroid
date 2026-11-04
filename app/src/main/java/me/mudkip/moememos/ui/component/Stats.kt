package me.mudkip.moememos.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.theme.moeCardColor
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
fun Stats() {
    val memosViewModel = LocalMemos.current
    val userStateViewModel = LocalUserState.current
    val days = remember(userStateViewModel.currentUser, LocalDate.now()) {
        userStateViewModel.currentUser?.let { currentUser ->
            ChronoUnit.DAYS.between(
                currentUser.startDate.atZone(OffsetDateTime.now().offset).toLocalDate(),
                LocalDate.now(),
            )
        } ?: 0
    }
    val lastWeek = remember(memosViewModel.matrix) { memosViewModel.matrix.takeLast(7) }
    val weekTotal = lastWeek.sumOf { it.count }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Insights,
                        contentDescription = null,
                        modifier = Modifier.height(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        R.string.stats_last_7_days.string,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        R.string.stats_week_total.string.format(weekTotal),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Sparkline(
                    values = lastWeek.map { it.count },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                value = memosViewModel.memos.count().toString(),
                label = R.string.memo.string.uppercase(),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = memosViewModel.tags.count().toString(),
                label = R.string.tag.string.uppercase(),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                value = days.toString(),
                label = R.string.day.string.uppercase(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = moeCardColor(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Minimal 7-day activity sparkline with a soft tonal fill. */
@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size > 1) stepX * index else size.width / 2,
                y = size.height - (value.toFloat() / max) * (size.height - 6.dp.toPx()) - 3.dp.toPx(),
            )
        }

        drawLine(
            color = baselineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        val fill = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { point -> lineTo(point.x, point.y) }
            lineTo(points.last().x, size.height)
            close()
        }

        drawPath(fill, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
        drawPath(line, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { point ->
            drawCircle(color = lineColor, radius = 2.dp.toPx(), center = point)
        }
    }
}
