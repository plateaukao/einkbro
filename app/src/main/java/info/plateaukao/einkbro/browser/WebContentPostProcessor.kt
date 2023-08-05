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
    }

    companion object {
        private const val twitterJs = """
            javascript:(function() {
            var adsHidden = 0;
var adSelector = "div[data-testid=placementTracking]";
var trendSelector = "div[data-testid=trend]";
var userSelector = "div[data-testid=UserCell]";
var sponsoredSvgPath = 'M20.75 2H3.25C2.007 2 1 3.007 1 4.25v15.5C1 20.993 2.007 22 3.25 22h17.5c1.243 0 2.25-1.007 2.25-2.25V4.25C23 3.007 21.993 2 20.75 2zM17.5 13.504c0 .483-.392.875-.875.875s-.875-.393-.875-.876V9.967l-7.547 7.546c-.17.17-.395.256-.62.256s-.447-.086-.618-.257c-.342-.342-.342-.896 0-1.237l7.547-7.547h-3.54c-.482 0-.874-.393-.874-.876s.392-.875.875-.875h5.65c.483 0 .875.39.875.874v5.65z';

function getAds() {
  return Array.from(document.querySelectorAll('div')).filter(function(el) {
    var filteredAd;

    if (el.getInnerHTML().includes(sponsoredSvgPath)) {
      filteredAd = el;
    }

    return filteredAd;
  })
}

function hideAd(ad) {
  if (ad.closest(adSelector) !== null) { // Promoted tweets
    ad.closest(adSelector).remove();
    adsHidden += 1;
  } else if (ad.closest(trendSelector) !== null) {
    ad.closest(trendSelector).remove();
    adsHidden += 1;
  } else if (ad.closest(userSelector) !== null) {
    ad.closest(userSelector).remove();
    adsHidden += 1;
  }
  console.log('Twitter ads hidden: ', adsHidden.toString());
}

// hide ads on page load
document.addEventListener('load', () => getAds().forEach(hideAd));

// oftentimes, tweets render after onload. LCP should catch them.
new PerformanceObserver((entryList) => {
  getAds().forEach(hideAd);
}).observe({type: 'largest-contentful-paint', buffered: true});

// re-check as user scrolls
document.addEventListener('scroll', () => getAds().forEach(hideAd));
            })()
        """
        private const val zoomAndDesktopTemplateJs =
            "javascript:document.getElementsByName('viewport')[0].setAttribute('content', '%s%s');"

        private const val enableZoomJs = "initial-scale=1,maximum-scale=10.0,"
        private const val facebookHideSponsoredPostsJs = """
            javascript:(function() {
              function removeItems() {
                  var reels = Array.from(document.querySelectorAll('div.m[data-actual-height="490"]')).filter(e => e.innerText.search("Reels") != -1);
                  reels.forEach(el => {el.style.display="none";});
                  
                  var hrLines = document.querySelectorAll('div.m[data-actual-height="1"]');
                  hrLines.forEach(el => {
                    var n = el.nextSibling; 
                    if (n.innerText.search("Suggested for you") != -1) {
                        n.style.display="none";
                        n.nextSibling.style.display="none";
                    }
                    if (n.innerText.search("Sponsored󰞋󰙷") != -1) {
                        n.style.display="none";
                        n.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none";
                    }
                    });
                    
                  var hrLines = document.querySelectorAll('div.m[data-actual-height="2"]');
                  hrLines.forEach(el => {
                    var n = el.nextSibling; 
                    if (n.innerText.search("Suggested for you") != -1) {
                        n.style.display="none";
                        n.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.style.display="none";
                    }
                    if (n.innerText.search("Sponsored") != -1) {
                        n.style.display="none";
                        n.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.nextSibling.style.display="none";
                        n.nextSibling.nextSibling.nextSibling.nextSibling.style.display="none";
                    }
                    });
              }
              setInterval(removeItems, 3000);
                
            var qcleanObserver = new window.MutationObserver(function(mutation, observer){ 
              setInterval(removeItems, 3000);
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
            "facebook.com" to facebookHideSponsoredPostsJs,
            "zhihu.com" to zhihuDisablePopupJs,
            "jianshu.com" to jianshuJs,
            "huxiu.com" to huxiuJs,
            "twitter.com" to twitterJs,
            "reddit.com" to redditJs,
        )
    }
}
