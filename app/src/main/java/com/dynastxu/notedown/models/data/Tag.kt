package com.dynastxu.notedown.models.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

class Tag (name: String, color: Color, icon: ImageVector) {
    val id = UUID.randomUUID()!!
}