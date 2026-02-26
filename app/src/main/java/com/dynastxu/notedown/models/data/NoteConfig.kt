package com.dynastxu.notedown.models.data

import java.util.Date

data class NoteConfig(
    val version: Int = 1,
    val title: String = "",
    val tags: List<Tag> = emptyList(),
    val createDate: Date? = null,
    val editDate: Date? = null,
    val readDate: Date? = null
)
