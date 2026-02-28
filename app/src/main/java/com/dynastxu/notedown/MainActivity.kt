package com.dynastxu.notedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dynastxu.notedown.models.data.Route
import com.dynastxu.notedown.models.view.MainViewModel
import com.dynastxu.notedown.pages.EditScreen
import com.dynastxu.notedown.pages.HomeScreen
import com.dynastxu.notedown.pages.SettingsScreen
import com.dynastxu.notedown.ui.theme.NotedownTheme
import com.dynastxu.notedown.views.AppDrawerContent
import com.dynastxu.notedown.views.AppTopBar



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

                // 抽屉
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            AppDrawerContent(navController, drawerState, scope, viewModel)
                        }
                    },
                    gesturesEnabled = true
                ) {
                    // 外层 Scaffold，包含动态顶部栏
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            AppTopBar(
                                navController,
                                drawerState,
                                scope,
                                viewModel
                            )
                        }  // 顶部栏随页面变化
                    ) { innerPadding ->
                        // 导航图放在 Scaffold 内容区，使用 innerPadding 避免被状态栏遮挡
                        NavHost(
                            navController = navController,
                            startDestination = Route.HOME,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Route.HOME) { HomeScreen(navController, viewModel) }
                            composable(Route.SETTINGS) { SettingsScreen(navController) }
                            composable(Route.EDIT) { EditScreen(navController, viewModel) }
                        }
                    }
                }
            }
        }
    }
}