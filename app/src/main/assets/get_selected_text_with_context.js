let contextLength = 120;
let selection = window.getSelection();
if (selection.rangeCount === 0) return "";

let range = selection.getRangeAt(0);
let startContainer = range.startContainer;
let endContainer = range.endContainer;

// Handle the case where the selected text spans multiple nodes
if (startContainer !== endContainer) {
    return "";  // For simplicity, not handling multi-node selections here
}

let textContent = startContainer.textContent;
let startOffset = range.startOffset;
let endOffset = range.endOffset;

// Extend previousContext to the previous ".", "。", "?", or "!"
let contextStartPos = startOffset;
while (contextStartPos > 0 && ![".", "。", "?", "!"].includes(textContent[contextStartPos - 1])) {
    contextStartPos--;
    if (startOffset - contextStartPos > contextLength) {
        break;
    }
}

// Extend nextContext to the next ".", "?", or "。"
let contextEndPos = endOffset;
while (contextEndPos < textContent.length && ![".", "?", "。"].includes(textContent[contextEndPos])) {
    contextEndPos++;
    if (contextEndPos - endOffset > contextLength) {
        break;
    }
}

let previousContext = textContent.substring(contextStartPos, startOffset);
let nextContext = textContent.substring(endOffset, contextEndPos+1);

let selectedText = selection.toString();
return previousContext + "<<" + selectedText + ">>" + nextContext;
