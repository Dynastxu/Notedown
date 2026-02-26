package com.dynastxu.notedown.models.view

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Block
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditorViewModel : ViewModel() {
    private val _blocks = MutableStateFlow<List<Block>>(listOf(Block.RichTextBlock()))
    val blocks = _blocks.asStateFlow()

    private val _focusedIndex = MutableStateFlow(0)
    val focusedIndex = _focusedIndex.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    fun save() {
        viewModelScope.launch {
            _blocks.value.forEach { block ->
                when (block) { // TODO
                    is Block.RichTextBlock -> {

                    }
                    is Block.ImageBlock -> {

                    }
                }
            }
        } // TODO 保存逻辑
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

    /**
     * 由界面层调用，传入用户选中的图片 URI 列表
     */
    fun onImagesSelected(uris: List<Uri>, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachePaths = uris.mapNotNull { uri ->
                copyUriToCache(uri, context) // 复制到缓存，返回本地路径
            }
            // 在主线程更新 blocks
            withContext(Dispatchers.Main) {
                insertImageBlocks(cachePaths)
            }
        }
    }

    /**
     * 将 Uri 指向的图片复制到应用专属缓存目录，返回文件绝对路径
     */
    private suspend fun copyUriToCache(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver // 需使用 AndroidViewModel 或通过构造注入 Context
        // 注意：若使用 AndroidViewModel，可获取 Application Context
        val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
        // 获取 MIME 类型并转换为对应扩展名
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when {
            mimeType.startsWith("image/jpeg") -> ".jpg"
            mimeType.startsWith("image/png") -> ".png"
            mimeType.startsWith("image/gif") -> ".gif"
            mimeType.startsWith("image/webp") -> ".webp"
            mimeType.startsWith("image/bmp") -> ".bmp"
            else -> ".jpg" // 默认回退到 jpg
        }
        val fileName = "img_${UUID.randomUUID()}$extension"
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        cacheFile.absolutePath
    }

    /**
     * 将图片路径插入为 ImageBlock（示例逻辑）
     */
    private fun insertImageBlocks(paths: List<String>) {
        paths.forEach { path ->
            addBlockAfter(_focusedIndex.value, Block.ImageBlock(src = path))
        }
    }
}