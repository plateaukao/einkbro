// Shared with text_node_monitor.js (which loads after this file). Kept as a window
// property so both scripts use one canonical definition.
//
// <img> is a void element: it can hold no child nodes, so it contributes nothing to
// textContent and alt text never appears there either. Stripping images is therefore a
// no-op on the result, and the deep cloneNode(true) this used to do was pure cost — paid
// once per block candidate on every tree scan, and once per element on every rebind.
window._translateGetTextExcludingImages = window._translateGetTextExcludingImages || function(element) {
  return element.textContent;
};
function getTextExcludingImages(element) {
  return window._translateGetTextExcludingImages(element);
}

// Site-specific junk labels that must not be marked for translation.
//
// This used to read child.innerText, which is layout-dependent: reading it flushes any
// pending layout. The marking pass dirties layout constantly (every marked block inserts
// a placeholder <p>), so each read forced a synchronous relayout of the whole document —
// once per element visited. That made a single scan quadratic: 2.6s on a long article,
// all of it before the first translation request could be sent. textContent needs no
// layout, and normalizing whitespace reproduces innerText's collapsing for labels this
// short. Containers are rejected on child count first so we never materialize a large
// subtree's text just to compare it against a one-word label.
function isJunkLabel(element) {
  if (element.children.length > 2) return false;
  var text = element.textContent;
  if (text.length > 32) return false;
  text = text.replace(/\s+/g, ' ').trim();
  return text === "link" || text === "original link";
}

function isInline(node) {
  if (node.nodeType === Node.TEXT_NODE) {
    return true;
  }
  if (node.nodeType === Node.ELEMENT_NODE) {
    const inlineTags = [
      "a", "span", "b", "i", "em", "strong", "u", "small", "code", "img", "label", "sub", "sup"
    ];
    return inlineTags.includes(node.tagName.toLowerCase());
  }
  return false;
}

function shouldTranslateAsBlock(element) {
  // If element has block children or BR, it's a container, not a block itself
  // But if it's a P tag, we usually treat it as a block unless it has block children (which is invalid HTML but possible in soup)
  // For our purpose:
  // If it has <br>, we want to split by BR, so return false (recurse).
  if (element.querySelector('br')) return false;

  // If it has block level children, return false.
  const blockTags = ["div", "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "table", "blockquote"];
  for (let i = 0; i < element.children.length; i++) {
    if (blockTags.includes(element.children[i].tagName.toLowerCase())) {
      return false;
    }
  }
  return true;
}

function fetchNodesWithText(element) {
  var result = [];

  // Skip subtrees that are already marked, so re-runs of this script are incremental
  // (only mark new content that has appeared since last run, e.g. via SPA hydration).
  if (element.classList && element.classList.contains('to-translate')) return result;

  const isBlockTag = ["p", "h1", "h2", "h3", "h4", "h5", "h6", "li"].includes(element.tagName.toLowerCase());

  if (isBlockTag && shouldTranslateAsBlock(element) && getTextExcludingImages(element).trim() !== "") {
    // If block already has marked descendants, fall through to per-child handling.
    if (!(element.querySelector && element.querySelector('.to-translate'))) {
      injectTranslateTag(element);
      result.push(element);
      return result;
    }
  }

  // 2. If not a simple block, iterate children and group inlines
  var children = Array.from(element.childNodes);
  var currentGroup = [];

  function flushGroup() {
    if (currentGroup.length === 0) return;

    // Check if group has any meaningful text
    const hasText = currentGroup.some(node => node.textContent.trim().length > 0);

    // Skip groups that already contain a marked element or descendant — those were
    // marked on a previous run of this script.
    const hasMarked = currentGroup.some(n =>
      n.nodeType === Node.ELEMENT_NODE && (
        (n.classList && n.classList.contains('to-translate')) ||
        (n.querySelector && n.querySelector('.to-translate'))
      )
    );

    if (hasText && !hasMarked) {
      const firstNode = currentGroup[0];
      const parent = firstNode.parentNode;

      // Tag elements directly when wrapping would visibly disturb layout.
      // Wrapping replaces N parent children with 1 <span>, which:
      //   - reparents flex/grid items into a non-item span (breaks the layout)
      //   - shifts whitespace/decorative siblings into the span
      //   - changes :nth-child / > * matches on the parent
      // The cheap wins: (i) a lone inline element can just be tagged in place;
      // (ii) flex/grid parents can have each text-bearing inline element tagged
      // individually instead of wrapped together.
      const elementMembers = currentGroup.filter(n => n.nodeType === Node.ELEMENT_NODE);
      const significantTextMembers = currentGroup.filter(
        n => n.nodeType === Node.TEXT_NODE && n.textContent.trim() !== ""
      );

      if (elementMembers.length === 1 && significantTextMembers.length === 0) {
        injectTranslateTag(elementMembers[0]);
        result.push(elementMembers[0]);
        currentGroup = [];
        return;
      }

      var parentDisplay = "";
      try { parentDisplay = window.getComputedStyle(parent).display; } catch (e) {}
      if (/^(inline-)?(flex|grid)$/.test(parentDisplay)) {
        elementMembers.forEach(function (el) {
          if (el.textContent.trim().length > 0) {
            injectTranslateTag(el);
            result.push(el);
          }
        });
        currentGroup = [];
        return;
      }

      var span = document.createElement("span");
      parent.insertBefore(span, firstNode);
      currentGroup.forEach(node => span.appendChild(node));
      injectTranslateTag(span);
      result.push(span);
    }
    currentGroup = [];
  }

  for (var i = 0; i < children.length; i++) {
    var child = children[i];

    // Skip ignored elements
    if (child.nodeType === Node.ELEMENT_NODE) {
      if (
        child.getAttribute("data-tiara-action-name") === "헤드글씨크기_클릭" ||
        isJunkLabel(child)
      ) {
        continue;
      }
      // SELECT/TEXTAREA/DATALIST subtree text is control state, not flow content;
      // recursing in would wrap <option> text in spans and insert <p> placeholders
      // inside the control, corrupting the dropdown.
      if (child.closest('button') || child.tagName === "SCRIPT" || child.tagName === "STYLE"
        || child.tagName === "SELECT" || child.tagName === "TEXTAREA" || child.tagName === "DATALIST"
        || child.classList.contains("screen_out")
        || child.classList.contains("blind")
        || child.classList.contains("ico_view")
      ) {
        continue;
      }
      // In by-paragraph mode, myCallback writes translated text into a sibling <p> and
      // tags it with class "translated". Don't re-mark those — that would translate the
      // translation, recursively. (Body's own "translated" class is irrelevant here since
      // we're iterating children, not the body element itself.)
      if (child.classList.contains("translated")) {
        continue;
      }
    }

    if (isInline(child)) {
      currentGroup.push(child);
    } else {
      // Block element or BR or other
      flushGroup();

      if (child.nodeType === Node.ELEMENT_NODE) {
        if (child.tagName === "BR") {
          // Ignore BR, it acts as separator
        } else {
          // Recurse into block element
          result.push(fetchNodesWithText(child));
        }
      }
    }
  }
  // Flush any remaining group
  flushGroup();

  return result;
}

function generateUUID() {
  var timestamp = new Date().getTime();
  const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const random = (timestamp + Math.random() * 16) % 16 | 0;
    timestamp = Math.floor(timestamp / 16);
    return (c === 'x' ? random : (random & 0x3 | 0x8)).toString(16);
  });

  return uuid;
}

function injectTranslateTag(node) {
  // for monitoring visibility
  node.className += " to-translate";
  // for locating element's position
  node.id = generateUUID().toString();
  // In-place mode rewrites text nodes within `node` itself,
  // so the sibling placeholder is unused and only adds margin-based layout gaps.
  if (window._translateInPlace) return;
  // for later inserting translated text
  var pElement = document.createElement("p");
  try {
    node.parentNode.insertBefore(pElement, node.nextSibling);
  } catch (error) {
  }
}

// Idempotent translation setup. Sites like news.daum.net trigger multiple onPageFinished
// callbacks per load (hash navigation + JS-driven reload), and lazy-rendered content keeps
// arriving for seconds after the WebView reports progress=100. This script must therefore
// be safe to call repeatedly:
//   * fetchNodesWithText skips already-marked subtrees, so re-runs only mark NEW content;
//   * no toggle/innerHTML-swap behaviour — those wiped pending async translations;
//   * a MutationObserver keeps marking content as the page renders it after first fire.
(function () {
  var hasTranslatedClass = document.body.classList.contains("translated");
  var stillMarked = document.querySelectorAll('.to-translate').length > 0;

  // Stale state: body still says "translated" but the page replaced its own innerHTML
  // (e.g. SPA hydration). Reset and re-translate from scratch.
  if (hasTranslatedClass && !stillMarked) {
    document.body.classList.remove("translated_but_hide");
    document.body.classList.remove("translated");
    delete document.originalInnerHTML;
    delete document.translatedInnerHTML;
    hasTranslatedClass = false;
  }

  if (!hasTranslatedClass) {
    document.body.classList.add("translated");
    document.originalInnerHTML = document.body.innerHTML;
  }
  fetchNodesWithText(document.body);

  // Observe future DOM mutations and mark new text-bearing content as it appears.
  //
  // This observer lives for the page's whole lifetime, so on a site that never stops
  // mutating (ad rotation, infinite scroll, live tickers) its cost is paid forever.
  // Three things keep it bounded:
  //
  //   * Our own writes are ignored. Marking a node inserts a placeholder <p>, and every
  //     arriving translation rewrites text inside a marker — both are childList mutations
  //     under document.body. Without this filter each translation scheduled another scan,
  //     so a page-full of them kept the loop fed indefinitely.
  //   * Scans are scoped to the subtrees that actually changed rather than restarting from
  //     document.body, so the cost tracks the size of the mutation, not the size of the page.
  //   * The delay backs off once scans stop finding new content, so a page whose DOM churns
  //     without adding text isn't rescanned three times a second forever.
  if (window._translateMutationObserver) {
    window._translateMutationObserver.disconnect();
  }

  var MIN_SCAN_DELAY_MS = 300;
  var MAX_SCAN_DELAY_MS = 5000;
  // Past this many distinct mutation targets, dedup costs more than it saves and a single
  // pass from body is the cheaper scan.
  var MAX_TRACKED_ROOTS = 32;

  var scanDelay = MIN_SCAN_DELAY_MS;
  var pendingRoots = null; // Set of elements awaiting a scan; null when none is scheduled.

  // True when a record describes a change we made ourselves. Everything we write lands
  // under a marker we already own: myCallback's in-place text rewrite happens inside the
  // .to-translate element, and its by-paragraph counterpart fills the sibling placeholder
  // that it then tags .translated. Site content appearing inside an existing marker is
  // already covered by that marker, so skipping these loses nothing.
  //
  // document.body carries its own "translated" class meaning "this page is set up for
  // translation", which is unrelated to the per-placeholder one. closest() would match it
  // for every node on the page and silence the observer entirely, so a match on body means
  // there was no real marker ancestor.
  function isOwnMutation(record) {
    var target = record.target;
    if (!target) return false;
    var el = target.nodeType === Node.ELEMENT_NODE ? target : target.parentElement;
    if (!el || !el.closest) return false;
    var owner = el.closest('.to-translate, .translated');
    return !!owner && owner !== document.body;
  }

  // Collapse to the shallowest still-attached roots: scanning a parent already covers its
  // descendants, and fetchNodesWithText skips marked subtrees on the way down.
  function shallowestRoots(roots) {
    var list = [];
    roots.forEach(function (el) { if (el.isConnected) list.push(el); });
    if (list.length > MAX_TRACKED_ROOTS) return [document.body];
    return list.filter(function (el) {
      return !list.some(function (other) { return other !== el && other.contains(el); });
    });
  }

  function runScan() {
    var roots = shallowestRoots(pendingRoots);
    pendingRoots = null;

    var markedBefore = document.querySelectorAll('.to-translate').length;
    roots.forEach(function (root) { fetchNodesWithText(root); });
    var markedAfter = document.querySelectorAll('.to-translate').length;

    // Drop the records our own marking just queued so they don't schedule another scan.
    // isOwnMutation can't catch these: injectTranslateTag's placeholder is inserted next
    // to a brand-new marker, so the mutation's target is the unmarked parent.
    window._translateMutationObserver.takeRecords();

    if (markedAfter > markedBefore) {
      scanDelay = MIN_SCAN_DELAY_MS;
      // Re-run text_node_monitor's IntersectionObserver bind for the new markers.
      if (typeof window._translateRebindObserver === "function") {
        window._translateRebindObserver();
      }
    } else {
      scanDelay = Math.min(scanDelay * 2, MAX_SCAN_DELAY_MS);
    }
  }

  window._translateMutationObserver = new MutationObserver(function (records) {
    var alreadyScheduled = pendingRoots !== null;
    for (var i = 0; i < records.length; i++) {
      if (isOwnMutation(records[i])) continue;
      var target = records[i].target;
      var el = target.nodeType === Node.ELEMENT_NODE ? target : target.parentElement;
      if (!el) continue;
      if (pendingRoots === null) pendingRoots = new Set();
      pendingRoots.add(el);
    }
    if (pendingRoots === null || alreadyScheduled) return;
    setTimeout(runScan, scanDelay);
  });
  window._translateMutationObserver.observe(document.body, { childList: true, subtree: true });
})();
