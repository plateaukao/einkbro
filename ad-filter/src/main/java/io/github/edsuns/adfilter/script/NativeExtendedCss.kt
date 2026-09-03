package io.github.edsuns.adfilter.script

/**
 * Turns "extended CSS" cosmetic selectors into native CSS rules instead of
 * feeding them to AdGuard's 48 KB ExtendedCss JS library. Chromium supports
 * :has() natively since 105, which covers about half of AdGuard Base's
 * extended rules — applied by the engine itself, continuously and at native
 * speed, where the library needed a MutationObserver re-matching selectors in
 * JS on every DOM change (measurably painful on low-end e-ink CPUs).
 *
 * Selectors using pseudo-classes only the library could evaluate (text
 * matching, ancestor walking, computed-style matching) are dropped: the ads
 * inside such containers are still network-blocked, so at worst an empty
 * frame stays visible.
 */
internal object NativeExtendedCss {

    // Pseudo-classes (and legacy attribute forms) with no native equivalent.
    private val NON_NATIVE_MARKERS = listOf(
        ":contains(",
        ":has-text(",
        ":matches-css",
        ":matches-attr(",
        ":matches-property(",
        ":xpath(",
        ":nth-ancestor(",
        ":upward(",
        ":remove(",
        ":if(",
        ":if-not(",
        ":min-text-length(",
        ":watch-attr",
        "-abp-",
        "[-ext-",
    )

    fun isNativeExpressible(selector: String): Boolean =
        '{' !in selector && '}' !in selector &&
            NON_NATIVE_MARKERS.none { selector.contains(it, ignoreCase = true) }

    /**
     * One standalone rule per selector — deliberately not grouped: a selector
     * the WebView doesn't support (e.g. :has() before Chromium 105) then
     * invalidates only its own rule instead of a whole grouped one.
     */
    fun buildRules(selectors: List<String>, hidingDeclaration: String): String {
        val sb = StringBuilder()
        for (selector in selectors) {
            if (!isNativeExpressible(selector)) continue
            sb.append(selector).append(hidingDeclaration).append('\n')
        }
        return sb.toString()
    }
}
