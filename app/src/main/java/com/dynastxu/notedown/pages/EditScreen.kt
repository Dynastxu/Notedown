package com.dynastxu.notedown.pages

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.view.EditorViewModel
import com.dynastxu.notedown.models.view.MainViewModel
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun EditScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: EditorViewModel = viewModel()
) {
    val blocks by viewModel.blocks.collectAsState()
    val focusedIndex by viewModel.focusedIndex.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val currentFolder by mainViewModel.currentFolder.collectAsState()
    val note by mainViewModel.selectedNote.collectAsState()
    val listState = rememberLazyListState()

    mainViewModel.onEditBtnPressed = {
        val isEditing = mainViewModel.isEditing.value
        viewModel.setIsEditing(isEditing)
        if (!isEditing) {
            viewModel.save()
        }
    }

    LaunchedEffect(Unit) {
        if (note == "") {
            // 新建笔记时的操作
            viewModel.createNote(currentFolder!!)
            mainViewModel.setIsEditing(!isEditing)
            viewModel.setIsEditing(!isEditing)
        } else {
            // 读取笔记时的操作
            viewModel.readNote(note)
        }
    }

    // 自动滚动到焦点块
    LaunchedEffect(focusedIndex) {
        listState.animateScrollToItem(focusedIndex)
    }

    // 使用 State 来存储获取到的高度（像素值）
    var heightInPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // 转换为 DP
    val heightInDp = remember(heightInPx) { density.run { heightInPx.toDp() } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size: IntSize -> // 在回调中获取尺寸
                heightInPx = size.height
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
                val isLastItem = index == viewModel.blocks.collectAsState().value.size - 1
                BlockItem(
                    block = block,
                    isFocused = index == focusedIndex,
                    onFocus = { viewModel.setFocusedIndex(index) },
                    readOnly = !isEditing,
//                    modifier = if (isLastItem) Modifier.heightIn(heightInDp * 0.6f) else Modifier,
                    onImageClick = {
                        mainViewModel.setSelectedImage(it.src)
                        // TODO 导航到图片查看页面
                    },
                    isLastBlock = isLastItem
                )
            }
        }

        if (isEditing) {
            val focusedBlock = blocks[focusedIndex]
            EditToolBar(
                block = focusedBlock,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun EditToolBar(block: Block, modifier: Modifier = Modifier, viewModel: EditorViewModel) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (block is Block.RichTextBlock) {
                if (block.state != null) {
                    val isBold =
                        block.state!!.currentSpanStyle.fontWeight == FontWeight.Bold
                    val isItalic =
                        block.state!!.currentSpanStyle.fontStyle == FontStyle.Italic
                    val isStrikethrough =
                        block.state!!.currentSpanStyle.textDecoration == TextDecoration.LineThrough
                    val context = LocalContext.current
                    val photoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickMultipleVisualMedia(),
                        onResult = { uris ->
                            // 直接把 URI 列表传给 ViewModel 处理
                            viewModel.onImagesSelected(uris, context)
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
            }
        }
    }
}

@Composable
fun BlockItem(
    block: Block,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit,
    readOnly: Boolean = false,
    onImageClick: (Block.ImageBlock) -> Unit,
    isLastBlock: Boolean = false
) {
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .border(Dp.Hairline, borderColor, MaterialTheme.shapes.small) // TODO 仅在开发者模式下显示边框
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        when (block) {
            is Block.RichTextBlock -> TextBlock(
                block = block,
                readOnly = readOnly,
                isLastBlock = isLastBlock
            )

            is Block.ImageBlock -> ImageBlock(
                block = block,
                onClick = { onImageClick(it) },
                onLongClick = {} // TODO
            )
        }
    }
}

@Composable
fun TextBlock(
    block: Block.RichTextBlock,
    readOnly: Boolean,
    isLastBlock: Boolean = false
) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.setMarkdown(block.text)
        // state.setMarkdown("""# 111""") // test. toHtml: <p><span style="font-size: 2.0em;"><b>111</b></span></p>
        block.state = state
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxSize()
    ) {
        BasicRichTextEditor(
            state = state,
            readOnly = readOnly,
            modifier = Modifier
                .fillMaxSize(),
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
}

@Composable
fun ImageBlock(
    block: Block.ImageBlock,
    onClick: (Block.ImageBlock) -> Unit,
    onLongClick: (Block.ImageBlock) -> Unit
) {
    // 使用 Coil 的 AsyncImage 统一处理所有图片源
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(block.src)
            .crossfade(true)
            .build(),
        contentDescription = block.alt,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(block) },
                onLongClick = { onLongClick(block) }
            ),
        placeholder = painterResource(R.drawable.outline_image_24),
        error = painterResource(R.drawable.outline_broken_image_24),
    )
}