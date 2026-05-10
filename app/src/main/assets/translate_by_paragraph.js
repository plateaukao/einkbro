// Shared with text_node_monitor.js (which loads after this file). Kept as a window
// property so both scripts use one canonical definition.
window._translateGetTextExcludingImages = window._translateGetTextExcludingImages || function(element) {
  var clone = element.cloneNode(true);
  clone.querySelectorAll('img').forEach(function(img) { img.remove(); });
  return clone.textContent;
};
function getTextExcludingImages(element) {
  return window._translateGetTextExcludingImages(element);
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
        child.innerText === "original link"
      ) {
        continue;
      }
      if (child.closest('button') || child.tagName === "SCRIPT" || child.tagName === "STYLE"
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
  // Coalesce rapid bursts so we don't churn the tree on every keystroke-like mutation.
  if (window._translateMutationObserver) {
    window._translateMutationObserver.disconnect();
  }
  var pendingScan = false;
  window._translateMutationObserver = new MutationObserver(function () {
    if (pendingScan) return;
    pendingScan = true;
    setTimeout(function () {
      pendingScan = false;
      fetchNodesWithText(document.body);
      // Re-run text_node_monitor's IntersectionObserver bind for any new markers.
      if (typeof window._translateRebindObserver === "function") {
        window._translateRebindObserver();
      }
    }, 300);
  });
  window._translateMutationObserver.observe(document.body, { childList: true, subtree: true });
})();
