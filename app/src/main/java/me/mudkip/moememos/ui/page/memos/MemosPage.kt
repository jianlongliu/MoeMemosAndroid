package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import me.mudkip.moememos.ui.component.SideDrawer
import me.mudkip.moememos.ui.page.common.LocalRootNavController
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalUserState

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

    val account by LocalUserState.current.currentAccount.collectAsStateWithLifecycle()
    val nestedEntry by memosNavController.currentBackStackEntryAsState()
    val rootEntry by rootNavController.currentBackStackEntryAsState()
    val nestedRoute = nestedEntry?.destination?.route
    val rootRoute = rootEntry?.destination?.route

    val destinations = buildList {
        add(BottomDestination(RouteName.MEMOS, R.string.memos, Icons.Outlined.GridView))
        if (account !is Account.Local) {
            add(BottomDestination(RouteName.EXPLORE, R.string.explore, Icons.Outlined.Home))
        }
        add(BottomDestination(RouteName.ARCHIVED, R.string.archived, Icons.Outlined.Inventory2))
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

    MemosNavigationDrawer(
        drawerState = drawerState,
        permanent = permanent,
        drawerContent = { state ->
            SideDrawer(memosNavController = memosNavController, drawerState = state)
        },
    ) { state ->
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!permanent) {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val selected = when (destination.route) {
                                RouteName.SETTINGS -> rootRoute == RouteName.SETTINGS
                                else -> rootRoute != RouteName.SETTINGS && nestedRoute == destination.route
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { select(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                MemosNavigation(drawerState = state, navController = memosNavController)
            }
        }
    }
}

@Composable
internal fun MemosNavigationDrawer(
    drawerState: DrawerState,
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
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    drawerTonalElevation = 0.dp,
                ) { drawerContent(drawerState) }
            },
        ) { navigation(drawerState) }
    }
}
