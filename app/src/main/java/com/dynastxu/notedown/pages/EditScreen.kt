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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
                BlockItem(
                    block = block,
                    isFocused = index == focusedIndex,
                    onFocus = { viewModel.setFocusedIndex(index) },
                    readOnly = !isEditing
                )
            }
        }

        if (isEditing) {
            val focusedBlock = blocks[focusedIndex]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (focusedBlock is Block.RichTextBlock) {
                        if (focusedBlock.state != null) {
                            val isBold =
                                focusedBlock.state!!.currentSpanStyle.fontWeight == FontWeight.Bold
                            // 加粗
                            IconButton(
                                onClick = {
                                    focusedBlock.state?.toggleSpanStyle(
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