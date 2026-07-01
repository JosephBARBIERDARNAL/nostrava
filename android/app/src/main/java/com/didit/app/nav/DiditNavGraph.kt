package com.didit.app.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.didit.app.ui.components.DiditScreenShell
import com.didit.app.ui.screens.ActivityHistoryScreen
import com.didit.app.ui.screens.ActivityHomeScreen
import com.didit.app.ui.screens.ActivitySummaryScreen
import com.didit.app.ui.screens.ActivityTrackScreen
import com.didit.app.ui.screens.AppSelectorScreen
import com.didit.app.ui.screens.GymHistoryScreen
import com.didit.app.ui.screens.GymHomeScreen
import com.didit.app.ui.screens.GymLogScreen
import com.didit.app.ui.screens.GymSummaryScreen

/** Push-and-pop-off-the-current-entry — the Compose equivalent of `navigate(route, {replace: true})`. */
fun NavHostController.replace(route: String) {
    val current = currentBackStackEntry?.destination?.route
    navigate(route) {
        if (current != null) popUpTo(current) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun DiditNavGraph() {
    val nav = rememberNavController()
    DiditScreenShell {
        NavHost(navController = nav, startDestination = "selector") {
            composable("selector") {
                AppSelectorScreen(nav)
            }
            composable("{activity}/home") { entry ->
                val activity = entry.arguments?.getString("activity") ?: return@composable
                ActivityHomeScreen(nav, activity)
            }
            composable("{activity}/track") { entry ->
                val activity = entry.arguments?.getString("activity") ?: return@composable
                ActivityTrackScreen(nav, activity)
            }
            composable(
                "{activity}/summary/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val activity = entry.arguments?.getString("activity") ?: return@composable
                val id = entry.arguments?.getLong("id") ?: return@composable
                ActivitySummaryScreen(nav, activity, id)
            }
            composable("{activity}/history") { entry ->
                val activity = entry.arguments?.getString("activity") ?: return@composable
                ActivityHistoryScreen(nav, activity)
            }
            composable("gym/home") {
                GymHomeScreen(nav)
            }
            composable("gym/log") {
                GymLogScreen(nav)
            }
            composable(
                "gym/summary/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                GymSummaryScreen(nav, id)
            }
            composable("gym/history") {
                GymHistoryScreen(nav)
            }
        }
    }
}
