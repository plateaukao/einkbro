package info.plateaukao.einkbro.view

import android.graphics.Point
import android.util.Base64
import android.webkit.ValueCallback
import android.webkit.WebView
import info.plateaukao.einkbro.preference.HighlightStyle
import info.plateaukao.einkbro.preference.TranslationMode
import info.plateaukao.einkbro.preference.TranslationTextStyle
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.unit.HelperUnit.loadAssetFile
import java.io.IOException
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Handles all JavaScript injection, CSS injection, and JS-based interactions for EBWebView.
 * Separates JS bridge concerns from core WebView configuration.
 */
class WebViewJsBridge(private val webView: WebView) {

    //region CSS Injection

    /**
     * Sets the CSS content of a managed, slot-identified <style> element.
     * Calling again with the same slot replaces the previous CSS instead of
     * appending another style tag; an empty [css] removes the slot, which
     * reverts to the page's own styling without a reload.
     */
    fun updateCssSlot(slot: String, css: String) {
        try {
            val encoded = Base64.encodeToString(css.toByteArray(), Base64.NO_WRAP)
            val js = loadAssetFile("update_css_slot.js")
                .replace("__SLOT_ID__", slot)
                .replace("__CSS_B64__", encoded)
            webView.evaluateJavascript(js, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCssSlot(slot: String) = updateCssSlot(slot, "")

    //endregion

    //region JS File Evaluation

    fun evaluateJsFile(
        fileName: String,
        withPrefix: Boolean = true,
        callback: ValueCallback<String>? = null,
    ) {
        val jsContent = loadAssetFile(fileName)
        if (withPrefix) {
            webView.evaluateJavascript(jsContent.wrapJsFunction(), callback)
        } else {
            webView.evaluateJavascript(jsContent, callback)
        }
    }

    //endregion

    //region Text Selection

    suspend fun getSelectedText(): String = suspendCoroutine { continuation ->
        val js = "window.getSelection().toString();"
        webView.evaluateJavascript(js) { value ->
            continuation.resume(value.substring(1, value.length - 1))
        }
    }

    fun selectSentence(point: Point) =
        evaluateJsFile("select_sentence.js") {
            webView.postDelayed({ simulateClickAction?.invoke(point) }, 100)
        }

    fun selectParagraph(point: Point) =
        evaluateJsFile("select_paragraph.js") {
            webView.postDelayed({ simulateClickAction?.invoke(point) }, 100)
        }

    var simulateClickAction: ((Point) -> Unit)? = null

    suspend fun getSelectedTextWithContext(contextLength: Int = 10): String =
        suspendCoroutine { continuation ->
            evaluateJsFile("get_selected_text_with_context.js") { value ->
                continuation.resume(value.substring(1, value.length - 1))
            }
        }

    fun addSelectionChangeListener() = evaluateJsFile("text_selection_change.js")

    fun removeTextSelection() {
        webView.evaluateJavascript(
            "javascript:window.getSelection().removeAllRanges()",
            null
        )
    }

    //endregion

    //region Link Extraction

    suspend fun getPageLinks(): String = suspendCoroutine { continuation ->
        evaluateJsFile("extract_links.js", withPrefix = false) { value ->
            continuation.resume(value ?: "[]")
        }
    }

    //endregion

    fun downloadBlobUrl(
        blobUrl: String,
        mimeType: String,
        downloadId: String,
    ) {
        val script = loadAssetFile("blob_url_fetch.js")
            .replace("__BLOB_URL__", JSONObject.quote(blobUrl))
            .replace("__MIME_TYPE__", JSONObject.quote(mimeType))
            .replace("__DOWNLOAD_ID__", JSONObject.quote(downloadId))
        webView.evaluateJavascript(script, null)
    }

    //region Highlights

    fun highlightTextSelection(highlightStyle: HighlightStyle) {
        // Idempotent slot update: safe to call on every highlight, and unlike a
        // one-shot flag it survives navigations to a new document.
        updateCssSlot(CSS_SLOT_HIGHLIGHT, loadAssetFile("highlight.css"))

        val className = when (highlightStyle) {
            HighlightStyle.UNDERLINE -> "highlight_underline"
            HighlightStyle.BACKGROUND_YELLOW -> "highlight_yellow"
            HighlightStyle.BACKGROUND_GREEN -> "highlight_green"
            HighlightStyle.BACKGROUND_BLUE -> "highlight_blue"
            HighlightStyle.BACKGROUND_PINK -> "highlight_pink"
            else -> ""
        }

        webView.evaluateJavascript(
            String.format(loadAssetFile("text_selection_highlight.js"), className).wrapJsFunction(),
            null
        )
    }

    //endregion

    //region Translation JS

    fun clearTranslationElements() {
        webView.evaluateJavascript(CLEAR_TRANSLATION_ELEMENTS_JS, null)
    }

    fun translateByParagraphInPlaceReplace() {
        webView.evaluateJavascript("window._translateInPlace = true;", null)
        evaluateJsFile("translate_by_paragraph.js") {
            evaluateJsFile("text_node_monitor.js", false)
        }
    }

    fun translateByParagraphInPlace(translationTextStyle: TranslationTextStyle) {
        val textBlockStyle = when (translationTextStyle) {
            TranslationTextStyle.NONE -> TRANSLATED_P_CSS_NONE
            TranslationTextStyle.DASHED_BORDER -> TRANSLATED_P_CSS_DASHED_BORDER
            TranslationTextStyle.VERTICAL_LINE -> TRANSLATED_P_CSS_VERTICAL_LINE
            TranslationTextStyle.GRAY -> TRANSLATED_P_CSS_GRAY
            TranslationTextStyle.BOLD -> TRANSLATED_P_CSS_BOLD
        }
        updateCssSlot(CSS_SLOT_TRANSLATION, textBlockStyle)
        evaluateJsFile("translate_by_paragraph.js") {
            evaluateJsFile("text_node_monitor.js", false)
        }
    }

    fun addGoogleTranslation(preferredLanguages: String) {
        val str = injectGoogleTranslateV2Js(preferredLanguages)
        webView.evaluateJavascript(str, null)
        evaluateJsFile("hide_google_translate_popup.js")
    }

    private fun injectGoogleTranslateV2Js(preferredLanguages: String): String =
        String.format(
            INJECT_GOOGLE_TRANSLATE_V2_JS_FORMAT,
            if (preferredLanguages.isNotEmpty()) "includedLanguages: '$preferredLanguages',"
            else ""
        )

    fun hideTranslateContext(translationMode: TranslationMode) {
        when (translationMode) {
            TranslationMode.GOOGLE_URL ->
                webView.evaluateJavascript(HIDE_GURL_TRANSLATE_CONTEXT, null)
            else -> Unit
        }
    }

    //endregion

    //region Reader Mode JS

    fun evaluateMozReaderModeJs(
        isVertical: Boolean = false,
        postAction: (() -> Unit)? = null,
    ) {
        updateCssSlot(
            CSS_SLOT_READER,
            loadAssetFile(if (isVertical) "verticalReaderview.css" else "readerview.css")
        )
        updateCssSlot(CSS_SLOT_VERTICAL, if (isVertical) loadAssetFile("vertical_layout.css") else "")

        val jsString = HelperUnit.getStringFromAsset("MozReadability.js") +
                "\n" + HelperUnit.getStringFromAsset("jsonld_article.js")
        webView.evaluateJavascript(jsString) {
            postAction?.invoke()
        }
    }

    /**
     * Injects the Readability library + its stylesheet into the live page so a
     * follow-up [getReaderModeBodyHtml] can extract the reader HTML without
     * entering reader mode. Only ever used for horizontal raw-HTML extraction
     * (EPUB export / save), so there is no vertical variant.
     */
    fun injectMozReaderModeJs() {
        try {
            val buffer = (loadAssetFile("MozReadability.js") +
                    "\n" + loadAssetFile("jsonld_article.js")).toByteArray()
            val cssBuffer = loadAssetFile("readerview.css").toByteArray()

            val encodedJs = Base64.encodeToString(buffer, Base64.NO_WRAP)
            val encodedCss = Base64.encodeToString(cssBuffer, Base64.NO_WRAP)
            webView.evaluateJavascript(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'text/javascript';" +
                        "script.innerHTML = window.atob('" + encodedJs + "');" +
                        "parent.appendChild(script);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encodedCss + "');" +
                        "parent.appendChild(style);" +
                        "window.scrollTo(0, 0);" +
                        "})()", null
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Rewrites the viewport meta tag. Reader mode's two-column layout pins the
     * scale ("initial-scale=1.0, minimum-scale=1.0") because its horizontal
     * column overflow otherwise makes WebView zoom out to fit the whole strip
     * after an orientation change.
     */
    fun setViewportContent(content: String) {
        val js = loadAssetFile("set_viewport_content.js")
            .replace("__VIEWPORT_CONTENT__", content)
        webView.evaluateJavascript(js, null)
    }

    fun disableReaderMode() {
        // Removing the reader style slots restores the page's own layout and
        // writing mode; no counteracting CSS is needed.
        clearCssSlot(CSS_SLOT_READER)
        clearCssSlot(CSS_SLOT_VERTICAL)
        clearCssSlot(CSS_SLOT_READER_SETTINGS)
        // Undo the two-column scale pin (no-op when it was never applied:
        // entering reader mode already set the meta to width=device-width).
        setViewportContent(VIEWPORT_DEFAULT)
        webView.evaluateJavascript(
            "javascript:(function() {" +
                    "if (typeof enableSiteStyleSheets === 'function') enableSiteStyleSheets();" +
                    "document.body.innerHTML = document.innerHTMLCache;" +
                    "document.body.classList.remove(\"mozac-readerview-body\");" +
                    "window.scrollTo(0, 0);" +
                    "})()", null
        )
    }

    fun replaceWithReaderModeBody(keepExtraContent: Boolean, url: String?, callback: ValueCallback<String>?) {
        webView.evaluateJavascript(
            "(function() { ${String.format(replaceWithReaderModeBodyJs(keepExtraContent), url)} })();",
            callback
        )
    }

    fun getReaderModeBodyHtml(keepExtraContent: Boolean, url: String?, callback: ValueCallback<String>?) {
        webView.evaluateJavascript(
            String.format(getReaderModeBodyHtmlJs(keepExtraContent), url),
            callback
        )
    }

    fun getReaderModeBodyText(keepExtraContent: Boolean, callback: ValueCallback<String>?) {
        webView.evaluateJavascript(getReaderModeBodyTextJs(keepExtraContent), callback)
    }

    //endregion

    //region Audio Mode

    fun enableAudioOnlyMode() = evaluateJsFile("audio_only_mode.js")

    fun disableAudioOnlyMode() = evaluateJsFile("audio_only_mode_off.js")

    //endregion

    companion object {
        fun String.wrapJsFunction(): String = "javascript:(function() { $this })()"

        //region CSS Slot Ids

        // The main blob recomputed by WebViewReaderHelper.updateCssStyle()
        const val CSS_SLOT_MAIN = "main"
        const val CSS_SLOT_READER = "reader"
        const val CSS_SLOT_READER_SETTINGS = "readerSettings"
        const val CSS_SLOT_VERTICAL = "vertical"

        const val VIEWPORT_DEFAULT = "width=device-width"
        const val VIEWPORT_FIXED_SCALE = "width=device-width, initial-scale=1.0, minimum-scale=1.0"
        const val CSS_SLOT_HIGHLIGHT = "highlight"
        const val CSS_SLOT_TRANSLATION = "translation"

        //endregion

        //region Reader Mode JS Templates

        private fun readabilityOptions(keepExtraContent: Boolean): String {
            return if (keepExtraContent) {
                "{classesToPreserve: preservedClasses, overwriteImgSrc: true, keepExtraContent: true}"
            } else {
                "{classesToPreserve: preservedClasses, overwriteImgSrc: true}"
            }
        }

        private fun replaceWithReaderModeBodyJs(keepExtraContent: Boolean) = """
            ${if (keepExtraContent) "inlineCodeStyles();" else ""}
            var scopedDoc = (typeof getReadabilityScopedDocument === 'function') ? getReadabilityScopedDocument() : null;
            var documentClone = scopedDoc || document.cloneNode(true);
            var article = new Readability(documentClone, ${readabilityOptions(keepExtraContent)}).parse();
            document.innerHTMLCache = document.body.innerHTML;

            if (article) {
                article.readingTime = getReadingTime(article.length, document.documentElement.lang.substring(0, 2));

                document.body.outerHTML = createHtmlBody(article)
                disableSiteStyleSheets();

                var viewport = document.getElementsByName('viewport')[0];
                if (viewport) viewport.setAttribute('content', 'width=device-width');
            }
        """

        private fun getReaderModeBodyHtmlJs(keepExtraContent: Boolean) = """
            javascript:(function() {
                ${if (keepExtraContent) "inlineCodeStyles();" else ""}
                var scopedDoc = (typeof getReadabilityScopedDocument === 'function') ? getReadabilityScopedDocument() : null;
                var documentClone = scopedDoc || document.cloneNode(true);
                var article = new Readability(documentClone, ${readabilityOptions(keepExtraContent)}).parse();
                if (!article) return '';
                article.readingTime = getReadingTime(article.length, document.documentElement.lang.substring(0, 2));
                var bodyOuterHTML = createHtmlBodyWithUrl(article, "%s")
                var headOuterHTML = document.head.outerHTML;
                return ('<html>'+ headOuterHTML + bodyOuterHTML +'</html>');
            })()
        """

        private fun getReaderModeBodyTextJs(keepExtraContent: Boolean) = """
            javascript:(function() {
                var scopedDoc = (typeof getReadabilityScopedDocument === 'function') ? getReadabilityScopedDocument() : null;
                var documentClone = scopedDoc || document.cloneNode(true);
                var article = new Readability(documentClone, ${readabilityOptions(keepExtraContent)}).parse();
                if (!article) return '';
                return article.title + ', ' + article.textContent;
            })()
        """

        //endregion

        //region CSS Constants

        const val NOTO_SANS_SERIF_FONT_CSS =
            "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+TC:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@400&display=swap');" +
                    "@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Noto Serif TC', 'Noto Serif JP', 'Noto Serif KR', 'Noto Serif SC', serif !important;\n" +
                    "}\n"

        const val IANSUI_FONT_CSS =
            "@import url('https://fonts.googleapis.com/css2?family=BIZ+UDPMincho&family=Iansui&display=swap');" +
                    "* {\n" +
                    "font-family: 'Iansui',serif !important;\n" +
                    "}\n"

        const val JA_MINCHO_FONT_CSS =
            "@import url('https://fonts.googleapis.com/css2?family=Shippori+Mincho:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Shippori Mincho',serif !important;\n" +
                    "}\n"

        const val KO_GAMJA_FONT_CSS =
            "@import url('https://fonts.googleapis.com/css2?family=Gamja+Flower:wght@400&display=swap');" +
                    "* {\n" +
                    "font-family: 'Gamja Flower',serif !important;\n" +
                    "}\n"

        const val SERIF_FONT_CSS =
            "* {\n" +
                    "font-family: serif !important;\n" +
                    "}\n"

        const val CUSTOM_FONT_CSS = """
            @font-face {
                 font-family: fontfamily;
                 font-weight: 400;
                 font-display: swap;
                 src: url('mycustomfont');
            }
            html body * {
              font-family: fontfamily, serif, popular-symbols, lite-glyphs-outlined, lite-glyphs-filled, snaptu-symbols !important;
            }
        """

        const val WHITE_BACKGROUND_CSS = """
* {
    color: #000000!important;
    border-color: #555555 !important;
    background-color: #FFFFFF !important;
}
input,select,option,button,textarea {
	border: #FFFFFF !important;
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input[type=button],input[type=submit],input[type=reset],input[type=image] {
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
input[type=button]: focus,input[type=submit]: focus,input[type=reset]: focus,input[type=image]: focus, input[type=button]: hover,input[type=submit]: hover,input[type=reset]: hover,input[type=image]: hover {
	background: #FFFFFF !important;
	border-color: #FFFFFF #FFFFFF #FFFFFF #FFFFFF !important;
}
        """

        const val MAKE_TEXT_BLACK_CSS = """
            * {
                color: #000000 !important;
            }
            a, a * {
                color: #000000 !important;
            }
            input,select,option,button,textarea {
                color: #000000 !important;
            }
            input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {
                color: #000000 !important;
            }
            input[type=button],input[type=submit],input[type=reset],input[type=image] {
                color: #000000 !important;
            }
        """

        const val BOLD_FONT_CSS = "* {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a,a * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a: visited,a: visited *,a: active,a: active * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "a: hover,a: hover * {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input,select,option,button,textarea {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input: focus,select: focus,option: focus,button: focus,textarea: focus,input: hover,select: hover,option: hover,button: hover,textarea: hover {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n" +
                "input[type=button]: focus,input[type=submit]: focus,input[type=reset]: focus,input[type=image]: focus, input[type=button]: hover,input[type=submit]: hover,input[type=reset]: hover,input[type=image]: hover {\n" +
                "\tfont-weight:value !important;\n" +
                "}\n"

        //endregion

        //region Translation CSS

        private const val TRANSLATED_P_CSS_NONE = """
            .to-translate + p:not(.translated) {
                display: none;
            }
            .translated {
                padding: 5px; display: inline-block; line-height: 1.5; max-width: 100vw;
            }
        """

        private const val TRANSLATED_P_CSS_GRAY = """
            .to-translate + p:not(.translated) {
                display: none;
            }
            .translated {
                color: gray; padding: 5px; display: inline-block; max-width: 100vw; line-height: 1.5;
            }
        """

        private const val TRANSLATED_P_CSS_BOLD = """
            .to-translate + p:not(.translated) {
                display: none;
            }
            .translated {
                font-weight: bold; padding: 5px; display: inline-block; max-width: 100vw; line-height: 1.5;
            }
        """

        private const val TRANSLATED_P_CSS_DASHED_BORDER = """
            .to-translate + p:not(.translated) {
                display: none;
            }
            .translated {
                border: 1px dashed lightgray; padding: 5px; display: inline-block; position: relative; max-width: 100vw; line-height: 1.5;
            }
        """

        private const val TRANSLATED_P_CSS_VERTICAL_LINE = """
            .to-translate + p:not(.translated) {
                display: none;
            }
            .translated {
                padding: 2px; margin-left: 7px; display: inline-block; position: relative; max-width: 100vw; line-height: 1.5;
            }
            .translated::before {
                content: ''; display: inline-block; width: 2px; height: 90%; background-color: black; position: absolute; left: -7px;
            }
        """

        //endregion

        //region Translation JS

        private const val CLEAR_TRANSLATION_ELEMENTS_JS = """
            javascript:(function() {
                document.body.innerHTML = document.originalInnerHTML;
                document.body.classList.remove("translated");
                window._translateInPlace = false;
            })()
        """

        private const val SECOND_PART =
            """setTimeout(
                    function() {
                          var css=document.createElement('style');
                          css.type='text/css';
                          css.charset='UTF-8';
                          css.appendChild(document.createTextNode('.goog-te-combo, .goog-te-banner *, .goog-te-ftab *, .goog-te-menu *, .goog-te-menu2 *, .goog-te-balloon * {font-size: 8pt !important;}'));
                          var teef=document.getElementById(':0.container');
                          if(teef){
                              teef.contentDocument.head.appendChild(css);
                          }
                    },
                    1000);"""

        private const val INJECT_GOOGLE_TRANSLATE_V2_JS_FORMAT =
            "!function(){!function(){function e(){" +
                    "window.setTimeout(" +
                    "function(){window[t].showBanner(!0)},10)}" +
                    "function n(){" +
                    "return new google.translate.TranslateElement({" +
                    "autoDisplay:!1,floatPosition:0,%s pageLanguage:'auto'" +
                    "})}" +
                    "var t=(document.documentElement.lang,'TE_7777'),o='TECB_7777';" +
                    "if(window[t])e();" +
                    "else if(!window.google||!google.translate||!google.translate.TranslateElement){window[o]||(window[o]=function(){window[t]=n(),e()});" +
                    "var a=document.createElement('script');a.src='https://translate.google.com/translate_a/element.js?cb='+encodeURIComponent(o)+'&client=tee',document.getElementsByTagName('head')[0].appendChild(a);$SECOND_PART}}()}();"

        private const val HIDE_GURL_TRANSLATE_CONTEXT = """
            javascript:(function() {
                document.querySelector('#gt-nvframe').style = "height:0px";
            })()
            """

        //endregion
    }
}
