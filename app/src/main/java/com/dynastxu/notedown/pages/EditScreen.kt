package com.dynastxu.notedown.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dynastxu.notedown.R
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.view.EditorViewModel
import com.dynastxu.notedown.models.view.MainViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
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
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedNote by mainViewModel.selectedNote.collectAsState()
    val selections = remember { mutableStateMapOf<Int, IntRange?>() }

    mainViewModel.onEditBtnPressed = {
        val isEditing = mainViewModel.isEditing.value
        viewModel.setIsEditing(isEditing)
        if (!isEditing) {
            viewModel.save()
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
                    modifier = if (isLastItem) Modifier.heightIn(heightInDp*0.6f) else Modifier
                )
            }
        }

        if (isEditing) {
            val focusedBlock = blocks[focusedIndex]
            EditToolBar(
                block = focusedBlock,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            )
        }
    }
}

@Composable
fun EditToolBar(block: Block, modifier: Modifier = Modifier) {
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
                    // 加粗
                    IconButton(
                        onClick = {
                            block.state?.toggleSpanStyle(
                                SpanStyle(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isBold) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_format_bold_24),
                            stringResource(R.string.icon_desc_bold)
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
    readOnly: Boolean = false
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
            .border(Dp.Hairline, borderColor, MaterialTheme.shapes.small)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        when (block) {
            is Block.RichTextBlock -> TextBlockEditor(
                block = block,
                readOnly = readOnly,
                onTextChange = { text, state ->
                    block.text = text
                    block.state = state
                }
            )
        }
    }
}

@Composable
fun TextBlockEditor(
    block: Block.RichTextBlock,
    readOnly: Boolean,
    onTextChange: (String, RichTextState) -> Unit
) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.setMarkdown(block.text)
    }

    // 当 state 变化时通知外部
    LaunchedEffect(state) {
        snapshotFlow { state }
            .collect { state ->
                if (state != block.state) {
                    onTextChange(state.toMarkdown(), state)
                }
            }
    }

    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        BasicRichTextEditor(
            state = state,
            readOnly = readOnly,
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 16.sp
            )
        )
    }
}