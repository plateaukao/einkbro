function fetchNodesWithText(element) {
  var result = [];
  for (var i = 0; i < element.children.length; i++) {
    var child = element.children[i];
    // bypass non-necessary element
    if (
      child.getAttribute("data-tiara-action-name") === "헤드글씨크기_클릭" ||
      child.innerText === "original link"
    ) {
      continue;
    }
    if (child.closest('img, button, code') || child.tagName === "SCRIPT"
          || child.classList.contains("screen_out")
          || child.classList.contains("blind")
          || child.classList.contains("ico_view")
    ) {
      continue;
    }
    if (
      ["p", "h1", "h2", "h3", "h4", "h5", "h6", "span", "strong"].includes(child.tagName.toLowerCase()) ||
      (child.children.length == 0 && child.innerText != "")
    ) {
      if (child.innerText !== "") {
        injectTranslateTag(child);
        console.log(child.textContent + "\n\n");
        result.push(child);
      }
    } else {
      result.push(fetchNodesWithText(child));
    }
  }
  return result;
}

function generateUUID() {
  var timestamp = new Date().getTime();
  const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
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
    // for later inserting translated text
   var pElement = document.createElement("p");
   try {
     //node.after(pElement);
     node.parentNode.insertBefore(pElement, node.nextSibling);
   } catch(error) {
    //console.log(node.textContent);
    //console.log(error);
   }
}

if (!document.body.classList.contains("translated")) {
    document.body.classList.add("translated");
    document.originalInnerHTML = document.body.innerHTML;
    fetchNodesWithText(document.body);
} else {
    if (!document.body.classList.contains("translated_but_hide")) {
        document.translatedInnerHTML = document.body.innerHTML;
        document.body.innerHTML = document.originalInnerHTML;
        document.body.classList.add("translated_but_hide");
    } else {
        document.body.innerHTML = document.translatedInnerHTML;
        document.body.classList.remove("translated_but_hide");
    }
}
