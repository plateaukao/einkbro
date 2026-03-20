function myCallback(elementId, responseString) {
    var el = document.getElementById(elementId);
    if (!el) return;
    var node = el.nextElementSibling;
    if (!node) return;
    node.textContent = responseString;
    node.classList.add("translated");
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
      var nextNode = entry.target.nextElementSibling;
      var text = getTranslatableText(entry.target);
      if (nextNode && nextNode.textContent === "" && text.trim() !== "") {
          androidApp.getTranslation(text, entry.target.id, "myCallback");
      }
    }
  });
}, { rootMargin: "150px" });

var targetNodes = document.querySelectorAll('.to-translate');
targetNodes.forEach(function(targetNode) {
    window._translateObserver.observe(targetNode);
});
