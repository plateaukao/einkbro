package info.plateaukao.einkbro.browser

import android.app.Application
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.NinjaWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebContentPostProcessor : KoinComponent {
    private val configManager: ConfigManager by inject()
    private val application: Application by inject()

    fun postProcess(ninjaWebView: NinjaWebView, url: String) {
        if (url.startsWith("data:text/html")) return

        for (entry in urlScriptMap) {
            val entryUrl = entry.key
            val script = entry.value
            if (url.contains(entryUrl)) {
                ninjaWebView.evaluateJavascript(script, null)
            }
        }

        if (!ninjaWebView.shouldUseReaderFont() && (configManager.desktop || configManager.enableZoom)) {
            val context = application.applicationContext
            val width = if (ViewUnit.getWindowWidth(context) < 800) "800" else "device-width"
            ninjaWebView.evaluateJavascript(
                zoomAndDesktopTemplateJs.format(
                    if (configManager.enableZoom) enableZoomJs else "",
                    if (configManager.desktop) "width=$width" else ""
                ),
                null
            )
        }

        if (configManager.enableVideoAutoFullscreen) {
            ninjaWebView.evaluateJavascript(videoAutoFullscreen, null)
        }

        if (ninjaWebView.shouldUseReaderFont()) {
            ninjaWebView.settings.textZoom = configManager.readerFontSize
        } else {
            ninjaWebView.settings.textZoom = configManager.fontSize
        }

        // some strange website scrolling support
        if (configManager.shouldFixScroll(url)) {
            val offset = configManager.pageReservedOffsetInString
            val offsetPercent =
                if (offset.endsWith('%')) offset.take(offset.length - 1).toInt() else 0
            val offsetPixel = if (offset.endsWith('%')) 0 else offset

            val js = SCROLL_FIX_JS.format(offsetPercent / 100.0, offsetPixel)
            ninjaWebView.evaluateJavascript(js, null);
        }

        if (configManager.shouldTranslateSite(url)) {
            ninjaWebView.showTranslation()
        }

        // text selection handling
        ninjaWebView.addSelectionChangeListener()
    }

    companion object {
        private const val SCROLL_FIX_JS = """
           javascript:(function() {
    function findScrollableParent(element) {
        if (!element) {
            return document.scrollingElement || document.documentElement;
        }
        
        if (element.scrollHeight > element.clientHeight) {
            const overflowY = window.getComputedStyle(element).overflowY;
            if (overflowY !== 'visible' && overflowY !== 'hidden') {
                return element;
            }
        }
        
        return findScrollableParent(element.parentElement);
    }

    function scrollPage(direction) {
        const scrollable = findScrollableParent(document.activeElement);
        const scrollAmount = direction * scrollable.clientHeight * (1 - %s) - %s;
        scrollable.scrollBy({
            top: scrollAmount,
            left: 0,
            behavior: 'auto'
        });
    }

    window.addEventListener('keydown', function(e) {
        if (e.key === 'PageDown' || e.keyCode === 34) {
            e.preventDefault();
            scrollPage(1);
        } else if (e.key === 'PageUp' || e.keyCode === 33) {
            e.preventDefault();
            scrollPage(-1);
        }
    }, true);
})() 
        """
        private const val zoomAndDesktopTemplateJs =
            "javascript:document.getElementsByName('viewport')[0].setAttribute('content', '%s%s');"

        private const val enableZoomJs = "initial-scale=1,maximum-scale=10.0,"
        private const val zhihuDisablePopupJs = """
            javascript:(function() {
                const style = document.createElement("style");
                style.innerHTML = "html{overflow: auto !important}.Modal-wrapper{display:none !important}.OpenInAppButton {display:none !important}";
                document.head.appendChild(style);
                
                var mutationObserver = new window.MutationObserver(function(mutation, observer){ 
                    if (document.querySelector('.signFlowModal')) {
                        let button = document.querySelector('.Button.Modal-closeButton.Button--plain');
                        if (button) button.click();
                    }
                })
                mutationObserver.observe(document, { subtree: true, childList: true });
                
                document.querySelector(".ContentItem-expandButton").click();
                document.querySelector(".ModalWrap-item:last-child .ModalWrap-itemBtn").click();
            })()
        """

        private const val jianshuJs = """
            document.querySelector("button.call-app-btn").remove();
            document.querySelector(".collapse-tips .close-collapse-btn").click();
            document.querySelector(".guidance-wrap-item:last-child .wrap-item-btn").click();
            document.querySelector(".open-app-modal .cancel").click();
            document.querySelector(".btn-content button.cancel").click();
        """

        private const val huxiuJs = """
            document.querySelector(".bottom-open-app-btn").remove();
            document.querySelector(".qr_code_pc").remove();
            document.querySelector(".guide-wrap").remove();
            document.querySelector("#related-article-wrap").remove();
            document.querySelector(".article-introduce-info").remove();
            document.querySelector(".article-recommend-wrap").remove();
            document.querySelector(".placeholder-line").remove();
            document.querySelector(".hot-so-wrap").remove();
        """

        private const val redditJs = """
            javascript:(function() {
                localStorage.setItem('bannerLastClosed', new Date());
                localStorage.setItem('xpromo-consolidation', new Date());
            
                var posts = [].filter.call(document.getElementsByTagName('article'), el => (
                   (el.getElementsByTagName('a')[0].getAttribute('rel') != null && el.getElementsByTagName('a')[0].getAttribute('rel').indexOf('sponsored') >= 0)));
                   
                while(posts.length > 0) { posts.pop().style.display = "none"; }
                  
                var qcleanObserver = new window.MutationObserver(function(mutation, observer){ 
                var posts = [].filter.call(document.getElementsByTagName('article'), el => (
                   (el.getElementsByTagName('a')[0].getAttribute('rel') != null && el.getElementsByTagName('a')[0].getAttribute('rel').indexOf('sponsored') >= 0)));
                   
                while(posts.length > 0) { posts.pop().style.display = "none"; }
                });
                
                qcleanObserver.observe(document, { subtree: true, childList: true });
            })()
        """

        private const val videoAutoFullscreen = """
            javascript:(function() {
            var element = document.querySelector("video");
            element.addEventListener("playing", function() {
                if (element.requestFullscreen) {
                    element.requestFullscreen();
                } else if (element.webkitRequestFullscreen) {
                    element.webkitRequestFullscreen(Element.ALLOW_KEYBOARD_INPUT);
                }
            }, false);
            })()
        """

        val urlScriptMap = mapOf(
            "zhihu.com" to zhihuDisablePopupJs,
            "jianshu.com" to jianshuJs,
            "huxiu.com" to huxiuJs,
            "reddit.com" to redditJs,
        )
    }
}
