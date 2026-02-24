package com.dynastxu.notedown.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dynastxu.notedown.R
import com.dynastxu.notedown.ROUTE_EDIT
import com.dynastxu.notedown.models.view.MainViewModel

/**
 * 主页 Composable
 */
@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {
    val folder by viewModel.folderReady.collectAsState()
    val notes by viewModel.notes.collectAsState()

    when (folder) {
        null -> {
            // 文件夹未准备好，显示加载状态
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            // 文件夹已准备好，显示内容
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (notes.isEmpty()) {
                    // 没有笔记
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            navController.navigate(ROUTE_EDIT)
                            viewModel.selectNote()
                        }) { Text(stringResource(R.string.btn_add_note)) }
                    }
                } else {

                }
            }
        }
    }
}