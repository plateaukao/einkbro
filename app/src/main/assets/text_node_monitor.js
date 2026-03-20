function myCallback(elementId, responseString) {
    //console.log("Element ID:", elementId, "Response string:", responseString);
    node = document.getElementById(elementId).nextElementSibling;
    node.textContent = responseString;
    node.classList.add("translated");
}

function getTranslatableText(element) {
    var clone = element.cloneNode(true);
    clone.querySelectorAll('img').forEach(function(img) { img.remove(); });
    return clone.textContent;
}

// Create a new IntersectionObserver object
observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    // Check if the target node is currently visible
    if (entry.isIntersecting) {
      //console.log('Node is visible:', entry.target.textContent);
      const nextNode = entry.target.nextElementSibling;
              //nextNode.textContent = result;
      var text = getTranslatableText(entry.target);
      if (nextNode && nextNode.textContent === "" && text.trim() !== "") {
          androidApp.getTranslation(text, entry.target.id, "myCallback");
      }
    } else {
      // The target node is not visible
      //console.log('Node is not visible');
    }
  });
});

// Select all elements with class name 'to-translate'
targetNodes = document.querySelectorAll('.to-translate');

// Loop through each target node and start observing it
targetNodes.forEach((targetNode) => {
  observer.observe(targetNode);
});
