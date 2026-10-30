(function (xFraction, yFraction, selectText) {
    const viewport = window.visualViewport;
    const x = (viewport ? viewport.offsetLeft : 0) + xFraction * (viewport ? viewport.width : window.innerWidth);
    const y = (viewport ? viewport.offsetTop : 0) + yFraction * (viewport ? viewport.height : window.innerHeight);
    const element = document.elementFromPoint(x, y);
    if (!element || element.isContentEditable || element.closest('input, textarea')) return null;
    const link = element.closest('a[href]');
    const image = element.closest('img');
    if (!link && !image) {
        if (!selectText) return null;
        const range = document.caretRangeFromPoint(x, y);
        if (!range || range.startContainer.nodeType !== Node.TEXT_NODE) return null;
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        // Use Chromium's word boundaries, including languages without spaces.
        selection.modify('move', 'forward', 'character');
        selection.modify('move', 'backward', 'word');
        selection.modify('extend', 'forward', 'word');
        return { selected: true };
    }
    // Also used by the existing Select Text action to temporarily disable the link.
    window._touchTarget = link;
    return {
        url: link ? link.href : (image.currentSrc || image.src),
        image: image ? (image.currentSrc || image.src) : '',
        title: link ? link.innerText : image.alt
    };
})
