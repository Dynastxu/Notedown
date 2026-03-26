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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.dynastxu.notedown.pages.ImageScreen
import com.dynastxu.notedown.pages.SettingsScreen
import com.dynastxu.notedown.ui.theme.NotedownTheme
import com.dynastxu.notedown.views.AppDrawerContent
import com.dynastxu.notedown.views.AppTopBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NotedownTheme {
                var showFolderDialog by remember { mutableStateOf(false) }
                var folderName by remember { mutableStateOf("") }

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
                                scope = scope,
                                viewModel = viewModel,
                                onNewFolderClick = { showFolderDialog = true }
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
                                visible = !currentRoute.startsWith(Route.IMAGE) && !currentRoute.startsWith(Route.EDIT),
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
                                    scope,
                                    viewModel
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
                            composable(Route.HOME) { HomeScreen(navController, viewModel) }
                            composable(Route.SETTINGS) { SettingsScreen(navController) }
                            composable(
                                "${Route.EDIT}/{notePathEncoded}",
                                arguments = listOf(navArgument("notePathEncoded") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val notePathEncoded = backStackEntry.arguments?.getString("notePathEncoded") ?: return@composable
                                EditScreen(
                                    notePathEncoded = notePathEncoded,
                                    navController = navController
                                )
                            }
                            composable(Route.IMAGE) { ImageScreen(navController, viewModel) }
                        }
                    }
                    // 文件夹添加窗口
                    if (showFolderDialog) {
                        AlertDialog(
                            onDismissRequest = { showFolderDialog = false },
                            title = {
                                Text(stringResource(R.string.label_creat_new_folder))
                            },
                            text = {
                                OutlinedTextField(
                                    value = folderName,
                                    onValueChange = { folderName = it },
                                    label = { Text(stringResource(R.string.label_folder_name)) },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                val unnamedName = stringResource(R.string.unnamed_name)
                                TextButton(
                                    onClick = {
                                        viewModel.createNewFolder(folderName, unnamedName)
                                        showFolderDialog = false
                                        folderName = ""
                                        scope.launch { drawerState.close() }
                                    }
                                ) {
                                    Text(stringResource(R.string.confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showFolderDialog = false
                                        folderName = ""
                                    }
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}