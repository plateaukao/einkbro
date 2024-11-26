package io.github.edsuns.adfilter

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
interface CustomFilter : Iterator<String> {
    /**
     * Get specified line of rule.
     */
    fun get(line: Int): String

    /**
     * @return true if custom contains the [rule]
     */
    fun contains(rule: String): Boolean

    /**
     * Add a new rule.
     */
    fun append(rule: String)

    /**
     * @return true if [rule] is a comment (starts with `! `)
     */
    fun isComment(rule: String): Boolean

    /**
     * Make the existing rule as a comment.
     */
    fun comment(rule: String)

    /**
     * Make the existing comment as a rule.
     */
    fun uncomment(rule: String)

    /**
     * Remove specified rule from custom filter.
     */
    fun remove(rule: String)

    /**
     * @return the number of rules in custom filter
     */
    fun size(): Int

    /**
     * Save the changes.
     */
    fun flush()
}