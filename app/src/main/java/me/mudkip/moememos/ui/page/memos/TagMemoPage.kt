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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.ActionIconButton
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.ui.theme.rememberMoeHazeStyle

@Composable
fun TagMemoPage(
    drawerState: DrawerState? = null,
    tag: String,
    navController: NavHostController
) {
    MemoBrowser { onMemoClick ->
        TagMemoPageContent(drawerState, tag, navController, onMemoClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagMemoPageContent(
    drawerState: DrawerState? = null,
    tag: String,
    navController: NavHostController,
    onMemoClick: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val hazeStyle = rememberMoeHazeStyle()
    val normalizedCurrentTag = remember(tag) { normalizeTag(tag) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                MemosList(
                    hazeState = hazeState,
                    onMemoClick = onMemoClick,
                    contentPadding = PaddingValues(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopAppBarDefaults.TopAppBarExpandedHeight,
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                    tag = tag,
                    onTagClick = { clickedTag ->
                        if (normalizeTag(clickedTag) == normalizedCurrentTag) {
                            return@MemosList
                        }
                        navController.navigate("${RouteName.TAG}/${java.net.URLEncoder.encode(clickedTag, "UTF-8")}") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                TopAppBar(
                    modifier = Modifier.align(Alignment.TopCenter).hazeEffect(hazeState, hazeStyle),
                    title = { Text(tag) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        if (drawerState != null) {
                            ActionIconButton(label = R.string.menu.string, onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = R.string.menu.string)
                            }
                        }
                    },
                )
            }
        }
    )
}

private fun normalizeTag(tag: String): String {
    return tag.removePrefix("#")
}
