package me.mudkip.moememos.ui.page.memos

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import me.mudkip.moememos.ui.component.SideDrawer

@Composable
fun MemosPage() {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val memosNavController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    MemosNavigationDrawer(
        drawerState = drawerState,
        permanent = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),
        drawerContent = { state ->
            SideDrawer(memosNavController = memosNavController, drawerState = state)
        },
    ) { state ->
        MemosNavigation(drawerState = state, navController = memosNavController)
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
