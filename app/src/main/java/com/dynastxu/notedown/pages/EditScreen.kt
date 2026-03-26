package com.dynastxu.notedown.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.ImageData
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.models.view.EditorViewModel
import com.dynastxu.notedown.views.ImagePreviewer
import com.dynastxu.notedown.views.Loading
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun EditScreen(
    notePathEncoded: String,
    navController: NavController,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val blocks by viewModel.blocks.collectAsState()
    val focusedIndex by viewModel.focusedIndex.collectAsState()
    val listState = rememberLazyListState()
    val noteReady by viewModel.noteReady.collectAsState()
    val title by viewModel.title.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val note by viewModel.note.collectAsState()
    var isPreviewingImage by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<ImageData?>(null) }

    // 解码路径
    val notePath = remember(notePathEncoded) {
        Uri.decode(notePathEncoded)
    }

    LaunchedEffect(notePath) {
        viewModel.loadNote(notePath)
    }

    if (!noteReady) {
        Loading(
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // 自动滚动到焦点块
    LaunchedEffect(focusedIndex) {
        listState.animateScrollToItem(focusedIndex)
    }

    if (!isPreviewingImage) {
        Scaffold(
            topBar = {
                // 自定义顶部栏，包含编辑模式切换按钮
                EditTopBar(
                    isEditing = isEditing,
                    navController = navController,
                    onToggleEdit = {
                        viewModel.toggleEditing()
                        if (isEditing) {
                            viewModel.saveNote()
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                viewModel.setTitle(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            readOnly = !isEditing,
                            placeholder = {
                                Text(
                                    stringResource(R.string.title)
                                )
                            }
                        )
                    }
                    itemsIndexed(blocks) { index, block ->
                        val isLastItem = index == viewModel.blocks.collectAsState().value.size - 1
                        val isFocused = index == focusedIndex
                        val borderColor =
                            if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                        BlockItem(
                            block = block,
                            readOnly = !isEditing,
                            onImageClick = {
                                @Suppress("AssignedValueIsNeverRead")
                                isPreviewingImage = true
                                @Suppress("AssignedValueIsNeverRead")
                                selectedImage = it
                            },
                            isLastBlock = isLastItem,
                            onNeedFocus = {
                                viewModel.setFocusedIndex(index)
                            },
                            onDeletePrevious = {
                                viewModel.deletePreviousBlockIfAtStart()
                            },
                            modifier = Modifier
                                .border(Dp.Hairline, borderColor, MaterialTheme.shapes.small),
                            isFocused = isFocused
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isEditing,
                    enter = slideInVertically(initialOffsetY = { it }), // FIXME 动画效果非预期
                    exit = slideOutVertically(targetOffsetY = { it * 2 }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val focusedBlock = if (blocks.isNotEmpty()) {
                        blocks[focusedIndex.coerceIn(0, blocks.size - 1)]
                    } else {
                        null
                    }
                    if (focusedBlock != null && note != null) {
                        EditToolBar(
                            block = focusedBlock,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .imePadding(),
                            viewModel = viewModel,
                            note = note!!
                        )
                    } else {
                        Log.e("EditScreen", "focusedBlock or note is null")
                    }
                }
            }
        }
    }
    else {
        ImagePreviewer(
            image = selectedImage,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                @Suppress("AssignedValueIsNeverRead")
                isPreviewingImage = false
            }
        )
    }
}

@Composable
fun EditToolBar(
    block: Block,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel,
    note: Note
) {
    var shouldShowToolbar by remember { mutableStateOf(false) }

    LaunchedEffect(block) {
        if (block is Block.RichTextBlock) {
            if (block.state != null) {
                shouldShowToolbar = true
            }
        } else {
            shouldShowToolbar = true
        }
    }

    if (!shouldShowToolbar) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            when (block) {
                is Block.RichTextBlock -> {
                    if (block.state == null) return@Row

                    val isBold =
                        block.state!!.currentSpanStyle.fontWeight == FontWeight.Bold
                    val isItalic =
                        block.state!!.currentSpanStyle.fontStyle == FontStyle.Italic
                    val isStrikethrough =
                        block.state!!.currentSpanStyle.textDecoration == TextDecoration.LineThrough
                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickMultipleVisualMedia(),
                        onResult = { uris ->
                            // 获取完整的带格式内容
                            val fullMarkdown = block.state!!.toMarkdown()
                            val selection = block.state!!.selection

                            // 传递完整信息给 ViewModel 处理
                            viewModel.onImagesSelected(
                                uris = uris,
                                fullContent = fullMarkdown,
                                selectionStart = selection.min,
                                selectionEnd = selection.max,
                                note = note
                            )
                        }
                    )

                    @Composable
                    fun colors(highlight: Boolean): IconButtonColors {
                        val color = LocalContentColor.current
                        val highlightColor = MaterialTheme.colorScheme.primary
                        return IconButtonDefaults.iconButtonColors(
                            contentColor = if (highlight) highlightColor else color
                        )
                    }
                    // 加粗
                    IconButton(
                        onClick = {
                            block.state!!.toggleSpanStyle(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = colors(isBold)
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_format_bold_24),
                            stringResource(R.string.icon_desc_bold)
                        )
                    }
                    // 斜体
                    IconButton(
                        onClick = {
                            block.state!!.toggleSpanStyle(
                                SpanStyle(fontStyle = FontStyle.Italic)
                            )
                        },
                        colors = colors(isItalic)
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_format_italic_24),
                            stringResource(R.string.icon_desc_italic)
                        )
                    }
                    // 删除线
                    IconButton(
                        onClick = {
                            block.state!!.toggleSpanStyle(
                                SpanStyle(textDecoration = TextDecoration.LineThrough)
                            )
                        },
                        colors = colors(isStrikethrough)
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_format_strikethrough_24),
                            stringResource(R.string.icon_desc_strikethrough)
                        )
                    }
                    // 插入图片
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_image_24),
                            stringResource(R.string.icon_desc_insert_img)
                        )
                    }
                }

                is Block.ImageBlock -> {
                    IconButton(
                        onClick = {
                            viewModel.removeFocusedBlock()
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_delete_24),
                            stringResource(R.string.icon_desc_delete)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 块
 *
 * @param block 块
 * @param readOnly 只读
 * @param onImageClick 图片点击事件
 * @param onNeedFocus 聚焦事件
 * @param isLastBlock 属于最后一个块
 * @param isFocused 是否被聚焦
 * @param onDeletePrevious 删除前一个块事件
 */
@Composable
fun BlockItem(
    block: Block,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onImageClick: (ImageData) -> Unit = {},
    isLastBlock: Boolean = false,
    onNeedFocus: () -> Unit = {},
    isFocused: Boolean = false,
    onDeletePrevious: () -> Boolean = { false }
) {
    Box(
        modifier = modifier
    ) {
        when (block) {
            is Block.RichTextBlock -> TextBlock(
                block = block,
                readOnly = readOnly,
                isLastBlock = isLastBlock,
                onNeedFocus = onNeedFocus,
                onDeletePrevious = onDeletePrevious
            )

            is Block.ImageBlock -> ImageBlock(
                block = block,
                onClick = {
                    if (readOnly) {
                        onImageClick(it.image)
                    } else if (isFocused) {
                        onImageClick(it.image)
                    } else {
                        onNeedFocus()
                    }
                },
                onLongClick = {}, // TODO
            )
        }
    }
}

/**
 * 文本块
 * @param block 文本块
 * @param readOnly 只读
 * @param isLastBlock 是否是最后一个块
 * @param onNeedFocus 聚焦事件
 * @param onDeletePrevious 删除前一个块事件
 */
@Composable
fun TextBlock(
    block: Block.RichTextBlock,
    readOnly: Boolean,
    isLastBlock: Boolean = false,
    onNeedFocus: () -> Unit,
    onDeletePrevious: () -> Boolean = { false }
) {
    var state = rememberRichTextState()

    // 设置初始文本
    if (block.state == null) {
        state.setMarkdown(block.initialText)
        block.state = state
    } else {
        state = block.state!!
    }

    BasicRichTextEditor(
        state = state,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .focusable()
            .onFocusChanged { if (it.isFocused) onNeedFocus() }
            .onPreviewKeyEvent { keyEvent ->
                // 检测删除键（Backspace）
                if (keyEvent.key == Key.Backspace &&
                    keyEvent.type == KeyEventType.KeyDown
                ) {
                    // 检查光标是否在最开始
                    val selectionStart = state.selection.min
                    if (selectionStart == 0) {
                        // 调用删除前一个块的方法
                        onDeletePrevious()
                    }
                }
                false
            },
        textStyle = TextStyle(
            fontSize = 16.sp
        ),
        onTextLayout = {
            Log.d("文本内容", "markdown: ${state.toMarkdown()}")
            Log.d("文本内容", "html: ${state.toHtml()}")
            block.state = state
        },
        minLines = if (isLastBlock) 32 else 1
    )
}

/**
 * 图片块
 * @param block 图片块
 * @param onClick 图片点击事件
 * @param onLongClick 图片长按事件
 */
@Composable
fun ImageBlock(
    block: Block.ImageBlock,
    onClick: (Block.ImageBlock) -> Unit,
    onLongClick: (Block.ImageBlock) -> Unit
) {
    // 使用 Coil 的 AsyncImage 统一处理所有图片源
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(block.image.src)
            .crossfade(true)
            .build(),
        contentDescription = block.image.alt,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(block) },
                onLongClick = { onLongClick(block) }
            ),
        placeholder = painterResource(R.drawable.outline_image_24),
        error = painterResource(R.drawable.outline_broken_image_24),
        onError = {
            Log.e("图片加载", "图片加载失败")
            Log.e("图片加载", "路径： ${block.image.src}")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTopBar(
    isEditing: Boolean,
    onToggleEdit: () -> Unit = {},
    navController: NavController
) {
    TopAppBar(
        title = {
            Text(
                text = if (isEditing) stringResource(R.string.title_edit) else stringResource(R.string.title_preview),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            IconButton(onClick = onToggleEdit) {
                Icon(
                    painter = if (!isEditing) painterResource(R.drawable.outline_edit_24) else painterResource(
                        R.drawable.outline_save_24
                    ),
                    contentDescription = if (isEditing) stringResource(R.string.icon_desc_edit) else stringResource(
                        R.string.icon_desc_save
                    )
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigateUp() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_arrow_back_24),
                    contentDescription = stringResource(R.string.icon_desc_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}