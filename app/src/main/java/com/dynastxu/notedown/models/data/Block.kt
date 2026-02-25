package com.dynastxu.notedown.models.data

import com.mohamedrejeb.richeditor.model.RichTextState

sealed class Block {
    /**
     * 文本块
     *
     * @param text 文本内容
     * @param isReadOnly 是否只读
     */
    data class RichTextBlock(
        var text: String = "",
        var state: RichTextState? = null
    ) : Block()
}