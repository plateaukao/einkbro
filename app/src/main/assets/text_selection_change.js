            var selectedText = "";
            function getSelectionPositionInWebView() {
    let selection = window.getSelection();

    if (selection) {
        let range = selection.getRangeAt(0);
        let startNode = range.startContainer;
        let startOffset = range.startOffset;
        let endNode = range.endContainer;
        let endOffset = range.endOffset;

        let start = getRectInWebView(startNode, startOffset);
        let end = getRectInWebView(endNode, endOffset);

            // Send anchor position to Android
            if (selection.toString() != selectedText) {
                selectedText = selection.toString();
                if (selectedText.length > 0) {
                    androidApp.getAnchorPosition(start.left, start.top, end.right, end.bottom);
                }
            }
    }
}

function getRectInWebView(node, offset) {
    let range = document.createRange();
    range.setStart(node, offset);
    range.setEnd(node, offset);
    let rect = range.getBoundingClientRect();

    return rect;
}

// Call the function to get selection position
document.addEventListener("selectionchange", function() {
    getSelectionPositionInWebView();
    });
