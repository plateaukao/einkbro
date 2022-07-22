package de.baumann.browser.browser

import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.view.NinjaWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebContentPostProcessor:KoinComponent {
    private val configManager: ConfigManager by inject()

    fun postProcess(ninjaWebView: NinjaWebView, url: String) {
        if (url.startsWith("data:text/html")) return

        for (entry in urlScriptMap) {
            val entryUrl = entry.key
            val script = entry.value
            if (url.contains(entryUrl)) {
                ninjaWebView.evaluateJavascript(script, null)
            }
        }

        if (configManager.enableZoom) {
            ninjaWebView.evaluateJavascript(enableZoomJs, null)
        }
    }

    companion object {
        private const val enableZoomJs = "javascript:document.getElementsByName('viewport')[0].setAttribute('content', 'initial-scale=1.0,maximum-scale=10.0');"
        private const val facebookHideSponsoredPostsJs = """
            javascript:(function() {
              var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0 || el.getElementsByTagName('header')[0].innerText == 'Suggested for you'); 
              while(posts.length > 0) { posts.pop().style.display = "none"; }
              
              var ads = Array.from(document.getElementsByClassName("bg-s3")).filter(e => e.innerText.indexOf("Sponsored") != -1);
              ads.forEach(el => {el.style.display="none"; el.nextSibling.style.display="none";el.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none"});
              
            var qcleanObserver = new window.MutationObserver(function(mutation, observer){ 
              var posts = [].filter.call(document.getElementsByTagName('article'), el => el.attributes['data-store'].value.indexOf('is_sponsored.1') >= 0 || el.getElementsByTagName('header')[0].innerText == 'Suggested for you'); 
              while(posts.length > 0) { posts.pop().style.display = "none"; }
              
              var ads = Array.from(document.getElementsByClassName("bg-s3")).filter(e => e.innerText.indexOf("Sponsored") != -1);
              ads.forEach(el => {el.style.display="none"; el.nextSibling.style.display="none";el.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.style.display="none"});
              ads.forEach(el => {el.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none"});
            });
            
            qcleanObserver.observe(document, { subtree: true, childList: true });
            })()
        """

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

        val urlScriptMap = mapOf(
                "facebook.com" to facebookHideSponsoredPostsJs,
                "zhihu.com" to zhihuDisablePopupJs,
                "jianshu.com" to jianshuJs,
        )
    }
}