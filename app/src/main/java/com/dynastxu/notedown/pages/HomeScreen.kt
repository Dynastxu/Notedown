package com.dynastxu.notedown.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dynastxu.notedown.R
import com.dynastxu.notedown.ROUTE_EDIT
import com.dynastxu.notedown.models.data.Folder
import com.dynastxu.notedown.models.data.Note
import com.dynastxu.notedown.models.view.HomeViewModel
import com.dynastxu.notedown.models.view.MainViewModel
import java.io.File

/**
 * 主页 Composable
 */
@Composable
fun HomeScreen(navController: NavController, mainViewModel: MainViewModel, viewModel: HomeViewModel = viewModel()) {
    val folderReady by mainViewModel.folderReady.collectAsState()
    val currentFolder by mainViewModel.currentFolder.collectAsState()
    val notes by viewModel.currentNotesList.collectAsState()
    val folders by viewModel.currentFoldersList.collectAsState()

    LaunchedEffect(notes, folders, currentFolder) {
        if (currentFolder == null) return@LaunchedEffect
        viewModel.scanNoteFolders(currentFolder!!)
    }

    when (folderReady) {
        null -> {
            // 文件夹未准备好，显示加载状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            // 文件夹已准备好，显示内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (notes.isEmpty() && folders.isEmpty()) {
                    // 没有笔记
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.no_note))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            navController.navigate(ROUTE_EDIT)
                            mainViewModel.selectNote()
                        }) { Text(stringResource(R.string.btn_add_note)) }
                    }
                } else {
                    NotesList(
                        navController = navController,
                        currentFolder = currentFolder!!,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun NotesList(navController: NavController, currentFolder: File, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val notes by viewModel.currentNotesList.collectAsState()
    val folders by viewModel.currentFoldersList.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        items(folders) {
            FolderItem(
                folder = it
            )
        }
        items(notes) {
            NoteItem(
                note = it
            )
        }
    }
}

@Composable
fun FolderItem(folder: Folder, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧文件夹图标
            Icon(
                painter = painterResource(R.drawable.baseline_folder_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧文本内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 加粗的文件夹名
                Text(
                    text = folder.folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 笔记数量
                Text(
                    text = "${folder.notesNum} 个笔记",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoteItem(note: Note, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧笔记图标
            Icon(
                painter = painterResource(R.drawable.baseline_article_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧文本内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 加粗的笔记标题
                Text(
                    text = note.config.title.ifEmpty { note.folder.name },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

//                // 标签信息
//                if (note.config.tags.isNotEmpty()) {
//                    Text(
//                        text = note.config.tags.joinToString(", ") { it.name },
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }

                // 日期信息
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                if (note.config.editDate != null) {
                    Text(
                        text = "${stringResource(R.string.text_modified_on)} ${dateFormat.format(note.config.editDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}