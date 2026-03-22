package com.dynastxu.notedown.repository

import android.net.Uri
import com.dynastxu.notedown.models.data.Block
import com.dynastxu.notedown.models.data.note.Note
import com.dynastxu.notedown.models.data.note.NoteContent

interface NoteRepository {
    suspend fun readNote(notePath: String): NoteContent?
    suspend fun saveNote(note: Note, content: List<Block>)
    /**
     * 将 Uri 指向的图片复制指定笔记，返回文件绝对路径
     *
     * @param note 指定笔记
     * @param uri 图片 Uri
     */
    suspend fun copyImageToNote(note: Note, uri: Uri): String?
}