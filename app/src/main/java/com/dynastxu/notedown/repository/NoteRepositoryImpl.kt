package com.dynastxu.notedown.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.ImageData
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.models.data.note.NoteConfig
import com.dynastxu.notedown.models.data.note.NoteContent
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID
import java.util.regex.Pattern

class NoteRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : NoteRepository {
    companion object {
        // Markdown 图片语法正则表达式: ![alt](src)
        private val IMAGE_PATTERN = Pattern.compile(
            "!\\[((?:[^]\\\\]|\\\\.)*)]\\(((?:[^)\\\\]|\\\\.)*)\\)"
        )

        /**
         * 转义 Markdown 特殊字符
         */
        private fun escapeMarkdownSpecialChars(input: String): String {
            // 需要转义的字符：\ [ ] ( )
            return input
                .replace("\\", "\\\\")  // 反斜杠优先转义
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
        }

        /**
         * 反转义 Markdown 特殊字符
         */
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

    private suspend fun getNoteByPath(path: String): Note? = withContext(Dispatchers.IO) {
        val folder = File(path)
        if (!folder.exists() || !folder.isDirectory) return@withContext null
        // 读取配置等
        val configFile = File(folder, "config.js")
        val config = if (configFile.exists()) {
            Gson().fromJson(configFile.readText(), NoteConfig::class.java)
        } else {
            NoteConfig()
        }
        Note(folder, config)
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
                    // 如果最后一个块已经是文本块，则合并内容；否则添加新文本块
                    if (blocks.isNotEmpty() && blocks.last() is Block.RichTextBlock) {
                        val lastTextBlock = blocks.last() as Block.RichTextBlock
                        val currentText = lastTextBlock.state?.toMarkdown() ?: ""
                        lastTextBlock.state?.setMarkdown(currentText + textBeforeImage)
                    } else {
                        blocks.add(Block.RichTextBlock(initialText = textBeforeImage.trimEnd()))
                    }
                }
            }

            // 确保第一个块是文本块：如果当前为空列表，则添加空文本块
            if (blocks.isEmpty()) {
                blocks.add(Block.RichTextBlock())
            }

            // 添加图片块（此时前一个块保证是文本块）
            blocks.add(Block.ImageBlock(image = ImageData(src, alt)))

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

    /**
     * 将 Uri 指向的图片复制指定目录，返回文件绝对路径
     *
     * @param folder 指定目录
     * @param uri 图片 Uri
     */
    private suspend fun copyUriToFolder(folder: File, uri: Uri): String? =
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

    override suspend fun readNote(notePath: String): NoteContent? {
        Log.i("读取笔记", "路径： $notePath")
        val note = getNoteByPath(notePath) ?: return null
        val gson = Gson()
        try {
            // 读取 MD 文件
            val mdFile = File(note.folder, "${note.folder.name}.md")
            if (mdFile.exists() && mdFile.isFile) {
                val content = mdFile.readText()
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
                note.config = config
                return NoteContent(note, parsedBlocks)
            } else {
                Log.e("读取笔记", "读取失败：文件不存在或路径不正确")
                return null
            }
        } catch (e: Exception) {
            Log.e("读取笔记", "读取失败： $e")
            return null
        }
    }

    override suspend fun saveNote(
        note: Note,
        content: List<Block>
    ) {
        Log.i("保存笔记", "正在保存")
        val stringContent = StringBuilder()
        content.forEach { block ->
            when (block) {
                is Block.RichTextBlock -> {
                    stringContent.append(block.state?.toMarkdown())
                }

                is Block.ImageBlock -> {
                    val escapedSrc =
                        escapeMarkdownSpecialChars(block.image.src)
                    val escapedAlt =
                        escapeMarkdownSpecialChars(block.image.alt)
                    stringContent.append("\n\n![${escapedAlt}](${escapedSrc})\n\n")
                }
            }
        }
        val mdFile = File(note.folder, "${note.folder.name}.md")
        mdFile.writeText(stringContent.toString())
        // 更新配置
        val gson = Gson()
        note.config.editDate = Date()
        val configFile = File(note.folder, "config.js")
        configFile.writeText(gson.toJson(note.config))
        Log.i("保存笔记", "保存成功")
    }

    override suspend fun copyImageToNote(
        note: Note,
        uri: Uri
    ): String? {
        val folder = File(note.folder, "imgs").apply {
            if (!exists()) mkdirs()
        }
        return copyUriToFolder(folder, uri)
    }
}