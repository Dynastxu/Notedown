package com.dynastxu.notedown.repository

import android.net.Uri
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.Folder
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.models.data.note.NoteContent
import java.io.File

interface NoteRepository {
    // TODO 建立异常类以应对操作失败
    suspend fun createNote(folder: Folder): Note

    suspend fun readNote(notePath: String): NoteContent?

    suspend fun saveNote(note: Note, content: List<Block>)

    suspend fun delNote(note: Note)

    suspend fun delNotes(notes: List<Note>)

    /**
     * 创建文件夹
     * @param folder 父级文件夹
     * @param name 文件夹名称
     * @return 新创建的文件夹
     */
    suspend fun createFolder(folder: Folder, name: String): Folder

    suspend fun delFolder(folder: Folder)

    suspend fun delFolders(folders: List<Folder>)

    /**
     * 将 Uri 指向的图片复制至指定笔记
     *
     * @param note 笔记
     * @param uri 图片 Uri
     * @return 图片绝对路径
     */
    suspend fun copyImageToNote(note: Note, uri: Uri): String?

    /**
     * 创建笔记根目录
     *
     * @return 根目录
     */
    suspend fun createNotedownRootFolder(): File?

    /**
     * 扫描指定目录下的笔记和文件夹
     *
     * @param dir 扫描目录
     * @return 扫描结果
     */
    suspend fun scanFoldersAndNotes(dir: File): ScanResult?

    /**
     * [scanFoldersAndNotes] 的扫描结果
     */
    data class ScanResult(val folders: List<Folder>, val notes: List<Note>)
}