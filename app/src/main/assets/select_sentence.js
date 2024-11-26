let selection = window.getSelection();
if (selection.rangeCount === 0) return;

let range = selection.getRangeAt(0);
let startContainer = range.startContainer;
let endContainer = range.endContainer;

if (startContainer !== endContainer || startContainer.nodeType !== Node.TEXT_NODE) {
    // Only handle cases where the selection is within a single text node
    return;
}

let textContent = startContainer.textContent;
let startOffset = range.startOffset;
let endOffset = range.endOffset;

let sentenceStart = startOffset;
let sentenceEnd = endOffset;

// Move the start of the range to the start of the sentence
while (sentenceStart > 0 && ![".", "?", "。", "!"].includes(textContent[sentenceStart - 1])) {
    sentenceStart--;
}

// Move the end of the range to the end of the sentence
while (sentenceEnd < textContent.length && ![".", "?", "。", "!"].includes(textContent[sentenceEnd])) {
    sentenceEnd++;
}

// Set the range to the sentence boundaries
range.setStart(startContainer, sentenceStart);
range.setEnd(startContainer, sentenceEnd);

// Clear previous selection and set the new one
selection.removeAllRanges();
selection.addRange(range);
