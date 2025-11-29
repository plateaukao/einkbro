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

  // 1. Check if this element should be treated as a single block
  // (e.g. <p>text <a>link</a> text</p> without <br>)
  // We skip this check for the root body or very large containers usually, 
  // but here we rely on the caller or the recursion to narrow it down.
  // However, we must be careful not to translate the whole body if it's just one big container.
  // The original logic had a specific list of tags. Let's keep some of that heuristic.

  const isBlockTag = ["p", "h1", "h2", "h3", "h4", "h5", "h6", "li"].includes(element.tagName.toLowerCase());

  if (isBlockTag && shouldTranslateAsBlock(element) && element.innerText.trim() !== "") {
    injectTranslateTag(element);
    console.log("Block: " + element.textContent + "\n\n");
    result.push(element);
    return result;
  }

  // 2. If not a simple block, iterate children and group inlines
  var children = Array.from(element.childNodes);
  var currentGroup = [];

  function flushGroup() {
    if (currentGroup.length === 0) return;

    // Check if group has any meaningful text
    const hasText = currentGroup.some(node => node.textContent.trim().length > 0);

    if (hasText) {
      // If group is just one element and it's already an element (not text), 
      // we might want to just tag it? No, usually we want to wrap mixed content.
      // But if it's a single <span>, maybe just tag it?
      // Let's consistently wrap to be safe and uniform.

      var span = document.createElement("span");
      // We need to insert the span before the first item of the group
      // But since we are replacing items, we have to be careful.

      const firstNode = currentGroup[0];
      const parent = firstNode.parentNode;

      // Insert span before first node
      parent.insertBefore(span, firstNode);

      // Move all nodes into span
      currentGroup.forEach(node => span.appendChild(node));

      injectTranslateTag(span);
      console.log("Group: " + span.textContent + "\n\n");
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
  // for later inserting translated text
  var pElement = document.createElement("p");
  try {
    //node.after(pElement);
    node.parentNode.insertBefore(pElement, node.nextSibling);
  } catch (error) {
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
