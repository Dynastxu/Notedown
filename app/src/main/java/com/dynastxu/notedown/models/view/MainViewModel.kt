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

    private val _selectedImage = MutableStateFlow("")
    val selectedImage: StateFlow<String> = _selectedImage

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

    fun setSelectedImage(src: String) {
        _selectedImage.value = src
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
                _currentFolder.value = notedownFolder
                notedownFolder
            }
            // 回到主线程更新状态
            _folderReady.value = folder
        }
    }
}