package com.dynastxu.notedown.models.data

import java.util.Date

data class NoteConfig(
    var version: Int = 1,
    var title: String = "",
    var tags: List<Tag> = emptyList(),
    var createDate: Date? = null,
    var editDate: Date? = null,
    var readDate: Date? = null
) {
    fun update(config: NoteConfig) {
        if (config.version != 1) version = config.version
        if (config.title != "") title = config.title
        if (config.tags.isNotEmpty()) tags = config.tags
        if (config.createDate != null) createDate = config.createDate
        if (config.editDate != null) editDate = config.editDate
        if (config.readDate != null) readDate = config.readDate
    }
}
