package com.dynastxu.notedown.models.data.note

import com.dynastxu.notedown.models.data.Tag
import java.util.Date

data class NoteConfig(
    var version: Int = 1,
    var title: String = "",
    var tags: List<Tag> = emptyList(),
    var createDate: Date? = null,
    var editDate: Date? = null,
    var readDate: Date? = null
)
