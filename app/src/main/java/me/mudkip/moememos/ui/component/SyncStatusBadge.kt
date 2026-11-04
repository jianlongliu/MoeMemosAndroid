package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

/**
 * Tonal status pill for the app bar: synced, syncing, or pending changes.
 * Tapping it starts a manual sync.
 */
@Composable
fun SyncStatusBadge(
    syncing: Boolean,
    unsyncedCount: Int,
    onSync: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = when {
        syncing -> colors.surfaceContainerHighest
        unsyncedCount > 0 -> colors.errorContainer
        else -> colors.surfaceContainerHigh
    }
    val content = when {
        syncing -> colors.onSurfaceVariant
        unsyncedCount > 0 -> colors.onErrorContainer
        else -> colors.onSurfaceVariant
    }
    val label = when {
        syncing -> R.string.sync_status_syncing.string
        unsyncedCount > 0 -> R.string.sync_status_pending.string.format(unsyncedCount)
        else -> R.string.sync_status_synced.string
    }

    Surface(
        onClick = onSync,
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            } else {
                Icon(
                    imageVector = when {
                        unsyncedCount > 0 -> Icons.Outlined.CloudOff
                        else -> Icons.Outlined.CloudDone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
