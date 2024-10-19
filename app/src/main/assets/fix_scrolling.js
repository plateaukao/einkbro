function findScrollableParent(element) {
    if (!element) {
        return document.scrollingElement || document.documentElement;
    }

    if (element.scrollHeight > element.clientHeight) {
        const overflowY = window.getComputedStyle(element).overflowY;
        if (overflowY !== 'visible' && overflowY !== 'hidden') {
            return element;
        }
    }

    return findScrollableParent(element.parentElement);
}

function scrollPage(direction) {
    const scrollable = findScrollableParent(document.activeElement);
    const scrollAmount = direction * scrollable.clientHeight * (1 - %s) - %s;
    scrollable.scrollBy({
        top: scrollAmount,
        left: 0,
        behavior: 'auto'
    });
}

window.addEventListener('keydown', function(e) {
    if (e.key === 'PageDown' || e.keyCode === 34) {
        e.preventDefault();
        scrollPage(1);
    } else if (e.key === 'PageUp' || e.keyCode === 33) {
        e.preventDefault();
        scrollPage(-1);
    }
}, true);
