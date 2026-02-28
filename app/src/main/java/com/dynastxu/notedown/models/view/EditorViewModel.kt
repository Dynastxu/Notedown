package com.dynastxu.notedown.models.view

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.regex.Pattern

class EditorViewModel : ViewModel() {
    companion object {
        // Markdown 图片语法正则表达式: ![alt](src)
        private val IMAGE_PATTERN = Pattern.compile(
            "!\\[((?:[^]\\\\]|\\\\.)*)]\\(((?:[^)\\\\]|\\\\.)*)\\)"
        )

        private fun escapeMarkdownSpecialChars(input: String): String {
            // 需要转义的字符：\ [ ] ( )
            return input
                .replace("\\", "\\\\")  // 反斜杠优先转义
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
        }

        private fun unescapeMarkdownSpecialChars(input: String): String {
            // 反转义：将转义序列还原
            // 注意顺序：先处理双反斜杠，再处理其他
            return input
                .replace("\\\\", "\\")  // 两个反斜杠变成一个
                .replace("\\[", "[")
                .replace("\\]", "]")
                .replace("\\(", "(")
                .replace("\\)", ")")
        }
    }

    private val _blocks = MutableStateFlow<List<Block>>(listOf(Block.RichTextBlock()))
    val blocks: StateFlow<List<Block>> = _blocks

    private val _focusedIndex = MutableStateFlow(0)
    val focusedIndex: StateFlow<Int> = _focusedIndex

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _noteReady = MutableStateFlow(false)
    val noteReady: StateFlow<Boolean> = _noteReady

    fun readNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("读取笔记", "正在读取")
            _blocks.value = emptyList()

            try {
                // 读取 MD 文件
                val mdFile = File(note.folder, "${note.folder.name}.md")
                if (mdFile.exists() && mdFile.isFile) {
                    val content = mdFile.readText(Charsets.UTF_8)
                    Log.d("读取笔记", "文本内容： \n$content")
                    // 解析内容并创建 blocks
                    val parsedBlocks = parseMarkdownContent(content)
                    // 在主线程更新 UI
                    withContext(Dispatchers.Main) {
                        _blocks.value = parsedBlocks
                    }
                } else {
                    // 文件不存在，创建默认的文本块
                    withContext(Dispatchers.Main) {
                        _blocks.value = listOf(Block.RichTextBlock())
                    }
                }
            } catch (e: Exception) {
                // 出错时创建默认的文本块
                withContext(Dispatchers.Main) {
                    _blocks.value = listOf(Block.RichTextBlock())
                }
                Log.e("读取笔记", "读取失败： $e")
            }

            _noteReady.value = true
            Log.i("读取笔记", "读取成功")
        }
    }

    /**
     * 解析 Markdown 内容，将文本和图片分离成不同的 block
     */
    private fun parseMarkdownContent(content: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val matcher = IMAGE_PATTERN.matcher(content)
        var lastIndex = 0

        // 查找所有图片
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val alt = unescapeMarkdownSpecialChars(matcher.group(1) ?: "")
            val src = unescapeMarkdownSpecialChars(matcher.group(2) ?: "")

            // 添加图片前面的文本块
            if (start > lastIndex) {
                // 如果有
                val textBeforeImage = content.substring(lastIndex, start)
                if (textBeforeImage.isNotBlank()) {
                    val textBlock = Block.RichTextBlock(initialText = textBeforeImage)
                    // 这里需要在 Compose 环境中创建 RichTextState
                    blocks.add(textBlock)
                }
            } else {
                // 如果没有
                val textBlock = Block.RichTextBlock()
                blocks.add(textBlock)
            }

            // 添加图片块
            blocks.add(Block.ImageBlock(src = src, alt = alt))

            lastIndex = end
        }

        // 添加最后剩余的文本
        if (lastIndex < content.length) {
            // 如果有
            val remainingText = content.substring(lastIndex)
            if (remainingText.isNotBlank()) {
                val textBlock = Block.RichTextBlock(initialText = remainingText)
                // 同样，这里需要在 Compose 环境中处理
                blocks.add(textBlock)
            }
        }

        // 如果没有任何内容，至少添加一个空的文本块
        if (blocks.isEmpty()) {
            blocks.add(Block.RichTextBlock())
        }

        return blocks
    }

    fun save(note: Note) {
        viewModelScope.launch {
            Log.i("保存笔记", "正在保存")
            val content = StringBuilder()
            _blocks.value.forEachIndexed { index, block ->
                when (block) {
                    is Block.RichTextBlock -> {
                        content.append(block.state?.toMarkdown())
                    }

                    is Block.ImageBlock -> {
                        val escapedSrc = escapeMarkdownSpecialChars(block.src)
                        val escapedAlt = escapeMarkdownSpecialChars(block.alt)
                        content.append("\n\n![${escapedAlt}](${escapedSrc})\n\n")
                    }
                }
                val isLastBlock = index == _blocks.value.lastIndex
                if (!isLastBlock) {
                    content.append("\n\n")
                }
            }
            try {
                val file = File(note.folder, "${note.folder.name}.md")
                // 使用 writeText 会自动创建文件，无需先调用 createNewFile()
                file.writeText(content.toString())
                Log.i("保存笔记", "保存成功")
            } catch (e: Exception) {
                // 捕获并记录具体异常信息
                Log.e("保存笔记", "保存失败: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("保存笔记", "堆栈跟踪:", e)
                // 可以在这里添加更多的错误处理逻辑
            }
        }
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
     *
     *
     */
    fun onImagesSelected(
        note: Note,
        uris: List<Uri>,
        context: Context,
        fullContent: String,
        selectionStart: Int,
        selectionEnd: Int
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val paths = uris.mapNotNull { uri ->
                val folder = note.folder
                copyUriToFolder(folder, uri, context) // 复制，返回本地路径
            }
            // 在主线程更新 blocks
            withContext(Dispatchers.Main) {
                insertImageBlocks(paths, fullContent, selectionStart, selectionEnd)
            }
        }
    }

    /**
     * 将 Uri 指向的图片复制指定目录，返回文件绝对路径
     *
     * @param folder 指定目录
     * @param uri 图片 Uri
     * @param context 上下文
     */
    private suspend fun copyUriToFolder(folder: File, uri: Uri, context: Context): String? =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
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
            val file = File(folder, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
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
            newBlocks.add(Block.ImageBlock(src = path))
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