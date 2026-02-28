package com.dynastxu.notedown.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Route
import com.dynastxu.notedown.models.view.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 抽屉
 */
@Composable
fun AppDrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: MainViewModel
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
        // 添加笔记
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.label_add_note)) },
            selected = false,
            onClick = {
                viewModel.selectNote()
                navController.navigate(Route.EDIT)
                scope.launch { drawerState.close() }
            },
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
            selected = currentRoute == Route.SETTINGS,
            onClick = {
                navController.navigate(Route.SETTINGS)
                scope.launch { drawerState.close() }
            },
            icon = { Icon(Icons.Default.Settings, stringResource(R.string.title_settings)) }
        )
    }
}