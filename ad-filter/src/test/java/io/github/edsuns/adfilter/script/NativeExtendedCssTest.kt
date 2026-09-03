package io.github.edsuns.adfilter.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeExtendedCssTest {

    @Test
    fun hasSelectorsAreNative() {
        assertTrue(NativeExtendedCss.isNativeExpressible("div:has(> a[href^=\"https://ads\"])"))
        assertTrue(NativeExtendedCss.isNativeExpressible(".list > [class]:has(> span):not(.keep)"))
        assertTrue(NativeExtendedCss.isNativeExpressible("#adbd.overdiv"))
    }

    @Test
    fun libraryOnlyPseudosAreDropped() {
        assertFalse(NativeExtendedCss.isNativeExpressible("div:contains(sponsored)"))
        assertFalse(NativeExtendedCss.isNativeExpressible("span:has-text(Ad)"))
        assertFalse(NativeExtendedCss.isNativeExpressible("div:matches-css(display: block)"))
        assertFalse(NativeExtendedCss.isNativeExpressible("a:upward(2)"))
        assertFalse(NativeExtendedCss.isNativeExpressible("p:xpath(//div)"))
        assertFalse(NativeExtendedCss.isNativeExpressible("i:matches-attr(\"data-ad\")"))
        assertFalse(NativeExtendedCss.isNativeExpressible("b:matches-property(x.y)"))
        // a :has wrapping a library-only pseudo is still library-only
        assertFalse(NativeExtendedCss.isNativeExpressible("div:has(> span:contains(Ad))"))
    }

    @Test
    fun braceSmugglingIsRejected() {
        assertFalse(NativeExtendedCss.isNativeExpressible("div} body {background: red"))
        assertFalse(NativeExtendedCss.isNativeExpressible("div{color:red"))
    }

    @Test
    fun buildRules_emitsOneStandaloneRulePerSelector() {
        val hiding = "{display: none !important;}"
        val rules = NativeExtendedCss.buildRules(
            listOf(
                "div:has(> .ad)",
                "span:contains(sponsored)", // dropped
                ".banner:has(a)",
            ),
            hiding,
        )
        assertEquals(
            "div:has(> .ad){display: none !important;}\n" +
                ".banner:has(a){display: none !important;}\n",
            rules,
        )
    }

    @Test
    fun buildRules_emptyWhenNothingExpressible() {
        assertEquals("", NativeExtendedCss.buildRules(listOf("a:upward(1)"), "{x}"))
        assertEquals("", NativeExtendedCss.buildRules(emptyList(), "{x}"))
    }
}
