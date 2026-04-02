package com.dynastxu.notedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dynastxu.notedown.models.data.Route
import com.dynastxu.notedown.models.view.MainViewModel
import com.dynastxu.notedown.pages.EditScreen
import com.dynastxu.notedown.pages.HomeScreen
import com.dynastxu.notedown.pages.SettingsScreen
import com.dynastxu.notedown.ui.theme.NotedownTheme
import com.dynastxu.notedown.views.AppDrawerContent
import com.dynastxu.notedown.views.AppTopBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NotedownTheme {
                // 创建导航控制器
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // 抽屉
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(280.dp) // 可能宽度还是过宽
                        ) {
                            AppDrawerContent(
                                navController = navController,
                                drawerState = drawerState,
                                scope = scope
                            )
                        }
                    },
                    gesturesEnabled = currentRoute == Route.HOME
                ) {
                    // 外层 Scaffold，包含动态顶部栏
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentRoute == null) return@Scaffold
                            AnimatedVisibility(
                                visible = !currentRoute.startsWith(
                                    Route.EDIT
                                ),
                                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                                    animationSpec = tween(300)
                                ),
                                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                                    animationSpec = tween(300)
                                )
                            ) {
                                AppTopBar(
                                    navController,
                                    drawerState,
                                    scope
                                )
                            }
                        }  // 顶部栏随页面变化
                    ) { innerPadding ->
                        // 导航图放在 Scaffold 内容区，使用 innerPadding 避免被状态栏遮挡
                        NavHost(
                            navController = navController,
                            startDestination = Route.HOME,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Route.HOME) { HomeScreen(navController) }
                            composable(Route.SETTINGS) { SettingsScreen(navController) }
                            composable(
                                "${Route.EDIT}/{notePathEncoded}",
                                arguments = listOf(navArgument("notePathEncoded") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val notePathEncoded =
                                    backStackEntry.arguments?.getString("notePathEncoded")
                                        ?: return@composable
                                EditScreen(
                                    notePathEncoded = notePathEncoded,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}