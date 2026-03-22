package com.dynastxu.notedown.models.data.note

import java.io.File

data class Note(
    val folder: File,
    var config: NoteConfig
)
