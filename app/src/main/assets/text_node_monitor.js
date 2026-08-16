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

    // Empty response = the native side failed to translate. Clear the in-flight flag and
    // queue the element so a later IntersectionObserver event or rebind scan retries it.
    // The rebind scan only looks at new markers, so without the queue a failure on an
    // already-bound element would never be picked up again.
    if (!responseString) {
        if (el) {
            window._translateRequested.delete(el);
            window._translateRetryQueue.add(el);
        }
        return;
    }

    // Cache so future re-renders of the same text apply instantly without a round-trip
    // (and so the element-replacement scenario above has data to work with).
    if (key) window._translateTextCache.set(key, responseString);
    _applyTranslationToElement(el, responseString);
}

// Shared with translate_by_paragraph.js (which loads first and defines the implementation,
// including why stripping images is unnecessary). Defined defensively here too in case this
// file is loaded standalone.
window._translateGetTextExcludingImages = window._translateGetTextExcludingImages || function(element) {
    return element.textContent;
};
function getTranslatableText(element) {
    return window._translateGetTextExcludingImages(element);
}

// An element still "intersects" the viewport when it sits behind an overlay or
// backdrop (IntersectionObserver knows nothing about stacking), so probe the
// topmost element at its visible center to tell whether the reader can see it.
function isTranslateTargetOccluded(el) {
  var r = el.getBoundingClientRect();
  if (r.width === 0 || r.height === 0) return false;
  var left = Math.max(r.left, 0), right = Math.min(r.right, window.innerWidth);
  var top = Math.max(r.top, 0), bottom = Math.min(r.bottom, window.innerHeight);
  if (left >= right || top >= bottom) return false; // outside the viewport: nothing to probe
  var hit = document.elementFromPoint((left + right) / 2, (top + bottom) / 2);
  if (!hit) return false;
  return !(el.contains(hit) || hit.contains(el));
}

// Stable partition: on-top targets keep document order and go first, occluded ones
// follow. Occluded content is still translated — this only decides who gets the
// native side's limited translation slots first, so e.g. an article overlay isn't
// stuck behind dozens of requests for the page hidden underneath it. Scores are
// precomputed because elementFromPoint forces layout and a sort comparator would
// call it O(n log n) times.
function sortOnTopFirst(targets) {
  var occluded = new Map();
  targets.forEach(function (el) { occluded.set(el, isTranslateTargetOccluded(el) ? 1 : 0); });
  targets.sort(function (a, b) { return occluded.get(a) - occluded.get(b); });
}

// Reuse the observer across re-injections. Recreating it (with disconnect) would orphan
// every node already in _translateObservedNodes: they'd be detached from the old observer
// but skipped by the rebind loop, so off-screen content would never translate on scroll.
// The callback resolves maybeRequestTranslation/getTranslatableText as globals at call
// time, so re-injected definitions apply to the reused observer too.
window._translateObserver = window._translateObserver || new IntersectionObserver((entries) => {
  var targets = [];
  entries.forEach((entry) => {
    if (entry.isIntersecting) targets.push(entry.target);
  });
  sortOnTopFirst(targets);
  // Single request path shared with the rebind scan: it checks the already-translated
  // marker for the current mode, the text cache, AND _translateRequested — otherwise
  // this callback re-requests elements whose bind-time request is still in flight.
  targets.forEach(maybeRequestTranslation);
}, { rootMargin: "400px" });

// Track which nodes are already observed so the rebind hook doesn't double-observe.
window._translateObservedNodes = window._translateObservedNodes || new WeakSet();
// Track which nodes already had their initial visibility-check translation kicked off.
window._translateRequested = window._translateRequested || new WeakSet();
// Elements whose translation came back empty and that deserve another attempt. Held
// explicitly so the rebind scan can retry exactly those instead of re-probing every marker
// on the page looking for work — see bindObserverToTargets.
window._translateRetryQueue = window._translateRetryQueue || new Set();

// Whether this marker is already carrying its translation. In-place mode stamps the
// element itself; by-paragraph mode fills the sibling placeholder, which starts empty.
function isTranslationApplied(targetNode) {
  if (window._translateInPlace) return targetNode.hasAttribute('data-original-html');
  var placeholder = targetNode.nextElementSibling;
  return !placeholder || placeholder.textContent !== "";
}

function maybeRequestTranslation(targetNode) {
  if (isTranslationApplied(targetNode)) return;
  if (window._translateRequested.has(targetNode)) return;

  // Viewport gate first, before any work that writes to the DOM. Applying a cached
  // translation dirties layout, so a write here would force the next element's
  // getBoundingClientRect to re-run layout — the same read/write interleaving that made
  // the marking pass quadratic. Gating first bounds the writes to the handful of markers
  // actually near the viewport; everything else is picked up when it scrolls into view.
  var r = targetNode.getBoundingClientRect();
  if (r.width === 0 || r.height === 0) return;
  // Match the IntersectionObserver's rootMargin so we don't translate way-off-screen content.
  if (r.top > window.innerHeight + 400 || r.bottom < -400) return;

  var text = getTranslatableText(targetNode);
  if (text.trim() === "") return;
  // If this exact text was translated before in this session, apply instantly.
  var cached = window._translateTextCache.get(_translateNormalize(text));
  if (cached) {
    _applyTranslationToElement(targetNode, cached);
    return;
  }
  window._translateRequested.add(targetNode);
  androidApp.getTranslation(text, targetNode.id, "myCallback");
}

// Only newly-marked elements need work here. Re-probing every marker on the page was the
// expensive part: isTranslateTargetOccluded calls elementFromPoint, which forces a layout
// and a hit-test, so one rebind on a long article cost a forced layout per paragraph — and
// the MutationObserver could trigger that several times a second. Elements already bound
// are the IntersectionObserver's responsibility from then on; the only ones still needing a
// nudge are those whose request came back empty, and those are tracked explicitly.
function bindObserverToTargets() {
  var fresh = [];
  var all = document.querySelectorAll('.to-translate');
  for (var i = 0; i < all.length; i++) {
    if (!window._translateObservedNodes.has(all[i])) fresh.push(all[i]);
  }

  var retries = [];
  window._translateRetryQueue.forEach(function (el) {
    // Skip nodes that are only in the queue because they're about to be bound below.
    if (el.isConnected && window._translateObservedNodes.has(el)) retries.push(el);
  });
  window._translateRetryQueue.clear();

  sortOnTopFirst(fresh);
  fresh.forEach(function(targetNode) {
    window._translateObserver.observe(targetNode);
    window._translateObservedNodes.add(targetNode);
    // IntersectionObserver isn't reliable for elements that were already on-screen at the
    // moment we observed them (e.g. content marked after lazy hydration completed). Do an
    // initial visibility scan so currently-visible markers get translated immediately.
    maybeRequestTranslation(targetNode);
  });
  retries.forEach(maybeRequestTranslation);
}

// Exposed so translate_by_paragraph.js's MutationObserver can re-bind for newly-added
// `.to-translate` elements that appear after lazy/SPA hydration.
window._translateRebindObserver = bindObserverToTargets;
bindObserverToTargets();
