package com.dynastxu.notedown.models.view

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // 用于通知 UI 文件夹是否准备就绪
    private val _folderReady = MutableStateFlow<File?>(null)
    val folderReady: StateFlow<File?> = _folderReady

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _selectedImage = MutableStateFlow<ImageData?>(null)
    val selectedImage: StateFlow<ImageData?> = _selectedImage

    private val _currentFolder = MutableStateFlow<File?>(null)
    val currentFolder: StateFlow<File?> = _currentFolder

    /**
     * 当顶部栏编辑按钮被按下时执行
     */
    var onEditBtnPressed = {}

    init {
        // 在 ViewModel 创建时自动开始创建文件夹
        createNotedownFolder()
    }

    fun setSelectedImage(image: ImageData) {
        _selectedImage.value = image
    }

    /**
     * 按下顶部栏编辑按钮
     */
    fun pressEditBtn() {
        _isEditing.value = !_isEditing.value
        onEditBtnPressed()
    }

    fun setIsEditing(isEditing: Boolean) {
        _isEditing.value = isEditing
    }

    fun selectNote(note: Note) {
        _selectedNote.value = note
    }

    fun createNewNote() {
        _selectedNote.value = createNewNote(_currentFolder.value!!)
        _isEditing.value = true
    }

    private fun createNewNote(folder: File): Note {
        val noteFolder = createUniqueFolder(folder, LocalDate.now().toString())
        val config = NoteConfig()
        if (noteFolder.mkdirs()) {
            File(noteFolder, "imgs").mkdirs()
            File(noteFolder, "${noteFolder.name}.md").createNewFile()
            val configFile = File(noteFolder, "config.js")
            if (configFile.createNewFile()) {
                // 将配置写入文件
                configFile.writeText(Gson().toJson(config))
            }
        }
        return Note(noteFolder, config)
    }

    /**
     * 创建唯一名称的文件夹，如果已存在则添加数字后缀
     *
     * @param parent 父文件夹
     * @param baseName 基础名称
     * @return 新的文件夹对象
     */
    private fun createUniqueFolder(parent: File, baseName: String): File {
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
                _currentFolder.value = notedownFolder
                notedownFolder
            }
            // 回到主线程更新状态
            _folderReady.value = folder
        }
    }
}