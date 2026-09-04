package com.sm.myapplication.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.myapplication.ui.screens.calendar.CalendarScreen
import com.sm.myapplication.ui.screens.dday.DDayDialog
import com.sm.myapplication.ui.screens.home.MainHomeScreen
import com.sm.myapplication.ui.screens.login.LoginScreen
import com.sm.myapplication.ui.screens.puremode.PureModeScreen
import com.sm.myapplication.ui.screens.rank.RankScreen
import com.sm.myapplication.ui.screens.rank.TierRankDialog
import com.sm.myapplication.ui.screens.setting.SettingScreen
import com.sm.myapplication.ui.screens.timer.BasicModeScreen
import com.sm.myapplication.ui.screens.todo.TodoAddScreen
import com.sm.myapplication.ui.screens.todo.TodoListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.LOGIN) {
        composable(Route.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Route.HOME) {
                    popUpTo(Route.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Route.HOME) { MainScaffold(navController, Route.HOME) }
        composable(Route.CALENDAR) { MainScaffold(navController, Route.CALENDAR) }
        composable(Route.RANK) { MainScaffold(navController, Route.RANK) }
        composable(Route.SETTING) { MainScaffold(navController, Route.SETTING) }

        composable(Route.BASIC_MODE) { BasicModeScreen(onBack = { navController.popBackStack() }) }
        composable(Route.PURE_MODE) { PureModeScreen(onBack = { navController.popBackStack() }) }
        composable(Route.TODO_LIST) { TodoListScreen(
            onBack = { navController.popBackStack() },
            onAdd = { navController.navigate(Route.TODO_ADD) },
        ) }
        composable(Route.TODO_ADD) { TodoAddScreen(onBack = { navController.popBackStack() }) }
        composable(Route.TIER_RANK) { TierRankDialog(onClose = { navController.popBackStack() }) }
        composable(Route.DDAY) { DDayDialog(onClose = { navController.popBackStack() }) }
    }
}

@Composable
private fun MainScaffold(navController: NavHostController, currentTab: String) {
    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = currentTab,
                onTabSelected = { route ->
                    if (route != currentTab) {
                        navController.navigate(route) {
                            popUpTo(Route.HOME) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.background)) {
            when (currentTab) {
                Route.HOME -> MainHomeScreen(
                    onStartBasicMode = { navController.navigate(Route.BASIC_MODE) },
                    onStartPureMode = { navController.navigate(Route.PURE_MODE) },
                    onOpenTodos = { navController.navigate(Route.TODO_LIST) },
                    onOpenDDay = { navController.navigate(Route.DDAY) },
                    onOpenTier = { navController.navigate(Route.TIER_RANK) },
                )
                Route.CALENDAR -> CalendarScreen()
                Route.RANK -> RankScreen(onOpenTier = { navController.navigate(Route.TIER_RANK) })
                Route.SETTING -> SettingScreen(onLogout = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(0)
                    }
                })
            }
        }
    }
}

