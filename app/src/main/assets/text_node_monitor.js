function myCallback(elementId, responseString) {
    var el = document.getElementById(elementId);
    if (!el) return;

    if (window._translateInPlace) {
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
        if (textNodes.length === 0) return;
        if (textNodes.length === 1) {
            textNodes[0].textContent = responseString;
        } else {
            var lengths = textNodes.map(function(n) { return n.textContent.length; });
            var total = lengths.reduce(function(a, b) { return a + b; }, 0);
            if (total === 0) { textNodes[0].textContent = responseString; return; }
            var pos = 0, cumLen = 0;
            for (var i = 0; i < textNodes.length; i++) {
                cumLen += lengths[i];
                var end = (i === textNodes.length - 1) ? responseString.length : Math.round(cumLen / total * responseString.length);
                textNodes[i].textContent = responseString.substring(pos, end);
                pos = end;
            }
        }
    } else {
        var node = el.nextElementSibling;
        if (!node) return;
        node.textContent = responseString;
        node.classList.add("translated");
    }
}

function getTranslatableText(element) {
    var clone = element.cloneNode(true);
    clone.querySelectorAll('img').forEach(function(img) { img.remove(); });
    return clone.textContent;
}

// Disconnect previous observer if re-injected
if (window._translateObserver) {
    window._translateObserver.disconnect();
}

window._translateObserver = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      var text = getTranslatableText(entry.target);
      if (text.trim() === "") return;
      if (window._translateInPlace) {
          if (!entry.target.hasAttribute('data-original-html')) {
              androidApp.getTranslation(text, entry.target.id, "myCallback");
          }
      } else {
          var nextNode = entry.target.nextElementSibling;
          if (nextNode && nextNode.textContent === "") {
              androidApp.getTranslation(text, entry.target.id, "myCallback");
          }
      }
    }
  });
}, { rootMargin: "400px" });

var targetNodes = document.querySelectorAll('.to-translate');
targetNodes.forEach(function(targetNode) {
    window._translateObserver.observe(targetNode);
});
