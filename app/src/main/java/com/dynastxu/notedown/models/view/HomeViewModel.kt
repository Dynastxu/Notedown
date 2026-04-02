package com.dynastxu.notedown.models.view

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Folder
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepository
): ViewModel() {
    // 用于通知 UI 文件夹是否准备就绪
    private val _folderReady = MutableStateFlow<File?>(null)
    val folderReady: StateFlow<File?> = _folderReady

    // 用于通知新建笔记是否准备就绪
    private val _newNoteReady = MutableStateFlow<Note?>(null)
    val newNoteReady: StateFlow<Note?> = _newNoteReady

    private val _currentFolder = MutableStateFlow<Folder?>(null)
    val currentFolder: StateFlow<Folder?> = _currentFolder

    private val _currentNotesList = MutableStateFlow<List<Note>>(emptyList())
    val currentNotesList: StateFlow<List<Note>> = _currentNotesList

    private val _currentFoldersList = MutableStateFlow<List<Folder>>(emptyList())
    val currentFoldersList: StateFlow<List<Folder>> = _currentFoldersList

    private val _selectMode = MutableStateFlow(false)
    val selectMode: StateFlow<Boolean> = _selectMode

    private val _selections = MutableStateFlow<List<Int>>(emptyList())
    val selections: StateFlow<List<Int>> = _selections

    private val _route = MutableStateFlow<List<File>>(emptyList())
    val route: StateFlow<List<File>> = _route

    init {
        createNotedownRootFolder()
    }

    fun setCurrentFolder(folder: Folder) {
        _currentFolder.value = folder
    }

    /**
     * 创建笔记根目录
     */
    private fun createNotedownRootFolder() {
        viewModelScope.launch {
            val folder = repository.createNotedownRootFolder()
            if (folder == null) {
                Log.e("创建失败", "创建笔记根目录失败")
                return@launch
            }
            // 回到主线程更新状态
            _currentFolder.value = Folder(folder)
            _folderReady.value = folder
        }
    }

    fun getSelectedFoldersAndNotes(): List<File> {
        val selections = mutableListOf<File>()
        _selections.value.forEach { index ->
            if (index >= _currentFoldersList.value.size) {
                val newIndex = index - _currentFoldersList.value.size
                selections.add(_currentNotesList.value[newIndex].folder)
            } else {
                selections.add(_currentFoldersList.value[index].folder)
            }
        }
        return selections
    }

    fun calculateRoute(root: File, current: File) {
        val fromPath = root.absolutePath
        val toPath = current.absolutePath

        // 确保 toPath 是 fromPath 的子目录
        if (!toPath.startsWith(fromPath)) {
            Log.e("路径错误", "toPath 必须是 fromPath 的子目录")
            _route.value = listOf(root)
        }

        // 移除前缀并分割路径
        val relativePath = toPath.removePrefix(fromPath).removePrefix("/")

        // 如果相对路径为空，说明两者相同，返回空列表
        if (relativePath.isBlank()) _route.value = listOf(root)

        // 按分隔符分割并过滤空字符串
        val pathSegments = relativePath.split("/").filter { it.isNotBlank() }

        // 构建 File 对象列表
        _route.value = buildList {
            add(root)
            var currentFile = root
            for (segment in pathSegments) {
                currentFile = File(currentFile, segment)
                add(currentFile)
            }
        }
    }

    fun setSelectMode(selectMode: Boolean) {
        _selectMode.value = selectMode
        _selections.value = emptyList()
    }

    fun select(index: Int) {
        val newList = _selections.value.toMutableList()
        if (_selections.value.contains(index)) {
            newList.remove(index)
        } else {
            newList.add(index)
        }
        _selections.value = newList
        Log.d("用户选择", newList.toString())
    }

    fun onDelete() {
        val notesToDelete = mutableListOf<Note>()
        val foldersToDelete = mutableListOf<Folder>()
        _selections.value.forEach { index ->
            if (index >= _currentFoldersList.value.size) {
                val newIndex = index - _currentFoldersList.value.size
                notesToDelete.add(_currentNotesList.value[newIndex])
                Log.i("用户删除", "删除了笔记 ${_currentNotesList.value[newIndex]}")
            } else {
                foldersToDelete.add(_currentFoldersList.value[index])
                Log.i("用户删除", "删除了文件夹 ${_currentFoldersList.value[index]}")
            }
        }
        viewModelScope.launch {
            repository.delNotes(notesToDelete)
            repository.delFolders(foldersToDelete)
        }
        _selections.value = emptyList()
    }

    /**
     * 扫描指定目录下的笔记和文件夹
     *
     * @param dir 扫描目录
     */
    fun scanFoldersAndNotes(dir: File) {
        viewModelScope.launch {
            val scanResult = repository.scanFoldersAndNotes(dir)
            if (scanResult != null) {
                _currentNotesList.value = scanResult.notes
                _currentFoldersList.value = scanResult.folders
            } else {
                Log.e("扫描失败", "扫描结果为 null")
            }
        }
    }

    fun createNewNote() {
        if (_currentFolder.value == null) {
            Log.e("创建笔记", "当前文件夹为 null")
            return
        }
        viewModelScope.launch {
            _newNoteReady.value = repository.createNote(_currentFolder.value!!)
        }
    }

    /**
     * 消费：新笔记创建完成
     */
    fun newNoteReadyConsume() {
        _newNoteReady.value = null
    }

    fun createNewFolder(folderName: String, defaultName: String) {
        if (_currentFolder.value == null) {
            Log.e("创建文件夹", "当前文件夹为 null")
            return
        }
        viewModelScope.launch {
            if (folderName.isBlank()) {
                repository.createFolder(_currentFolder.value!!, defaultName)
            } else {
                repository.createFolder(_currentFolder.value!!, folderName)
            }
        }
    }
}