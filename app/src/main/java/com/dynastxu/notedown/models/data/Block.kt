package com.dynastxu.notedown.models.data

import com.mohamedrejeb.richeditor.model.RichTextState

sealed class Block {
    /**
     * 文本块
     *
     * @param state 文本内容
     * @param initialText 初始文本参数
     */
    data class RichTextBlock(
        var state: RichTextState? = null,
        val initialText: String = ""
    ) : Block()

    data class ImageBlock(
        var src: String = "",
        var alt: String = ""
    ) : Block()
}