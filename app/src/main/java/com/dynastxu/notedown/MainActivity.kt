package com.dynastxu.notedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dynastxu.notedown.models.view.MainViewModel
import com.dynastxu.notedown.pages.HomeScreen
import com.dynastxu.notedown.pages.SettingsScreen
import com.dynastxu.notedown.ui.theme.NotedownTheme

const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotedownTheme {
                // 创建导航控制器
                val navController = rememberNavController()

                // 外层 Scaffold，包含动态顶部栏
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { AppTopBar(navController) }  // 顶部栏随页面变化
                ) { innerPadding ->
                    // 导航图放在 Scaffold 内容区，使用 innerPadding 避免被状态栏遮挡
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(ROUTE_HOME) { HomeScreen(navController, viewModel) }
                        composable(ROUTE_SETTINGS) { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}

/**
 * 动态顶部应用栏，根据当前路由显示不同标题，并自动处理返回按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavHostController) {
    // 观察当前栈顶条目，以获取当前路由
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 根据路由确定标题
    val title = when (currentRoute) {
        ROUTE_HOME -> stringResource(R.string.title_home)
        ROUTE_SETTINGS -> stringResource(R.string.title_settings)
        else -> stringResource(R.string.app_name)
    }

    // 是否显示返回按钮（当不在首页时）
    val canNavigateBack = currentRoute != ROUTE_HOME

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (canNavigateBack) {
                // 点击返回上一页
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.icon_desc_back)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}


