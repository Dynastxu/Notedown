package com.dynastxu.notedown.views

import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Route
import com.dynastxu.notedown.models.view.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 动态顶部应用栏，根据当前路由显示不同标题，并自动处理返回按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: MainViewModel
) {
    // 观察当前栈顶条目，以获取当前路由
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isEditing by viewModel.isEditing.collectAsState()

    // 根据路由确定标题
    val title = when (currentRoute) {
        Route.HOME -> stringResource(R.string.title_home)
        Route.SETTINGS -> stringResource(R.string.title_settings)
        Route.EDIT -> stringResource(R.string.title_edit)
        Route.IMAGE -> stringResource(R.string.title_image)
        else -> stringResource(R.string.app_name)
    }

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            when (currentRoute) {
                Route.HOME -> {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            painterResource(R.drawable.outline_menu_24),
                            stringResource(R.string.icon_desc_menu)
                        )
                    }
                }

                else -> {
                    // 点击返回上一页
                    IconButton(onClick = {
                        // 如果在编辑页面，触发返回保存逻辑
                        if (currentRoute == Route.EDIT) {
                            viewModel.onEditBackPressed()
                        }

                        navController.navigateUp()
                    }) {
                        Icon(
                            painterResource(R.drawable.outline_arrow_back_24),
                            stringResource(R.string.icon_desc_back)
                        )
                    }
                }
            }
        },
        actions = {
            when (currentRoute) {
                Route.EDIT -> {
                    IconButton(onClick = { viewModel.onPressEditBtn() }) {
                        if (isEditing) {
                            Icon(
                                painterResource(R.drawable.outline_save_24),
                                stringResource(R.string.icon_desc_done)
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.outline_edit_24),
                                stringResource(R.string.icon_desc_edit)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}