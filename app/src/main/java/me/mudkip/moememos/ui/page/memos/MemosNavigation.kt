package me.mudkip.moememos.ui.page.memos

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.ui.page.common.MemosNavHost
import me.mudkip.moememos.viewmodel.LocalUserState

@Composable
fun MemosNavigation(
    drawerState: DrawerState? = null,
    navController: NavHostController
) {
    val userStateViewModel = LocalUserState.current
    val currentAccount by userStateViewModel.currentAccount.collectAsStateWithLifecycle()
    val hasExplore = currentAccount !is Account.Local

    MemosNavHost(
        navController = navController,
        startDestination = RouteName.MEMOS
    ) {
        composable(
            RouteName.MEMOS,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(140)) },
        ) {
            MemosHomePage(
                drawerState = drawerState,
                navController = navController,
            )
        }

        composable(
            RouteName.ARCHIVED,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(140)) },
        ) {
            ArchivedMemoPage(
                drawerState = drawerState
            )
        }

        composable(
            "${RouteName.TAG}/{tag}"
        ) { entry ->
            TagMemoPage(
                drawerState = drawerState,
                tag = entry.arguments?.getString("tag")?.let(Uri::decode) ?: "",
                navController = navController
            )
        }

        composable(
            RouteName.EXPLORE,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(140)) },
        ) {
            if (hasExplore) {
                ExplorePage(
                    drawerState = drawerState
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(RouteName.MEMOS) {
                        popUpTo(RouteName.EXPLORE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(RouteName.SEARCH) {
            SearchPage(navController = navController)
        }
    }
}
