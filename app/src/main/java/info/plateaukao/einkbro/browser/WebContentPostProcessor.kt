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
            ninjaWebView.evaluateJavascript(SCROLL_FIX_JS, null);
        }
        // text selection handling
        ninjaWebView.addSelectionChangeListener()
    }

    companion object {
        // referenced from https://greasyfork.org/zh-CN/scripts/494503-high-contrast-theme-for-e-ink-displays-and-page-down
        private const val SCROLL_FIX_JS = """
            javascript:(function() {
    const body = document.body;
    const scrollRatio = 0.9;
 
    const Container = document.createElement("div");
    Container.style.cssText = `
        width: 1px; height: 1px;
        bottom:0;right:0;
        position:fixed;
        display:flex;
        flex-direction:column;
        border-style:dashed;
        z-index:2147483647;
        border-width:2px;
    `;
 
    // 创建一个数组来存储具有overflow-y: auto的元素
    let elementsWithOverflowYAuto = [window];
    let el = window;
    // 设置一个标志变量来记录是否滚动
    let scrolled = false;
    let index = -1;
 
    init();
 
    function init() {
        // 创建一个数组来存储具有overflow-y: auto的元素
        elementsWithOverflowYAuto = [window];
        el = window;
        // 设置一个标志变量来记录是否滚动
        scrolled = false;
        index = -1;
 
 
        // 确认滚动元素
        setTimeout(() => {
            check();
        }, 80);
 
    }
 
    function check() {
        if (scrolled) {
            return;
        }
        index++;
        if (index == 1) {
            tryFindOverflowY();
        }
        if (index >= elementsWithOverflowYAuto.length) {
            el = window;
            return;
        }
        el = elementsWithOverflowYAuto[index];
        // 监听滚动事件
        el.addEventListener('scroll', function () {
            scrolled = true;
        });
 
        // 执行滚动操作
        el.scrollBy(0, 1);
        setTimeout(() => {
            check();
        }, 30);
 
    }
 
 
    function tryFindOverflowY() {
        // 获取所有元素.
        var allElements = document.querySelectorAll('*');
        let arr = [];
        // 遍历所有元素，检查它们的样式
        allElements.forEach(function (element) {
            // 获取元素的计算样式
            var style = getComputedStyle(element);
 
            // 检查overflow-y属性是否为auto
            if (style.overflowY === 'auto') {
                // 如果是，添加到结果数组中
                arr.push(element);
            }
        });
        arr.sort((a, b) => {
            return (b.clientHeight + b.clientWidth) - (a.clientHeight + a.clientWidth);
        })
        elementsWithOverflowYAuto = elementsWithOverflowYAuto.concat(arr);
    }
 
    const UpButton = document.createElement("div");
    UpButton.id = "EinkBroUpButton";
    UpButton.style.cssText = "flex:1";
    UpButton.addEventListener('click', () => {
        scroll(-scrollRatio * window.innerHeight);
    });
 
    const DownButton = document.createElement("div");
    DownButton.id = "EinkBroDownButton";
    DownButton.style.cssText = "flex:1;border-top-style:dashed;border-width:2px;";
    DownButton.addEventListener('click', () => {
        scroll(scrollRatio * window.innerHeight);
    });
 
    function scroll(y) {
        if (document.contains(el)) {
            el.scrollBy(0, y);
            return;
        }
        init();
        setTimeout(() => {
            if (document.contains(el)) {
                el.scrollBy(0, y);
            }
        }, 150);
    }
 
    Container.appendChild(UpButton);
    Container.appendChild(DownButton);
    body.appendChild(Container);
            })()
        """

        private const val twitterJs = """
            var adsHidden = 0;
var adSelector = "div[data-testid=placementTracking]";
var trendSelector = "div[data-testid=trend]";
var userSelector = "div[data-testid=UserCell]";
var articleSelector = "article[data-testid=tweet]";

var sponsoredSvgPath = 'M20.75 2H3.25C2.007 2 1 3.007 1 4.25v15.5C1 20.993 2.007 22 3.25 22h17.5c1.243 0 2.25-1.007 2.25-2.25V4.25C23 3.007 21.993 2 20.75 2zM17.5 13.504c0 .483-.392.875-.875.875s-.875-.393-.875-.876V9.967l-7.547 7.546c-.17.17-.395.256-.62.256s-.447-.086-.618-.257c-.342-.342-.342-.896 0-1.237l7.547-7.547h-3.54c-.482 0-.874-.393-.874-.876s.392-.875.875-.875h5.65c.483 0 .875.39.875.874v5.65z';
var sponsoredBySvgPath = 'M19.498 3h-15c-1.381 0-2.5 1.12-2.5 2.5v13c0 1.38 1.119 2.5 2.5 2.5h15c1.381 0 2.5-1.12 2.5-2.5v-13c0-1.38-1.119-2.5-2.5-2.5zm-3.502 12h-2v-3.59l-5.293 5.3-1.414-1.42L12.581 10H8.996V8h7v7z';
var youMightLikeSvgPath = 'M12 1.75c-5.11 0-9.25 4.14-9.25 9.25 0 4.77 3.61 8.7 8.25 9.2v2.96l1.15-.17c1.88-.29 4.11-1.56 5.87-3.5 1.79-1.96 3.17-4.69 3.23-7.97.09-5.54-4.14-9.77-9.25-9.77zM13 14H9v-2h4v2zm2-4H9V8h6v2z';
var adsSvgPath = 'M19.498 3h-15c-1.381 0-2.5 1.12-2.5 2.5v13c0 1.38 1.119 2.5 2.5 2.5h15c1.381 0 2.5-1.12 2.5-2.5v-13c0-1.38-1.119-2.5-2.5-2.5zm-3.502 12h-2v-3.59l-5.293 5.3-1.414-1.42L12.581 10H8.996V8h7v7z';
var peopleFollowSvgPath = 'M17.863 13.44c1.477 1.58 2.366 3.8 2.632 6.46l.11 1.1H3.395l.11-1.1c.266-2.66 1.155-4.88 2.632-6.46C7.627 11.85 9.648 11 12 11s4.373.85 5.863 2.44zM12 2C9.791 2 8 3.79 8 6s1.791 4 4 4 4-1.79 4-4-1.791-4-4-4z';
var xAd = '>Ad<'; // TODO: add more languages; appears to only be used for English accounts as of 2023-08-03
var removePeopleToFollow = false; // set to 'true' if you want these suggestions removed, however note this also deletes some tweet replies
const promotedTweetTextSet = new Set(['Promoted Tweet', 'プロモツイート']);

function getAds() {
  return Array.from(document.querySelectorAll('div')).filter(function(el) {
    var filteredAd;

    if (el.innerHTML.includes(sponsoredSvgPath)) {
      filteredAd = el;
    } else if (el.innerHTML.includes(sponsoredBySvgPath)) {
      filteredAd = el;
    } else if (el.innerHTML.includes(youMightLikeSvgPath)) {
      filteredAd = el;
    } else if (el.innerHTML.includes(adsSvgPath)) {
      filteredAd = el;
    } else if (removePeopleToFollow && el.innerHTML.includes(peopleFollowSvgPath)) {
      filteredAd = el;
    } else if (el.innerHTML.includes(xAd)) {
      filteredAd = el;
    } else if (promotedTweetTextSet.has(el.innerText)) { // TODO: bring back multi-lingual support from git history
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
  } else if (ad.closest(articleSelector) !== null) {
    ad.closest(articleSelector).remove();
    adsHidden += 1;
  } else if (promotedTweetTextSet.has(ad.innerText)) {
    ad.remove();
    adsHidden += 1;
  }

  console.log('Twitter ads hidden: ', adsHidden.toString());
}

function getAndHideAds() {
  getAds().forEach(hideAd)
}

// hide ads on page load
document.addEventListener('load', () => getAndHideAds());

// oftentimes, tweets render after onload. LCP should catch them.
new PerformanceObserver((entryList) => {
  getAndHideAds();
}).observe({type: 'largest-contentful-paint', buffered: true});

// re-check as user scrolls
document.addEventListener('scroll', () => getAndHideAds());

// re-check as user scrolls tweet sidebar (exists when image is opened)
var sidebarExists = setInterval(function() {
  let timelines = document.querySelectorAll("[aria-label='Timeline: Conversation']");

  if (timelines.length == 2) {
    let tweetSidebar = document.querySelectorAll("[aria-label='Timeline: Conversation']")[0].parentElement.parentElement;
    tweetSidebar.addEventListener('scroll', () => getAndHideAds());
  }
}, 500);
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
            //"facebook.com" to facebookHideSponsoredPostsJs,
            "zhihu.com" to zhihuDisablePopupJs,
            "jianshu.com" to jianshuJs,
            "huxiu.com" to huxiuJs,
            //"twitter.com" to twitterJs,
            "reddit.com" to redditJs,
        )
    }
}
