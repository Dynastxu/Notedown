package com.dynastxu.notedown.models.data

sealed class Block {
    /**
     * 块可保存接口
     */
    interface IBlockPreSaveAble {
        /**
         * 保存更改
         */
        fun save(): Block
    }

    /**
     * 文本块
     *
     * @param text 文本内容
     * @param isReadOnly 是否只读
     */
    open class TextBlock(
        open var text: String = "",
        open var isReadOnly: Boolean = false
    ) : Block(), IBlockPreSaveAble {
        var savedText: String = ""
            private set

        init {
            savedText = text
        }

        override fun save(): TextBlock {
            savedText = text
            return this
        }
    }

    /**
     * 标题块
     *
     * @param savedText 文本内容
     * @param level 标题级别
     */
    class HeadingBlock(
        override var text: String,
        override var isReadOnly: Boolean,
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