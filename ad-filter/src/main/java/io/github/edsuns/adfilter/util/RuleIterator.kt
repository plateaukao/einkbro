package io.github.edsuns.adfilter.util

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
open class RuleIterator internal constructor(data: String? = null) : Iterator<String> {

    internal val dataBuilder: StringBuilder =
        if (data == null) StringBuilder() else StringBuilder(data)

    private var curLine: Int = -1
    private var lineStart: Int = 0
    private var lineEnd: Int = 0

    private fun reset() {
        curLine = -1
        lineStart = 0
        lineEnd = 0
    }

    override fun hasNext(): Boolean = lineEnd + 1 < dataBuilder.length

    override fun next(): String = get(curLine + 1)

    fun size(): Int {
        var count = 0
        var start = 0
        var end = dataBuilder.indexOf(LINE_END, start)
        while (end > 0 && (end - start) > 0) {
            start = end + 1
            end = dataBuilder.indexOf(LINE_END, start)
            count++
        }
        return count
    }

    fun get(line: Int): String {
        if (curLine == line) {
            return dataBuilder.substring(lineStart, lineEnd)
        }
        if (curLine > line) {
            reset()
        }
        while (curLine + 1 < line) {
            lineStart = dataBuilder.indexOf(LINE_END, lineStart)
            if (lineStart == -1 || lineStart + 1 >= dataBuilder.length) {
                return ""
            }
            lineStart++
            curLine++
        }
        lineEnd = dataBuilder.indexOf(LINE_END, lineStart)
        if (lineEnd == -1) {
            return ""
        }
        return dataBuilder.substring(lineStart, lineEnd)
    }

    fun contains(rule: String): Boolean {
        return indexOf(rule) > -1
    }

    private fun indexOf(rule: String): Int {
        var start = 0
        var end = dataBuilder.indexOf(LINE_END)
        while (end > 0) {
            if (dataBuilder.substring(start, end) == rule) {
                return start
            }
            start = end + 1
            end = dataBuilder.indexOf(LINE_END, start)
        }
        return -1
    }

    fun append(rule: String) {
        if (rule.isBlank()) {
            return
        }
        if (!contains(rule)) {
            dataBuilder.append(rule).append(LINE_END)
        }
    }

    fun isComment(rule: String): Boolean {
        return rule.startsWith("! ")
    }

    fun comment(rule: String) {
        if (!isComment(rule)) {
            val index = indexOf(rule)
            if (index > -1) {
                dataBuilder.delete(index, index + rule.length)
                dataBuilder.insert(index, rule.toComment())
            }
        }
    }

    fun uncomment(rule: String) {
        val comment: String
        val uncomment: String
        if (isComment(rule)) {
            comment = rule
            uncomment = rule.substring(2).trim()
        } else {
            comment = rule.toComment()
            uncomment = rule
        }
        val index = indexOf(comment)
        if (index > -1) {
            dataBuilder.delete(index, index + comment.length)
            dataBuilder.insert(index, uncomment)
        }
    }

    fun remove(rule: String) {
        if (rule.isBlank()) {
            return
        }
        val index = indexOf(rule)
        if (index > -1) {
            dataBuilder.delete(index, index + rule.length + 1)
        }
    }

    private fun String.toComment() = "! $this"

    companion object {
        private const val LINE_END = "\n"
    }
}