package info.plateaukao.einkbro.unit

/**
 * A piece of an AI answer the Compose result dialog renders on its own: plain
 * markdown text goes through [MarkdownParser] as before, while an image needs a
 * view of its own (an AnnotatedString can't carry one).
 */
sealed interface MarkdownBlock {
    data class Text(val markdown: String) : MarkdownBlock
    data class Image(val url: String, val alt: String) : MarkdownBlock
}

object MarkdownBlocks {
    // ![alt](url "optional title"); group 1 = alt, group 2 = URL.
    private val imagePattern = Regex("!\\[([^\\]]*)\\]\\(\\s*([^)\\s]+)(?:\\s+\"[^\"]*\")?\\s*\\)")

    /** True when [markdown] holds at least one image to render. */
    fun hasRichBlocks(markdown: String): Boolean = imagePattern.containsMatchIn(markdown)

    /** Splits [markdown] into text and image blocks, in document order. */
    fun split(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        var cursor = 0
        for (match in imagePattern.findAll(markdown)) {
            addText(blocks, markdown.substring(cursor, match.range.first))
            blocks.add(
                MarkdownBlock.Image(
                    url = match.groupValues[2],
                    alt = match.groupValues[1].trim(),
                )
            )
            cursor = match.range.last + 1
        }
        addText(blocks, markdown.substring(cursor))
        return blocks
    }

    private fun addText(blocks: MutableList<MarkdownBlock>, text: String) {
        if (text.isNotBlank()) blocks.add(MarkdownBlock.Text(text.trim()))
    }
}
