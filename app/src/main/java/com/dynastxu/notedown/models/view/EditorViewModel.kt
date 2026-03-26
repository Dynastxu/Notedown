package com.dynastxu.notedown.models.view

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.ImageData
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val notePath: String = Uri.decode(savedStateHandle["notePathEncoded"]) ?: ""

    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note

    private val _blocks = MutableStateFlow<List<Block>>(listOf(Block.RichTextBlock()))
    val blocks: StateFlow<List<Block>> = _blocks

    private val _focusedIndex = MutableStateFlow(0)
    val focusedIndex: StateFlow<Int> = _focusedIndex

    private val _noteReady = MutableStateFlow(false)
    val noteReady: StateFlow<Boolean> = _noteReady

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    fun setTitle(title: String) {
        _title.value = title
    }

    /**
     * 切换编辑模式
     */
    fun toggleEditing() {
        _isEditing.value = !_isEditing.value
    }

    fun loadNote(notePath: String = this.notePath) {
        _noteReady.value = false
        if (!notePath.isEmpty()) {
            viewModelScope.launch {
                val content = repository.readNote(notePath) ?: return@launch
                _blocks.value = content.content
                _title.value = content.note.config.title
                _note.value = content.note
                _noteReady.value = true
            }
        } else {
            Log.e("加载笔记", "笔记路径为空")
        }
    }

    fun saveNote() {
        if (_note.value != null) {
            viewModelScope.launch {
                repository.saveNote(_note.value!!, _blocks.value)
            }
        } else {
            Log.e("保存笔记", "笔记为 null")
        }
    }

    fun removeFocusedBlock() {
        removeBlockAt(_focusedIndex.value)
    }

    /**
     * 当光标位于文本块最开始时，删除前一个非文本块并合并文本
     *
     * @return 是否执行了删除操作
     */
    fun deletePreviousBlockIfAtStart(): Boolean {
        val currentIndex = _focusedIndex.value
        if (currentIndex <= 0) return false

        val currentBlock = _blocks.value[currentIndex]
        if (currentBlock !is Block.RichTextBlock) return false

        // 检查光标是否在最开始
        val selectionStart = currentBlock.state?.selection?.min ?: 0
        if (selectionStart != 0) return false

        val previousBlock = _blocks.value[currentIndex - 1]

        // 只处理前一个是图片块的情况
        if (previousBlock !is Block.ImageBlock) return false

        // 获取前后相邻的文本块
        val blocks = _blocks.value.toMutableList()
        val prevTextBlock = if (currentIndex - 2 >= 0) blocks[currentIndex - 2] as? Block.RichTextBlock else null

        // 合并文本内容
        val mergedText = buildString {
            prevTextBlock?.state?.toMarkdown()?.let { append(it) }
            currentBlock.state?.toMarkdown()?.let {
                if (isNotEmpty()) append("\n")
                append(it)
            }
        }

        // 创建合并后的新文本块
        val mergedBlock = Block.RichTextBlock(initialText = mergedText)

        // 删除图片块和当前文本块
        blocks.removeAt(currentIndex)
        blocks.removeAt(currentIndex - 1)

        // 替换前面的文本块
        if (prevTextBlock != null) {
            blocks[currentIndex - 2] = mergedBlock
        } else {
            blocks.add(0, mergedBlock)
        }

        _blocks.value = blocks
        _focusedIndex.value = if (prevTextBlock != null) currentIndex - 2 else 0

        return true
    }

    fun removeBlockAt(index: Int) {
        val block = _blocks.value[index]
        when (block) {
            is Block.RichTextBlock -> {}
            is Block.ImageBlock -> {
                val blocks = _blocks.value.toMutableList()

                // 获取前后相邻的文本块（需要安全转换类型）
                val prevBlock = if (index > 0) blocks[index - 1] as? Block.RichTextBlock else null
                val nextBlock = if (index < blocks.lastIndex) blocks[index + 1] as? Block.RichTextBlock else null

                if (prevBlock == null || nextBlock == null) {
                    Log.e("删除块", "图片：相邻的文本块不存在")
                    return
                }

                // 合并文本内容
                val mergedText = buildString {
                    prevBlock.state?.toMarkdown()?.let { append(it) }
                    append("\n")
                    nextBlock.state?.toMarkdown()?.let { append(it) }
                }

                // 创建合并后的新文本块
                val mergedBlock = Block.RichTextBlock(initialText = mergedText)

                blocks.removeAt(index + 1)
                blocks.removeAt(index)
                blocks[index - 1] = mergedBlock

                _blocks.value = blocks
                _focusedIndex.value = index - 1
            }
        }
    }

    fun setFocusedIndex(index: Int) {
        _focusedIndex.value = index.coerceIn(0, _blocks.value.lastIndex)
        Log.d("UI", "聚焦索引： $index")
    }

    /**
     * 由界面层调用，传入用户选中的图片 URI 列表
     *
     * @param note 当前笔记
     * @param uris 图片 URI 列表
     * @param context 上下文
     * @param fullContent 文本块[Block.RichTextBlock]的全部文本内容（ Markdown 格式）
     * @param selectionStart 选中开始位置
     * @param selectionEnd 选中结束位置
     */
    fun onImagesSelected(
        note: Note,
        uris: List<Uri>,
        fullContent: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        // TODO 改为先显示占位图，异步加载图片，以解决性能问题
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val paths = uris.mapNotNull { uri ->
                copyUriToNote(note, uri) // 复制，返回本地路径
            }
            if (paths.isEmpty()) return@launch
            // 在主线程更新 blocks
            withContext(Dispatchers.Main) {
                insertImageBlocks(paths, fullContent, selectionStart, selectionEnd)
            }
        }
    }

    private suspend fun copyUriToNote(note: Note, uri: Uri): String? {
        return repository.copyImageToNote(note, uri)
    }

    /**
     * 将图片路径插入为 ImageBlock
     */
    private fun insertImageBlocks(
        paths: List<String>,
        fullContent: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        // 分割内容保留格式
        val leftContent = fullContent.take(selectionStart)
        val rightContent = fullContent.substring(selectionEnd)

        // 创建新的文本块保持原有格式
        val leftBlock = Block.RichTextBlock(initialText = leftContent)
        val rightBlock = Block.RichTextBlock(initialText = rightContent)

        val blocks = _blocks.value.toMutableList()

        // 要替换的新列表
        val newBlocks = mutableListOf<Block>()
        newBlocks.add(leftBlock)
        paths.forEachIndexed { index, path ->
            newBlocks.add(Block.ImageBlock(image = ImageData(path)))
            if (index != paths.lastIndex) {
                newBlocks.add(Block.RichTextBlock())
            }
        }
        newBlocks.add(rightBlock)

        // 替换当前焦点位置的块为新的块列表
        blocks.removeAt(_focusedIndex.value)
        blocks.addAll(_focusedIndex.value, newBlocks)

        _blocks.value = blocks
    }
}