package com.gravitysiege.gravitysiegegame.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gravitysiege.gravitysiegegame.GameStore
import com.gravitysiege.gravitysiegegame.audio.Sfx
import com.gravitysiege.gravitysiegegame.ui.screens.GameScreen
import com.gravitysiege.gravitysiegegame.ui.screens.LoadingScreen
import com.gravitysiege.gravitysiegegame.ui.screens.MenuScreen
import com.gravitysiege.gravitysiegegame.ui.screens.SettingsScreen
import com.gravitysiege.gravitysiegegame.ui.screens.WebDocumentScreen

object Routes {
    const val LOADING = "loading"
    const val MENU = "menu"
    const val GAME = "game"
    const val SETTINGS = "settings"
    const val WEB = "web"
}

@Composable
fun AppNav(store: GameStore, sfx: Sfx, activity: Activity) {
    val nav = rememberNavController()
    val open: (String) -> Unit = remember(nav, sfx) {
        { route ->
            sfx.open()
            nav.navigate(route)
        }
    }
    val back: () -> Unit = remember(nav, sfx) {
        {
            sfx.back()
            nav.popBackStack()
        }
    }

    NavHost(navController = nav, startDestination = Routes.LOADING) {
        composable(Routes.LOADING) {
            LoadingScreen(activity) {
                nav.navigate(Routes.MENU) {
                    popUpTo(Routes.LOADING) { inclusive = true }
                }
            }
        }
        composable(Routes.MENU) { MenuScreen(store, sfx, open) }
        composable(Routes.GAME) { GameScreen(store, sfx, back) }
        composable(Routes.SETTINGS) { SettingsScreen(store, sfx, open, back) }
        composable(
            route = "${Routes.WEB}/{page}",
            arguments = listOf(navArgument("page") { type = NavType.StringType }),
        ) { entry ->
            WebDocumentScreen(entry.arguments?.getString("page").orEmpty(), back)
        }
    }
}
