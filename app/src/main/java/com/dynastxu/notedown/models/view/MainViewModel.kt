package com.dynastxu.notedown.models.view

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // 用于通知 UI 文件夹是否准备就绪
    private val _folderReady = MutableStateFlow<File?>(null)
    val folderReady: StateFlow<File?> = _folderReady

    private val _selectedNote = MutableStateFlow("")
    val selectedNote: StateFlow<String> = _selectedNote

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _notes = MutableStateFlow<List<String>>(emptyList())
    val notes: StateFlow<List<String>> = _notes

    private val _selectedImage = MutableStateFlow("")
    val selectedImage: StateFlow<String> = _selectedImage

    /**
     * 当顶部栏编辑按钮被按下时执行
     */
    var onEditBtnPressed = {}

    init {
        // 在 ViewModel 创建时自动开始创建文件夹
        createNotedownFolder()

        // 监听文件夹准备状态变化
        viewModelScope.launch {
            _folderReady.collect { folder ->
                if (folder != null) {
                    initNotesList()
                }
            }
        }
    }

    fun setSelectedImage(src: String) {
        _selectedImage.value = src
    }

    fun initNotesList() {
        val folder = _folderReady.value
        if (folder == null) {
            _notes.value = emptyList()
            return
        }

        // 检查文件夹是否存在且可读
        if (!folder.exists() || !folder.canRead()) {
            _notes.value = emptyList()
            return
        }

        val files = folder.listFiles()
        if (files.isNullOrEmpty()) {
            _notes.value = emptyList()
        } else {
            scanMdFolders(folder)
        }
    }

    fun editBtnPressed() {
        _isEditing.value = !_isEditing.value
        onEditBtnPressed()
    }

    fun selectNote(note: String = "") {
        _selectedNote.value = note
    }

    private fun createNotedownFolder() {
        viewModelScope.launch {
            val folder = withContext(Dispatchers.IO) {
                // 在 IO 线程执行文件操作
                val documentsDir =
                    getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: return@withContext null
                val notedownFolder = File(documentsDir, "notedown")
                if (!notedownFolder.exists()) {
                    if (!notedownFolder.mkdirs()) return@withContext null
                }
                notedownFolder
            }
            // 回到主线程更新状态
            _folderReady.value = folder
        }
    }

    /**
     * 扫描指定目录下包含 MD 文件的子文件夹
     * 第一层不会包含 MD 文件，需要递归遍历子文件夹
     *
     * @param rootDir 根目录
     */
    private fun scanMdFolders(rootDir: File) {
        viewModelScope.launch {
            val foldersWithMd = withContext(Dispatchers.IO) {
                findFoldersWithMdFiles(rootDir)
            }
            _notes.value = foldersWithMd
        }
    }

    /**
     * 递归查找包含MD文件的文件夹
     *
     * @param dir 要搜索的目录
     * @return 包含 MD 文件的文件夹路径列表
     */
    private fun findFoldersWithMdFiles(dir: File): List<String> {
        val result = mutableListOf<String>()

        // 获取所有子文件和子目录
        val files = dir.listFiles() ?: return result

        // 检查当前目录是否包含 MD 文件
        val hasMdFiles = files.any { it.isFile && it.extension.lowercase() == "md" }

        if (hasMdFiles) {
            // 如果当前目录包含 MD 文件，添加到结果中
            result.add(dir.absolutePath)
        }

        // 递归检查所有子目录
        files.filter { it.isDirectory }.forEach { subDir ->
            result.addAll(findFoldersWithMdFiles(subDir))
        }

        return result
    }
}