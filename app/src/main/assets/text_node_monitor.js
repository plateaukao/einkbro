// JS-side text→translation cache. Lets us apply translations to future re-renders of the
// same content without re-querying the native side, and lets the callback recover when an
// SPA replaces the element between request and response (the elementId is gone but the
// new element has the same text — match by normalized text instead).
window._translateTextCache = window._translateTextCache || new Map();

function _translateNormalize(s) {
    return (s || '').replace(/\s+/g, ' ').trim();
}

function _applyTranslationToElement(el, responseString) {
    if (!el) return false;
    if (window._translateInPlace) {
        // Already translated — a duplicate/late response. Re-applying would churn the
        // text nodes (visible refresh on e-ink) and overwrite the original-HTML backup
        // below with already-translated content.
        if (el.hasAttribute('data-original-html')) return true;
        el.setAttribute('data-original-html', el.innerHTML);
        // Replace only text nodes to preserve links, styles, and other elements.
        // Skip whitespace-only text nodes — they're source-formatting whitespace sitting
        // between block/flex children; filling them with characters turns them into
        // visible anonymous flex items and breaks the parent's layout.
        var textNodes = [];
        var walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
        while (walker.nextNode()) {
            if (walker.currentNode.textContent.trim() !== "") textNodes.push(walker.currentNode);
        }
        if (textNodes.length === 0) return false;
        if (textNodes.length === 1) {
            textNodes[0].textContent = responseString;
        } else {
            var lengths = textNodes.map(function(n) { return n.textContent.length; });
            var total = lengths.reduce(function(a, b) { return a + b; }, 0);
            if (total === 0) { textNodes[0].textContent = responseString; return true; }
            var pos = 0, cumLen = 0;
            for (var i = 0; i < textNodes.length; i++) {
                cumLen += lengths[i];
                var end = (i === textNodes.length - 1) ? responseString.length : Math.round(cumLen / total * responseString.length);
                textNodes[i].textContent = responseString.substring(pos, end);
                pos = end;
            }
        }
        return true;
    } else {
        var node = el.nextElementSibling;
        if (!node) return false;
        node.textContent = responseString;
        node.classList.add("translated");
        return true;
    }
}

function myCallback(elementId, originalText, responseString) {
    var key = _translateNormalize(originalText);
    var el = document.getElementById(elementId);
    if (!el) {
        // SPAs (e.g. news.daum.net) often re-render between request and response, killing the
        // original element. Fall back to any unfilled marker whose normalized text matches.
        var candidates = document.querySelectorAll('.to-translate:not([data-original-html])');
        for (var i = 0; i < candidates.length; i++) {
            if (_translateNormalize(getTranslatableText(candidates[i])) === key) {
                el = candidates[i];
                break;
            }
        }
    }

    // Empty response = the native side failed to translate. Clear the in-flight flag so a
    // later IntersectionObserver event or rebind scan can retry this element.
    if (!responseString) {
        if (el) window._translateRequested.delete(el);
        return;
    }

    // Cache so future re-renders of the same text apply instantly without a round-trip
    // (and so the element-replacement scenario above has data to work with).
    if (key) window._translateTextCache.set(key, responseString);
    _applyTranslationToElement(el, responseString);
}

// Shared with translate_by_paragraph.js (which loads first and defines the implementation).
// Defined defensively here too in case this file is loaded standalone.
window._translateGetTextExcludingImages = window._translateGetTextExcludingImages || function(element) {
    var clone = element.cloneNode(true);
    clone.querySelectorAll('img').forEach(function(img) { img.remove(); });
    return clone.textContent;
};
function getTranslatableText(element) {
    return window._translateGetTextExcludingImages(element);
}

// Reuse the observer across re-injections. Recreating it (with disconnect) would orphan
// every node already in _translateObservedNodes: they'd be detached from the old observer
// but skipped by the rebind loop, so off-screen content would never translate on scroll.
// The callback resolves maybeRequestTranslation/getTranslatableText as globals at call
// time, so re-injected definitions apply to the reused observer too.
window._translateObserver = window._translateObserver || new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (!entry.isIntersecting) return;
    if (window._translateInPlace) {
      // Single request path shared with the rebind scan: checks data-original-html,
      // the text cache, AND _translateRequested — otherwise this callback re-requests
      // elements whose bind-time request is still awaiting its response.
      maybeRequestTranslation(entry.target);
      return;
    }
    var text = getTranslatableText(entry.target);
    if (text.trim() === "") return;
    var nextNode = entry.target.nextElementSibling;
    // The empty-sibling check only holds until the response arrives, so guard in-flight
    // requests with _translateRequested here too.
    if (nextNode && nextNode.textContent === "" && !window._translateRequested.has(entry.target)) {
      window._translateRequested.add(entry.target);
      androidApp.getTranslation(text, entry.target.id, "myCallback");
    }
  });
}, { rootMargin: "400px" });

// Track which nodes are already observed so the rebind hook doesn't double-observe.
window._translateObservedNodes = window._translateObservedNodes || new WeakSet();
// Track which nodes already had their initial visibility-check translation kicked off.
window._translateRequested = window._translateRequested || new WeakSet();

function maybeRequestTranslation(targetNode) {
  if (!window._translateInPlace) return;
  if (targetNode.hasAttribute('data-original-html')) return;
  var text = getTranslatableText(targetNode);
  if (text.trim() === "") return;
  // If this exact text was translated before in this session, apply instantly.
  var cached = window._translateTextCache.get(_translateNormalize(text));
  if (cached) {
    _applyTranslationToElement(targetNode, cached);
    return;
  }
  if (window._translateRequested.has(targetNode)) return;
  var r = targetNode.getBoundingClientRect();
  if (r.width === 0 || r.height === 0) return;
  // Match the IntersectionObserver's rootMargin so we don't translate way-off-screen content.
  if (r.top > window.innerHeight + 400 || r.bottom < -400) return;
  window._translateRequested.add(targetNode);
  androidApp.getTranslation(text, targetNode.id, "myCallback");
}

function bindObserverToTargets() {
  document.querySelectorAll('.to-translate').forEach(function(targetNode) {
    if (!window._translateObservedNodes.has(targetNode)) {
      window._translateObserver.observe(targetNode);
      window._translateObservedNodes.add(targetNode);
    }
    // IntersectionObserver isn't reliable for elements that were already on-screen at the
    // moment we observed them (e.g. content marked after lazy hydration completed). Do an
    // initial visibility scan so currently-visible markers get translated immediately.
    maybeRequestTranslation(targetNode);
  });
}

// Exposed so translate_by_paragraph.js's MutationObserver can re-bind for newly-added
// `.to-translate` elements that appear after lazy/SPA hydration.
window._translateRebindObserver = bindObserverToTargets;
bindObserverToTargets();
