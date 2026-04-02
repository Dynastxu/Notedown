package com.dynastxu.notedown.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.Folder
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
import java.time.LocalDate
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

    private fun getNoteByPath(path: String): Note? {
        val folder = File(path)
        if (!folder.exists() || !folder.isDirectory) return null
        // 读取配置等
        val configFile = File(folder, "config.js")
        val config = if (configFile.exists()) {
            Gson().fromJson(configFile.readText(), NoteConfig::class.java)
        } else {
            NoteConfig()
        }
        return Note(folder, config)
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
     * @return 图片绝对路径
     */
    private fun copyUriToFolder(folder: File, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
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
        return file.absolutePath
    }

    override suspend fun createNote(folder: Folder): Note = withContext(Dispatchers.IO) {
        val noteFolder = uniqueFolder(folder.folder, UUID.randomUUID().toString())
        val config = NoteConfig(
            createDate = Date()
        )
        if (noteFolder.mkdirs()) {
            File(noteFolder, "imgs").mkdirs()
            File(noteFolder, "${LocalDate.now()}.md").createNewFile()
            val configFile = File(noteFolder, "config.js")
            if (configFile.createNewFile()) {
                // 将配置写入文件
                configFile.writeText(Gson().toJson(config))
            }
        }
        return@withContext Note(noteFolder, config)
    }

    override suspend fun readNote(notePath: String): NoteContent? = withContext(Dispatchers.IO) {
        Log.i("读取笔记", "路径： $notePath")
        val note = getNoteByPath(notePath) ?: return@withContext null
        val gson = Gson()
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
            return@withContext NoteContent(note, parsedBlocks)
        } else {
            Log.e("读取笔记", "读取失败：文件不存在或路径不正确")
            return@withContext null
        }
    }

    override suspend fun saveNote(
        note: Note,
        content: List<Block>
    ): Unit = withContext(Dispatchers.IO) {
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

    override suspend fun copyImageToNote(note: Note, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            val folder = File(note.folder, "imgs").apply {
                if (!exists()) mkdirs()
            }
            return@withContext copyUriToFolder(folder, uri)
        }

    override suspend fun createNotedownRootFolder(): File? = withContext(Dispatchers.IO) {
        // 在 IO 线程执行文件操作
        val documentsDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: return@withContext null
        val notedownFolder = File(documentsDir, "notedown")
        if (!notedownFolder.exists()) {
            if (!notedownFolder.mkdirs()) return@withContext null
        }
        return@withContext notedownFolder
    }

    override suspend fun scanFoldersAndNotes(dir: File): NoteRepository.ScanResult? = withContext(
        Dispatchers.IO
    ) {
        // 检查目录是否存在且可读
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return@withContext null
        }
        val notesList = mutableListOf<Note>()
        val folderList = mutableListOf<Folder>()
        // 获取所有子文件和子目录
        val files = dir.listFiles() ?: return@withContext null
        // 遍历所有文件
        files.forEach {
            if (it.isDirectory) {
                if (isNoteFolder(it)) {
                    notesList.add(
                        Note(it, readNoteConfigs(it))
                    )
                } else {
                    folderList.add(
                        Folder(it, countNotesNum(it))
                    )
                }
            }
        }
        NoteRepository.ScanResult(folderList, notesList)
    }

    private fun readNoteConfigs(folder: File): NoteConfig {
        val gson = Gson()
        val configFile = File(folder, "config.js")
        // 如果 config.js 文件不存在，使用默认值
        if (!configFile.exists() || !configFile.isFile) {
            return NoteConfig()
        }

        val configContent = configFile.readText(Charsets.UTF_8)
        return gson.fromJson(configContent, NoteConfig::class.java)
    }

    private fun countNotesNum(folder: File): Int {
        // TODO 异步处理
        if (!folder.exists() || !folder.isDirectory) return 0

        var count = 0
        val files = folder.listFiles() ?: return 0

        files.forEach { file ->
            if (file.isDirectory) {
                if (isNoteFolder(file)) {
                    count++
                } else {
                    count += countNotesNum(file)
                }
            }
        }

        return count
    }

    private fun isNoteFolder(dir: File): Boolean {
        // 获取所有子文件和子目录
        val files = dir.listFiles() ?: return false
        // 检查当前目录是否包含 MD 文件
        val hasMdFiles = files.any { it.isFile && it.extension.lowercase() == "md" }
        return hasMdFiles
    }

    override suspend fun delNote(note: Note): Unit = withContext(Dispatchers.IO) {
        if (deleteRecursively(note.folder)) {
            Log.i("删除笔记", "删除成功：${note.folder.absolutePath}")
        } else {
            Log.e("删除笔记", "删除失败：${note.folder.absolutePath}")
        }
    }

    override suspend fun delNotes(notes: List<Note>) {
        notes.forEach {
            delNote(it)
        }
    }

    override suspend fun createFolder(folder: Folder, name: String): Folder = withContext(
        Dispatchers.IO
    ) {
        val f = uniqueFolder(folder.folder, name).also { it.mkdirs() }
        return@withContext Folder(f)
    }

    override suspend fun delFolder(folder: Folder): Unit = withContext(Dispatchers.IO) {
        if (deleteRecursively(folder.folder)) {
            Log.i("删除文件夹", "删除成功：${folder.folder.absolutePath}")
        } else {
            Log.e("删除文件夹", "删除失败：${folder.folder.absolutePath}")
        }
    }

    override suspend fun delFolders(folders: List<Folder>) {
        folders.forEach {
            delFolder(it)
        }
    }

    /**
     * 递归删除文件或文件夹
     *
     * @param file 要删除的文件或文件夹
     * @return 删除是否成功
     */
    private fun deleteRecursively(file: File): Boolean {
        if (!file.exists()) return true

        return if (file.isDirectory) {
            // 如果是目录，先删除所有子文件和子目录
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
            // 然后删除空目录
            file.delete()
        } else {
            // 如果是文件，直接删除
            file.delete()
        }
    }

    /**
     * 创建唯一名称的文件夹，如果已存在则添加数字后缀
     *
     * @param parent 父文件夹
     * @param baseName 基础名称
     * @return 新的文件夹对象
     */
    private fun uniqueFolder(parent: File, baseName: String): File {
        var counter = 1
        var newName = baseName
        var newFolder = File(parent, newName)
        while (newFolder.exists()) {
            newName = "${baseName}(${counter})"
            newFolder = File(parent, newName)
            counter++
        }
        return newFolder
    }
}