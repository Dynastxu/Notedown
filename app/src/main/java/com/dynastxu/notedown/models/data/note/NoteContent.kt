package com.dynastxu.notedown.models.data.note

import com.dynastxu.notedown.models.data.Block

data class NoteContent(
    val note: Note,
    val content: List<Block>
)
