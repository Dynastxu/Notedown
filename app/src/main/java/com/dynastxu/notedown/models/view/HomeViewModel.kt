package com.dynastxu.notedown.models.view

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynastxu.notedown.models.data.Folder
import com.dynastxu.notedown.models.data.Note
import com.dynastxu.notedown.models.data.NoteConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel : ViewModel() {
    private val _currentNotesList = MutableStateFlow<List<Note>>(emptyList())
    val currentNotesList: StateFlow<List<Note>> = _currentNotesList

    private val _currentFoldersList = MutableStateFlow<List<Folder>>(emptyList())
    val currentFoldersList: StateFlow<List<Folder>> = _currentFoldersList

    private val _selectMode = MutableStateFlow(false)
    val selectMode: StateFlow<Boolean> = _selectMode

    private val _selections = MutableStateFlow<List<Int>>(emptyList())
    val selections: StateFlow<List<Int>> = _selections

    private val gson = Gson()

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
        _selections.value.forEach { index ->
            if (index >= _currentFoldersList.value.size) {
                val newIndex = index - _currentFoldersList.value.size
                delete(_currentNotesList.value[newIndex])
                Log.i("用户删除", "删除了笔记 ${_currentNotesList.value[newIndex]}")
            } else {
                delete(_currentFoldersList.value[index])
                Log.i("用户删除", "删除了文件夹 ${_currentFoldersList.value[index]}")
            }
        }
        _selections.value = emptyList()
    }

    private fun delete(note: Note) {
        // TODO 这里为确保 UI 正确刷新取消了异步执行，之后需要换成显示删除进度，完成后刷新 UI
        deleteRecursively(note.folder)
    }

    private fun delete(folder: Folder) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteRecursively(folder.folder)
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
     * 扫描指定目录下包含 MD 文件的子文件夹
     *
     * @param dir 扫描目录
     */
    fun scanNoteFolders(dir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            // 检查目录是否存在且可读
            if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
                return@launch
            }
            val notesList = mutableListOf<Note>()
            val folderList = mutableListOf<Folder>()
            // 获取所有子文件和子目录
            val files = dir.listFiles() ?: return@launch
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
            _currentNotesList.value = notesList.toList()
            _currentFoldersList.value = folderList.toList()
        }
    }

    private fun isNoteFolder(dir: File): Boolean {
        // 获取所有子文件和子目录
        val files = dir.listFiles() ?: return false
        // 检查当前目录是否包含 MD 文件
        val hasMdFiles = files.any { it.isFile && it.extension.lowercase() == "md" }
        return hasMdFiles
    }

    private fun readNoteConfigs(folder: File): NoteConfig {
        val configFile = File(folder, "config.js")
        // 如果 config.js 文件不存在，使用默认值
        if (!configFile.exists() || !configFile.isFile) {
            return NoteConfig()
        }

        val configContent = configFile.readText(Charsets.UTF_8)
        return gson.fromJson(configContent, NoteConfig::class.java)
    }

    private fun countNotesNum(folder: File): Int {
        // TODO
        return 0
    }
}