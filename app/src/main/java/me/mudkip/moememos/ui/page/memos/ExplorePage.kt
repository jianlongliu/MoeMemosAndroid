package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.theme.LocalHazeState
import me.mudkip.moememos.ui.theme.rememberMoeHazeStyle
import me.mudkip.moememos.ui.theme.moePageColor
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorePage(
    drawerState: DrawerState? = null
) {
    val scope = rememberCoroutineScope()
    val ownHazeState = rememberHazeState()
    val hazeState = LocalHazeState.current ?: ownHazeState
    val hazeStyle = rememberMoeHazeStyle()

    Scaffold(
        containerColor = moePageColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = { innerPadding ->
            CompositionLocalProvider(LocalHazeState provides hazeState) {
                Box(Modifier.fillMaxSize()) {
                    ExploreList(
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopAppBarDefaults.TopAppBarExpandedHeight + 12.dp,
                            bottom = innerPadding.calculateBottomPadding(),
                        )
                    )
                    TopAppBar(
                        modifier = Modifier.align(Alignment.TopCenter).hazeEffect(hazeState, hazeStyle),
                        title = { Text(text = R.string.explore.string) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                        navigationIcon = {
                            if (drawerState != null) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = R.string.menu.string)
                                }
                            }
                        }
                    )
                    }
            }
        }
    )
}
