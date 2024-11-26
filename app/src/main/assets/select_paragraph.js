let selection = window.getSelection();
if (selection.rangeCount === 0) return;

let range = selection.getRangeAt(0);
let startContainer = range.startContainer;
let endContainer = range.endContainer;

// Check if the selection is within a single text node
if (startContainer !== endContainer || startContainer.nodeType !== Node.TEXT_NODE) {
    return;
}

let textContent = startContainer.textContent;
let startOffset = range.startOffset;
let endOffset = range.endOffset;

let paragraphStart = startOffset;
let paragraphEnd = endOffset;

// Move the start of the range to the start of the paragraph (i.e., look for newline or start of the node)
while (paragraphStart > 0 && textContent[paragraphStart - 1] !== '\n') {
    paragraphStart--;
}

// Move the end of the range to the end of the paragraph (i.e., look for newline or end of the node)
while (paragraphEnd < textContent.length && textContent[paragraphEnd] !== '\n') {
    paragraphEnd++;
}

// Set the range to the paragraph boundaries
range.setStart(startContainer, paragraphStart);
range.setEnd(startContainer, paragraphEnd);

// Clear previous selection and set the new one
selection.removeAllRanges();
selection.addRange(range);
