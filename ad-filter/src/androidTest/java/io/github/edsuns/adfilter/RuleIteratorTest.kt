package io.github.edsuns.adfilter

import io.github.edsuns.adfilter.util.RuleIterator
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by Edsuns@qq.com on 2021/6/12.
 */
class RuleIteratorTest {
    private val exampleRule = "example/rule"

    @Test
    fun contains() {
        val rules = RuleIterator()

        rules.append(exampleRule)
        assertTrue(rules.contains(exampleRule))
    }

    @Test
    fun appendAndRemove() {
        val rules = RuleIterator()

        assertEquals(0, rules.size())
        rules.append(exampleRule)
        assertEquals(1, rules.size())

        rules.remove(exampleRule)
        assertEquals(0, rules.size())
    }

    @Test
    fun commentAndUncomment() {
        val rules = RuleIterator()

        assertEquals(0, rules.size())
        rules.append(exampleRule)
        assertEquals(1, rules.size())

        rules.comment(exampleRule)
        assertEquals(1, rules.size())
        assertTrue(rules.isComment(rules.get(0)))

        rules.uncomment(exampleRule)
        assertEquals(1, rules.size())
        assertFalse(rules.isComment(rules.get(0)))
    }
}