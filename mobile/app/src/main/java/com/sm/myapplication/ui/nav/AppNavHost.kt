package com.sm.myapplication.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.network.ApiClient
import com.sm.myapplication.network.JwtUtils
import com.sm.myapplication.ui.screens.calendar.CalendarScreen
import com.sm.myapplication.ui.screens.dday.DDayDialog
import com.sm.myapplication.ui.screens.home.MainHomeScreen
import com.sm.myapplication.ui.screens.login.LoginScreen
import com.sm.myapplication.ui.screens.puremode.PureModeScreen
import com.sm.myapplication.ui.screens.rank.RankScreen
import com.sm.myapplication.ui.screens.rank.TierRankDialog
import com.sm.myapplication.ui.screens.setting.HelpScreen
import com.sm.myapplication.ui.screens.setting.MyInfoScreen
import com.sm.myapplication.ui.screens.setting.PrivacyScreen
import com.sm.myapplication.ui.screens.setting.SettingScreen
import com.sm.myapplication.ui.screens.timer.BasicModeScreen
import com.sm.myapplication.ui.screens.todo.TodoAddScreen
import com.sm.myapplication.ui.screens.todo.TodoListScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 저장된 토큰이 살아있으면 로그인 화면을 건너뛴다. 읽기 전에는 null이라 NavHost를 만들지 않는다(로그인 화면 깜빡임 방지).
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val repo = AppRepository.get(context.applicationContext)
        val token = repo.accessToken.first()
        startDestination = if (JwtUtils.isNotExpired(token)) {
            // 로그인된 상태로 앱을 켜면 서버와 Todo/D-Day를 맞춘다(백그라운드).
            repo.syncInBackground()
            Route.HOME
        } else {
            // 만료된 토큰이 남아 있으면 같이 정리한다.
            if (token != null) repo.logout()
            Route.LOGIN
        }
    }

    // 토큰을 붙였는데 서버가 401로 거절하면(서명 불일치/서버 초기화 등) 세션을 정리하고 로그인 화면으로 보낸다.
    LaunchedEffect(startDestination) {
        if (startDestination == null) return@LaunchedEffect
        ApiClient.unauthorized.collect {
            if (navController.currentDestination?.route != Route.LOGIN) {
                AppRepository.get(context.applicationContext).logout()
                navController.navigate(Route.LOGIN) { popUpTo(0) }
            }
        }
    }

    val start = startDestination
    if (start == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.HOME) { MainScaffold(navController, Route.HOME) }
        composable(Route.CALENDAR) { MainScaffold(navController, Route.CALENDAR) }
        composable(Route.RANK) { MainScaffold(navController, Route.RANK) }
        composable(Route.SETTING) { MainScaffold(navController, Route.SETTING) }

        composable(Route.SETTING_MY_INFO) {
            MyInfoScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.SETTING_PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.SETTING_HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.BASIC_MODE) {
            BasicModeScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.PURE_MODE) {
            PureModeScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.TODO_LIST) {
            TodoListScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Route.TODO_ADD) },
            )
        }

        composable(Route.TODO_ADD) {
            TodoAddScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.TIER_RANK) {
            TierRankDialog(onClose = { navController.popBackStack() })
        }

        composable(Route.DDAY) {
            DDayDialog(onClose = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    currentTab: String
) {
    // 로그아웃에서 suspend 함수(logout)를 실행하기 위한 CoroutineScope
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var userName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        userName = AppPreferences(context.applicationContext).memberName.first()
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = currentTab,
                onTabSelected = { route ->
                    if (route != currentTab) {
                        navController.navigate(route) {
                            popUpTo(Route.HOME) {
                                inclusive = false
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        // 설정 탭은 초록 헤더가 상태바 뒤까지 올라가므로 상단 인셋을 직접 처리한다
        contentWindowInsets = if (currentTab == Route.SETTING) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.statusBars
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                Route.HOME -> MainHomeScreen(
                    userName = userName,
                    onStartBasicMode = { navController.navigate(Route.BASIC_MODE) },
                    onStartPureMode = { navController.navigate(Route.PURE_MODE) },
                    onOpenTodos = { navController.navigate(Route.TODO_LIST) },
                    onOpenDDay = { navController.navigate(Route.DDAY) },
                    onOpenTier = { navController.navigate(Route.TIER_RANK) },
                    onOpenProfile = { navController.navigate(Route.SETTING_MY_INFO) },
                )

                Route.CALENDAR -> CalendarScreen()

                Route.RANK -> RankScreen(
                    onOpenTier = { navController.navigate(Route.TIER_RANK) },
                )

                Route.SETTING -> SettingScreen(
                    onOpenMyInfo = { navController.navigate(Route.SETTING_MY_INFO) },
                    onOpenPrivacy = { navController.navigate(Route.SETTING_PRIVACY) },
                    onOpenHelp = { navController.navigate(Route.SETTING_HELP) },
                    onOpenDDay = { navController.navigate(Route.DDAY) },
                    onLogout = {
                        scope.launch {
                            AppRepository.get(context.applicationContext).logout()
                            navController.navigate(Route.LOGIN) { popUpTo(0) }
                        }
                    },
                )
            }
        }
    }
}
