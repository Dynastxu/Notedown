package com.dynastxu.notedown.models.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Block
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.toMutableList

class EditorViewModel : ViewModel() {
    private val _blocks = MutableStateFlow<List<Block>>(listOf(Block.RichTextBlock()))
    val blocks = _blocks.asStateFlow()

    private val _focusedIndex = MutableStateFlow(0)
    val focusedIndex = _focusedIndex.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    fun save() {
        viewModelScope.launch {  } // TODO 保存逻辑
    }

    fun setIsEditing(isEditing: Boolean) {
        _isEditing.value = isEditing
    }

    fun addBlockAfter(index: Int, block: Block) {
        _blocks.update { list ->
            list.toMutableList().apply { add(index + 1, block) }
        }
    }

    fun removeBlockAt(index: Int) {
        _blocks.update { list ->
            if (list.size > 1) {
                list.toMutableList().apply { removeAt(index) }
            } else list
        }
        // 焦点调整
        if (index <= _focusedIndex.value && _focusedIndex.value > 0) {
            _focusedIndex.value -= 1
        }
    }

    fun updateTextBlock(index: Int, update: Block.RichTextBlock.() -> Unit) {
        _blocks.update { list ->
            list.mapIndexed { i, block ->
                if (i == index && block is Block.RichTextBlock) {
                    block.apply(update)
                } else block
            }
        }
    }

    fun setFocusedIndex(index: Int) {
        _focusedIndex.value = index.coerceIn(0, _blocks.value.lastIndex)
    }

    fun moveFocusUp() {
        if (_focusedIndex.value > 0) _focusedIndex.value -= 1
    }

    fun moveFocusDown() {
        if (_focusedIndex.value < _blocks.value.lastIndex) _focusedIndex.value += 1
    }
}