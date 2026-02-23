package com.dynastxu.notedown.models.data

sealed class Block {
    /**
     * 文本块
     *
     * @param text 文本内容
     */
    open class TextBlock(
        open var text: String = ""
    ) : Block()

    /**
     * 标题块
     *
     * @param text 文本内容
     * @param level 标题级别
     */
    class HeadingBlock(
        override var text: String,
        var level: Int = 1
    ) : TextBlock()

    /**
     * 图片块
     *
     * @param imageRes 图片资源
     * @param altText 图片描述
     */
    class ImageBlock(
        val imageRes: String,
        val altText: String
    ) : Block()
}