package com.dynastxu.notedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dynastxu.notedown.models.view.MainViewModel
import com.dynastxu.notedown.pages.HomeScreen
import com.dynastxu.notedown.pages.SettingsScreen
import com.dynastxu.notedown.ui.theme.NotedownTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // 抽屉
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DrawerContent(navController, drawerState, scope)
                        }
                    },
                    gesturesEnabled = true
                ) {
                    // 外层 Scaffold，包含动态顶部栏
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { AppTopBar(navController, drawerState, scope) }  // 顶部栏随页面变化
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
}

/**
 * 动态顶部应用栏，根据当前路由显示不同标题，并自动处理返回按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // 观察当前栈顶条目，以获取当前路由
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 根据路由确定标题
    val title = when (currentRoute) {
        ROUTE_HOME -> stringResource(R.string.title_home)
        ROUTE_SETTINGS -> stringResource(R.string.title_settings)
        else -> stringResource(R.string.app_name)
    }

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            when (currentRoute) {
                ROUTE_HOME -> {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            stringResource(R.string.icon_desc_menu)
                        )
                    }
                }

                else -> {
                    // 点击返回上一页
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            stringResource(R.string.icon_desc_back)
                        )
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

@Composable
fun DrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.label_add_note)) },
            selected = false,
            onClick = { TODO() },
            icon = { Icon(Icons.Default.Add, stringResource(R.string.label_add_note)) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.label_search)) },
            selected = false,
            onClick = { TODO() },
            icon = { Icon(Icons.Default.Search, stringResource(R.string.label_search)) }
        )
        HorizontalDivider(thickness = Dp.Hairline)
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.title_settings)) },
            selected = currentRoute == ROUTE_SETTINGS,
            onClick = {
                navController.navigate(ROUTE_SETTINGS)
                scope.launch { drawerState.close() }
            },
            icon = { Icon(Icons.Default.Settings, stringResource(R.string.title_settings)) }
        )
    }
}


