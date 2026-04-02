package com.dynastxu.notedown.models.data

import java.io.File

data class Folder(
    val folder: File,
    val notesNum: Int = 0
)