package com.dynastxu.notedown.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Folder
import com.dynastxu.notedown.models.data.Route
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.models.data.note.NoteConfig
import com.dynastxu.notedown.models.view.HomeViewModel
import com.dynastxu.notedown.utils.export
import com.dynastxu.notedown.views.Loading
import java.io.File
import java.util.Date

/**
 * 主页 Composable
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val folderReady by viewModel.folderReady.collectAsState()
    val newNoteReady by viewModel.newNoteReady.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val notes by viewModel.currentNotesList.collectAsState()
    val folders by viewModel.currentFoldersList.collectAsState()
    val selectMode by viewModel.selectMode.collectAsState()

    var showFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    LaunchedEffect(notes, folders, currentFolder) {
        if (currentFolder == null) return@LaunchedEffect
        viewModel.scanFoldersAndNotes(currentFolder!!.folder)
    }

    LaunchedEffect(newNoteReady) {
        if (newNoteReady != null) {
            val encodedPath = Uri.encode(newNoteReady!!.folder.absolutePath)
            viewModel.newNoteReadyConsume()
            navController.navigate("${Route.EDIT}/$encodedPath")
        }
    }

    if (folderReady == null) {
        // 文件夹未准备好，显示加载状态
        Loading()
    } else {
        // 文件夹已准备好，显示内容
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Route(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
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
                            viewModel.createNewNote()
                        }) { Text(stringResource(R.string.btn_add_note)) }
                    }
                } else {
                    NotesList(
                        navController = navController,
                        viewModel = viewModel,
                        onLongClick = { index ->
                            if (!selectMode) {
                                viewModel.setSelectMode(true)
                            }
                            viewModel.select(index)
                        }
                    )
                }

                // 文件夹添加窗口
                if (showFolderDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showFolderDialog = false
                        },
                        title = {
                            Text(stringResource(R.string.label_creat_new_folder))
                        },
                        text = {
                            OutlinedTextField(
                                value = folderName,
                                onValueChange = {
                                    folderName = it
                                },
                                label = { Text(stringResource(R.string.label_folder_name)) },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            val defaultName = stringResource(R.string.unnamed_name)
                            TextButton(
                                onClick = {
                                    viewModel.createNewFolder(folderName, defaultName)
                                    showFolderDialog = false
                                    folderName = ""
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

            if (!selectMode) {
                FloatingActionButtons(
                    onCreateNoteClick = {
                        viewModel.createNewNote()
                    },
                    onCreateFolderClick = {
                        showFolderDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun FloatingActionButtons(
    modifier: Modifier = Modifier,
    onCreateNoteClick: () -> Unit = {},
    onCreateFolderClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = onCreateFolderClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                painterResource(R.drawable.outline_create_new_folder_24),
                stringResource(R.string.label_creat_new_folder)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExtendedFloatingActionButton(
            onClick = onCreateNoteClick,
            icon = {
                Icon(
                    painterResource(R.drawable.outline_add_notes_24),
                    stringResource(R.string.label_add_note)
                )
            },
            text = {
                Text(stringResource(R.string.btn_add_note))
            }
        )
    }
}

@Composable
fun Route(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val route by viewModel.route.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val folderReady by viewModel.folderReady.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(folderReady, currentFolder) {
        if (folderReady == null || currentFolder == null) return@LaunchedEffect
        viewModel.calculateRoute(folderReady!!, currentFolder!!.folder)
    }

    LaunchedEffect(route) {
        if (route.isNotEmpty()) {
            listState.animateScrollToItem(route.lastIndex)
        }
    }

    LazyRow(
        modifier = modifier,
        state = listState
    ) {
        items(route.size) { index ->
            if (index != 0) {
                Text(" > ")
            }
            Text(
                text = route[index].name,
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            viewModel.setCurrentFolder(Folder(route[index]))
                        }
                    )
            )
        }
    }
}

@Composable
fun NotesList(
    navController: NavController,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onLongClick: (Int) -> Unit
) {
    val notes by viewModel.currentNotesList.collectAsState()
    val folders by viewModel.currentFoldersList.collectAsState()
    val selectMode by viewModel.selectMode.collectAsState()
    val selections by viewModel.selections.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()

    if (currentFolder == null) {
        Log.e("笔记列表", "当前目录为 null")
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setSelectMode(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(folders.size) { index ->
                FolderItem(
                    folder = folders[index],
                    onLongClick = { onLongClick(index) },
                    onClick = {
                        if (selectMode) {
                            viewModel.select(index)
                        } else {
                            viewModel.setCurrentFolder(
                                Folder(folders[index].folder)
                            )
                        }
                    },
                    selected = selections.contains(index)
                )
            }
            items(notes.size) { index ->
                val actualIndex = index + folders.size
                NoteItem(
                    note = notes[index],
                    onLongClick = { onLongClick(actualIndex) },
                    onClick = {
                        if (selectMode) {
                            // 多选模式
                            viewModel.select(actualIndex)
                        } else {
                            // 使用 NavType.encodeToUrl() 方法进行安全编码
                            val encodedPath = Uri.encode(notes[index].folder.absolutePath)
                            navController.navigate("${Route.EDIT}/$encodedPath")
                        }
                    },
                    selected = selections.contains(actualIndex)
                )
            }
        }

        AnimatedVisibility(
            visible = selectMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it * 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomToolBar(
                viewModel = viewModel,
                currentFolder = currentFolder!!.folder
            )
        }
    }
}

@Composable
fun BottomToolBar(viewModel: HomeViewModel, modifier: Modifier = Modifier, currentFolder: File) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // 导出
            val context = LocalContext.current
            val createZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip")
            ) { uri ->
                uri?.let {
                    val selectedFolders = viewModel.getSelectedFoldersAndNotes()
                    if (selectedFolders.isNotEmpty()) {
                        export(context, selectedFolders, it)
                        viewModel.setSelectMode(false)
                    }
                }
            }
            IconButton(
                onClick = {
                    val timestamp = System.currentTimeMillis()
                    createZipLauncher.launch("notedown_export_$timestamp.zip")
                }
            ) {
                Icon(
                    painterResource(R.drawable.outline_export_notes_24),
                    stringResource(R.string.icon_desc_export)
                )
            }
            // 移动
            IconButton(
                onClick = {} // TODO
            ) {
                Icon(
                    painterResource(R.drawable.outline_move_item_24),
                    stringResource(R.string.icon_desc_move)
                )
            }
            // 删除
            IconButton(
                onClick = {
                    viewModel.onDelete()
                    viewModel.scanFoldersAndNotes(currentFolder)
                }
            ) {
                Icon(
                    painterResource(R.drawable.outline_delete_24),
                    stringResource(R.string.icon_desc_delete)
                )
            }
            // 分享
            IconButton(
                onClick = {} // TODO
            ) {
                Icon(
                    painterResource(R.drawable.outline_share_24),
                    stringResource(R.string.icon_desc_share)
                )
            }
            // 取消
            IconButton(
                onClick = {
                    viewModel.setSelectMode(false)
                }
            ) {
                Icon(
                    painterResource(R.drawable.outline_cancel_24),
                    stringResource(R.string.icon_desc_cancel)
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    folder: Folder,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    selected: Boolean = false
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (selected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧文件夹图标
            Icon(
                painter = if (folder.notesNum == 0) painterResource(R.drawable.outline_folder_24) else painterResource(
                    R.drawable.baseline_folder_24
                ),
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
fun NoteItem(
    note: Note,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    selected: Boolean = false
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (selected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
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
                    text = note.config.title.ifBlank { note.folder.name },
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
                val dateFormat =
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                if (note.config.editDate != null) {
                    Text(
                        text = "${stringResource(R.string.text_modified_on)} ${
                            dateFormat.format(
                                note.config.editDate!!
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PNoteItem() {
    NoteItem(
        Note(
            File(""), NoteConfig(
                title = "Title",
                editDate = Date()
            )
        )
    )
}