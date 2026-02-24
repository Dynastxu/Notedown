package com.dynastxu.notedown.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.view.EditorViewModel
import com.dynastxu.notedown.models.view.MainViewModel

@Composable
fun EditScreen(navController: NavController, mainViewModel: MainViewModel, viewModel: EditorViewModel = viewModel()){
    val blocks by viewModel.blocks.collectAsState()
    val focusedIndex by viewModel.focusedIndex.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedNote by mainViewModel.selectedNote.collectAsState()

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
                    onDelete = { viewModel.removeBlockAt(index) },
                    isReadOnly = !isEditing
                )
            }
        }
    }
}

@Composable
fun BlockItem(
    block: Block,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onDelete: () -> Unit = {},
    onUpdateText: (String) -> Unit = {},
    isReadOnly: Boolean = false
){
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, borderColor, MaterialTheme.shapes.small)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        when (block) {
            is Block.HeadingBlock -> HeadingBlockEditor(block, onUpdateText)
            is Block.TextBlock -> TextBlockEditor(block, onUpdateText, isReadOnly)
            is Block.ImageBlock -> ImageBlockView(block, onDelete)
        }
    }
}

@Composable
fun HeadingBlockEditor(block: Block.HeadingBlock, onUpdateText: (String) -> Unit) {

}

@Composable
fun TextBlockEditor(block: Block.TextBlock, onUpdateText: (String) -> Unit, isReadOnly: Boolean = false) {
    var textFieldValue by remember(block.text) {
        mutableStateOf(TextFieldValue(block.text))
    }

    Column {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onUpdateText(it.text)
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            readOnly = isReadOnly
        )
    }
}

@Composable
fun ImageBlockView(block: Block.ImageBlock, onDelete: () -> Unit) {

}