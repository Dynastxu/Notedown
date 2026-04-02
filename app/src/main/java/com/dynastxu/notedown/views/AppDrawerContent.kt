package com.dynastxu.notedown.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(0.dp, 16.dp, 16.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { scope.launch { drawerState.close() } }
            ) {
                Icon(
                    painterResource(R.drawable.outline_menu_24),
                    stringResource(R.string.icon_desc_menu)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge
            )
        }
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        // 搜索
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.label_search)) },
            selected = false,
            onClick = { }, // TODO
            icon = {
                Icon(
                    painterResource(R.drawable.outline_search_24),
                    stringResource(R.string.label_search)
                )
            }
        )
        HorizontalDivider(thickness = Dp.Hairline)
        // 设置
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.title_settings)) },
            selected = currentRoute == Route.SETTINGS,
            onClick = {
                navController.navigate(Route.SETTINGS)
                scope.launch { drawerState.close() }
            },
            icon = {
                Icon(
                    painterResource(R.drawable.outline_settings_24),
                    stringResource(R.string.title_settings)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PAppDrawerContent() {
    AppDrawerContent(
        navController = rememberNavController(),
        drawerState = DrawerState(initialValue = DrawerValue.Closed),
        scope = rememberCoroutineScope()
    )
}