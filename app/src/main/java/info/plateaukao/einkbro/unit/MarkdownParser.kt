package info.plateaukao.einkbro.unit

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object MarkdownParser {
    private const val DEFAULT_FONT_SIZE = 18

    /**
     * Parses a given markdown text and converts it into an [AnnotatedString] with appropriate styles.
     * from: mdparserkitcore/src/main/java/com/daksh/mdparserkit/core/ParseMarkdown.kt
     *
     * @param markdownText The input markdown text to parse.
     * @return An [AnnotatedString] with styles applied according to the markdown syntax.
     */
    fun parseMarkdown(markdownText: String): AnnotatedString {
        val lines = markdownText.split("\n")
        val resultBuilder = AnnotatedString.Builder()
        var currentStyle: SpanStyle

        lines.forEach { line ->
            when {
                // Heading 1: Extracting content, applying bold style, and appending to resultBuilder
                line.startsWith("# ") -> {
                    val content = line.removePrefix("# ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 4).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Similar processing for Heading 2 to Heading 6
                // Heading 2
                line.startsWith("## ") -> {
                    val content = line.removePrefix("## ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 3).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 3
                line.startsWith("### ") -> {
                    val content = line.removePrefix("### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 4
                line.startsWith("#### ") -> {
                    val content = line.removePrefix("#### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 5
                line.startsWith("##### ") -> {
                    val content = line.removePrefix("##### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 1).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 6
                line.startsWith("###### ") -> {
                    val content = line.removePrefix("###### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 1).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Unordered list item: Extracting content, applying bold style, appending bullet point symbol, and appending to resultBuilder
                line.startsWith("* ") || line.startsWith("- ") -> {
                    val content = line.removePrefix("* ").removePrefix("- ").trim()
                    currentStyle = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )
                    resultBuilder.append(
                        AnnotatedString("\u2022 ", currentStyle)
                    )
                    textMarkDown(content, resultBuilder, fontSize = 14.sp)
                }
                // Ordered list item: Extracting content, applying bold style, appending number and period, and appending to resultBuilder
                line.matches(Regex("^\\d+\\.\\s.*$")) -> {
                    val regex = Regex("^\\d+\\.\\s.*$")
                    val startIndex = regex.find(line)?.range?.first ?: 0
                    currentStyle = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )
                    val annotatedString = buildAnnotatedString {
                        if (startIndex > 0) {
                            append(line.substring(0, startIndex))
                        }
                        withStyle(currentStyle) {
                            append(line.substring(startIndex, startIndex + 2))
                        }
                    }
                    resultBuilder.append(annotatedString)
                    textMarkDown(
                        inputText = line.substring(startIndex + 2, line.length),
                        resultBuilder = resultBuilder,
                        fontSize = 16.sp
                    )
                }
                // Remaining Text
                else -> {
                    textMarkDown(line, resultBuilder, fontSize = DEFAULT_FONT_SIZE.sp)
                }
            } // Appending new line
            resultBuilder.append("\n")
        }
        return resultBuilder.toAnnotatedString().trim() as AnnotatedString
    }

    /**
     * Converts markdown-style text formatting to [AnnotatedString] with appropriate [SpanStyle]s.
     *
     * @param inputText The input text to be converted.
     * @param resultBuilder The [AnnotatedString.Builder] to append the converted text to.
     * @param fontSize The desired font size for the text.
     * @return The converted text with markdown formatting replaced by appropriate [SpanStyle]s.
     */
    private fun textMarkDown(
        inputText: String,
        resultBuilder: AnnotatedString.Builder,
        fontSize: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal,
    ) {
        val boldItalicPattern = Regex("\\*\\*[*_](.*?)[*_]\\*\\*")
        val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
        val italicPattern = Regex("[*_](.*?)[*_]")
        val strikethroughPattern = Regex("~~(.+?)~~")

        var currentIndex = 0

        while (currentIndex < inputText.length) {
            val nextBoldItalic = boldItalicPattern.find(inputText, startIndex = currentIndex)
            val nextBold = boldPattern.find(inputText, startIndex = currentIndex)
            val nextItalic = italicPattern.find(inputText, startIndex = currentIndex)
            val nextStrikethrough = strikethroughPattern.find(inputText, startIndex = currentIndex)

            val nextMarkDown = listOfNotNull(
                nextBoldItalic,
                nextBold,
                nextItalic,
                nextStrikethrough
            ).minByOrNull { it.range.first }

            if (nextMarkDown != null) {
                if (nextMarkDown.range.first > currentIndex) {
                    // Append any normal text before the markdown
                    val normalText = inputText.substring(currentIndex, nextMarkDown.range.first)
                    val style = SpanStyle(fontWeight = fontWeight, fontSize = fontSize)
                    resultBuilder.append(AnnotatedString(normalText, style))
                }

                val matchText = nextMarkDown.groupValues.getOrNull(1).orEmpty()

                val style = when (nextMarkDown) {
                    nextBoldItalic -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = fontSize
                    )

                    nextBold -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )

                    nextItalic -> SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontSize = fontSize
                    )

                    nextStrikethrough -> SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = fontSize
                    )

                    else -> throw IllegalStateException("Unhandled markdown type")
                }

                resultBuilder.append(AnnotatedString(matchText, style))

                currentIndex = nextMarkDown.range.last + 1
            } else {
                // Append any remaining text if no more markdown found
                val normalText = inputText.substring(currentIndex)
                val style = SpanStyle(fontWeight = fontWeight, fontSize = fontSize)
                resultBuilder.append(AnnotatedString(normalText, style))
                currentIndex = inputText.length
            }
        }
    }
}
