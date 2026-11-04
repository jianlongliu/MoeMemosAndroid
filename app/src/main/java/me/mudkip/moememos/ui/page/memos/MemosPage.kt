package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.ui.component.DockItem
import me.mudkip.moememos.ui.component.FloatingDock
import me.mudkip.moememos.ui.component.SideDrawer
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.ui.theme.moePageColor
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import me.mudkip.moememos.ui.theme.LocalHazeState
import me.mudkip.moememos.ui.theme.rememberMoeHazeStyle

private data class BottomDestination(
    val route: String,
    val label: Int,
    val icon: ImageVector,
)

@Composable
fun MemosPage() {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val permanent = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val memosNavController = rememberNavController()
    val rootNavController = LocalRootNavController.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val shellHazeState = rememberHazeState()
    val hazeStyle = rememberMoeHazeStyle()

    val account by LocalUserState.current.currentAccount.collectAsStateWithLifecycle()
    val nestedEntry by memosNavController.currentBackStackEntryAsState()
    val rootEntry by rootNavController.currentBackStackEntryAsState()
    val nestedRoute = nestedEntry?.destination?.route
    val rootRoute = rootEntry?.destination?.route

    val destinations = buildList {
        add(BottomDestination(RouteName.MEMOS, R.string.memos, Icons.Filled.GridView))
        if (account !is Account.Local) {
            add(BottomDestination(RouteName.EXPLORE, R.string.explore, Icons.Filled.Home))
        }
        add(BottomDestination(RouteName.ARCHIVED, R.string.archived, Icons.Filled.Inventory2))
    }

    fun select(destination: BottomDestination) {
        when (destination.route) {
            RouteName.SETTINGS -> rootNavController.navigate(RouteName.SETTINGS) {
                launchSingleTop = true
            }
            RouteName.MEMOS -> {
                if (rootRoute == RouteName.SETTINGS) {
                    rootNavController.popBackStack(RouteName.MEMOS, false)
                }
                memosNavController.navigate(RouteName.MEMOS) {
                    popUpTo(RouteName.MEMOS) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            else -> {
                if (rootRoute == RouteName.SETTINGS) {
                    rootNavController.popBackStack(RouteName.MEMOS, false)
                }
                memosNavController.navigate(destination.route) {
                    popUpTo(RouteName.MEMOS) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    CompositionLocalProvider(LocalHazeState provides shellHazeState) {
    MemosNavigationDrawer(
        drawerState = drawerState,
        hazeState = shellHazeState,
        hazeStyle = hazeStyle,
        permanent = permanent,
        drawerContent = { state ->
            SideDrawer(memosNavController = memosNavController, drawerState = state)
        },
    ) { state ->
        Scaffold(
            containerColor = moePageColor(),
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                MemosNavigation(drawerState = state, navController = memosNavController)
                if (!permanent) {
                    FloatingDock(
                        items = destinations.map { destination ->
                            DockItem(
                                label = stringResource(destination.label),
                                icon = destination.icon,
                            )
                        },
                        selectedIndex = destinations.indexOfFirst { destination ->
                            when (destination.route) {
                                RouteName.SETTINGS -> rootRoute == RouteName.SETTINGS
                                else -> rootRoute != RouteName.SETTINGS && nestedRoute == destination.route
                            }
                        },
                        onSelect = { index -> select(destinations[index]) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
    }
}

@Composable
internal fun MemosNavigationDrawer(
    drawerState: DrawerState,
    hazeState: dev.chrisbanes.haze.HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    permanent: Boolean,
    drawerContent: @Composable (DrawerState?) -> Unit,
    content: @Composable (DrawerState?) -> Unit,
) {
    val currentContent = rememberUpdatedState(content)
    // Move the same composition between drawer layouts so navigation and pane
    // selection survive resizing across the permanent-drawer breakpoint.
    val navigation = remember {
        movableContentOf<DrawerState?> { state -> currentContent.value(state) }
    }
    if (permanent) {
        PermanentNavigationDrawer(
            drawerContent = { PermanentDrawerSheet { drawerContent(null) } },
        ) { navigation(null) }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    modifier = Modifier.hazeEffect(state = hazeState, style = hazeStyle),
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    drawerContainerColor = Color.Transparent,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    drawerTonalElevation = 0.dp,
                ) { drawerContent(drawerState) }
            },
        ) { navigation(drawerState) }
    }
}
