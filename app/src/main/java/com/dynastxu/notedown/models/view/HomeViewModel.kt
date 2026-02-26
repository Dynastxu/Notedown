package com.dynastxu.notedown.models.view

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

    private val gson = Gson()

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