package me.mudkip.moememos.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Ambient Material You backdrop for the app shell.
 *
 * The tonal blooms are drawn into one layer and blurred with
 * [androidx.compose.ui.draw.blur], which is Compose's surface blur. Keeping the
 * blur here (instead of on every card) follows the Compose blur guidance and
 * avoids paying for a render effect on every list row.
 */
@Composable
fun MoeBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .blur(40.dp),
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.24f),
                        colors.tertiaryContainer.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                ),
            )
        }
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val topCenter = Offset(size.width * 0.12f, size.height * 0.08f)
            val bottomCenter = Offset(size.width * 0.92f, size.height * 0.88f)
            val topRadius = size.minDimension * 0.72f
            val bottomRadius = size.minDimension * 0.64f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.28f), Color.Transparent),
                    center = topCenter,
                    radius = topRadius,
                ),
                radius = topRadius,
                center = topCenter,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.tertiaryContainer.copy(alpha = 0.34f), Color.Transparent),
                    center = bottomCenter,
                    radius = bottomRadius,
                ),
                radius = bottomRadius,
                center = bottomCenter,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.secondaryContainer.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.28f),
                    radius = size.minDimension * 0.42f,
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.78f, size.height * 0.28f),
            )
        }
    }
}

/** The Haze source for the current navigation shell, if blur is available. */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Material 3 surface tint for Haze-backed top bars, drawers, and sheets. */
@Composable
fun rememberMoeHazeStyle(): HazeStyle {
    val colors = MaterialTheme.colorScheme
    return remember(colors.surface, colors.primary) {
        HazeStyle(
            backgroundColor = colors.surface,
            tint = HazeTint(colors.surface.copy(alpha = 0.30f)),
            blurRadius = 28.dp,
            noiseFactor = 0.05f,
            fallbackTint = HazeTint(colors.surface.copy(alpha = 0.88f)),
        )
    }
}

/** Shared Material 3 top app bar colors for the blurred app shell. */
@Composable
fun moeTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    scrolledContainerColor = MaterialTheme.colorScheme.surface,
)
