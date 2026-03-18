package com.dynastxu.notedown.models.view

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.ImageData
import com.dynastxu.notedown.models.data.Note
import com.dynastxu.notedown.models.data.NoteConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
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

    private val _noteReady = MutableStateFlow(false)
    val noteReady: StateFlow<Boolean> = _noteReady

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val gson = Gson()

    fun setTitle(title: String) {
        _title.value = title
    }

    fun readNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("读取笔记", "正在读取")
//            _blocks.value = emptyList()

            try {
                // 读取 MD 文件
                val mdFile = File(note.folder, "${note.folder.name}.md")
                if (mdFile.exists() && mdFile.isFile) {
                    val content = mdFile.readText(Charsets.UTF_8)
                    Log.d("读取笔记", "文本内容： \n$content")
                    // 解析内容并创建 blocks
                    val parsedBlocks = parseMarkdownContent(content)

                    // 读取并更新配置文件
                    val configFile = File(note.folder, "config.js")
                    val config = if (configFile.exists() && configFile.isFile) {
                        val configContent = configFile.readText()
                        val noteConfig = gson.fromJson(configContent, NoteConfig::class.java)
                        // 更新读取时间
                        noteConfig.copy(readDate = Date()).also { updatedConfig ->
                            configFile.writeText(gson.toJson(updatedConfig))
                        }
                    } else {
                        // 如果配置文件不存在，创建新的配置
                        NoteConfig(readDate = Date()).also { newConfig ->
                            if (configFile.createNewFile()) {
                                configFile.writeText(gson.toJson(newConfig))
                            }
                        }
                    }
                    note.config.update(config)

                    // 在主线程更新 UI
                    withContext(Dispatchers.Main) {
                        _blocks.value = parsedBlocks
                    }
                } else {
                    // TODO 改为显示文件不存在
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
     * 解析 Markdown 内容，将文本和非文本分离成不同的 block
     *
     * @return 块列表，满足：1.第一个元素是文本块 2.最后一个元素是文本块 3.非文本块的相邻块是文本块
     */
    private fun parseMarkdownContent(content: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val matcher = IMAGE_PATTERN.matcher(content)
        var lastIndex = 0

        // 遍历所有图片标记，将内容分割成文本块和图片块
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val alt = unescapeMarkdownSpecialChars(matcher.group(1) ?: "")
            val src = unescapeMarkdownSpecialChars(matcher.group(2) ?: "")

            // 如果图片前有文本内容，则添加为文本块
            if (start > lastIndex) {
                val textBeforeImage = content.substring(lastIndex, start)
                if (textBeforeImage.isNotBlank()) {
                    val textBlock = Block.RichTextBlock(initialText = textBeforeImage.trimEnd())
                    blocks.add(textBlock)
                }
            }

            // 确保第一个块是文本块：如果当前为空列表或最后一个不是文本块，则添加空文本块
            if (blocks.isEmpty() || blocks.last() !is Block.RichTextBlock) {
                blocks.add(Block.RichTextBlock())
            }

            // 添加图片块（此时前一个块保证是文本块）
            blocks.add(Block.ImageBlock(image = ImageData(src, alt)))

            // 在图片块后立即添加空文本块，确保图片块后也有文本块
            blocks.add(Block.RichTextBlock())

            lastIndex = end
        }

        // 处理最后剩余的文本内容
        if (lastIndex < content.length) {
            val remainingText = content.substring(lastIndex)
            if (remainingText.isNotBlank()) {
                // 如果最后一个块已经是文本块，则合并内容；否则添加新文本块
                if (blocks.isNotEmpty() && blocks.last() is Block.RichTextBlock) {
                    val lastTextBlock = blocks.last() as Block.RichTextBlock
                    val currentText = lastTextBlock.state?.toMarkdown() ?: ""
                    lastTextBlock.state?.setMarkdown(currentText + remainingText)
                } else {
                    blocks.add(Block.RichTextBlock(initialText = remainingText))
                }
            }
        }

        // 如果没有任何内容，至少添加一个空文本块
        if (blocks.isEmpty()) {
            blocks.add(Block.RichTextBlock())
        }

        // 最终检查：确保最后一个块一定是文本块
        if (blocks.last() !is Block.RichTextBlock) {
            blocks.add(Block.RichTextBlock())
        }

        return blocks
    }

    fun save(note: Note) {
        viewModelScope.launch {
            Log.i("保存笔记", "正在保存")
            val content = StringBuilder()
            _blocks.value.forEach { block ->
                when (block) {
                    is Block.RichTextBlock -> {
                        content.append(block.state?.toMarkdown())
                    }

                    is Block.ImageBlock -> {
                        val escapedSrc = escapeMarkdownSpecialChars(block.image.src)
                        val escapedAlt = escapeMarkdownSpecialChars(block.image.alt)
                        content.append("\n\n![${escapedAlt}](${escapedSrc})\n\n")
                    }
                }
            }
            try {
                val file = File(note.folder, "${note.folder.name}.md")
                // 使用 writeText 会自动创建文件，无需先调用 createNewFile()
                file.writeText(content.toString())

                // 更新并保存配置文件
                val configFile = File(note.folder, "config.js")
                note.config.editDate = Date()
                note.config.title = _title.value
                if (configFile.exists() || configFile.createNewFile()) {
                    configFile.writeText(gson.toJson(note.config))
                }

                Log.i("保存笔记", "保存成功")
            } catch (e: Exception) {
                // 捕获并记录具体异常信息
                Log.e("保存笔记", "保存失败: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("保存笔记", "堆栈跟踪:", e)
                // 可以在这里添加更多的错误处理逻辑
            }
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
                val folder = File(note.folder, "imgs").apply { 
                    if (!exists()) mkdirs() 
                }
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