function removeHighlightFromSelection() {
    const selection = window.getSelection();
    // 檢查是否有選取範圍
    if (!selection.rangeCount) return;
    const range = selection.getRangeAt(0);
    const container = range.commonAncestorContainer;
    // 確保範圍是在一個元素內部
    const parentElement = container.nodeType === 3 ? container.parentNode : container;

    // 查找所有的 highlight divs
    const highlights = parentElement.parentNode.querySelectorAll('div.highlight_underline, div.highlight_yellow, div.highlight_green, div.highlight_blue, div.highlight_pink');

    // 移除每個 highlight div 的外部 HTML
    highlights.forEach(highlight => {
        highlight.outerHTML = highlight.innerHTML;
    });
}

// 綁定一個按鈕來觸發這個函數
removeHighlightFromSelection();
