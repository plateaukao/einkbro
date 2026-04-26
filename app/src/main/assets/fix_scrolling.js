(function() {
    function findScrollableParent(element) {
        if (!element || element === document.body || element === document.documentElement) {
            return null;
        }

        if (element.scrollHeight > element.clientHeight + 1) {
            var overflowY = window.getComputedStyle(element).overflowY;
            if (overflowY === 'auto' || overflowY === 'scroll') {
                return element;
            }
        }

        return findScrollableParent(element.parentElement);
    }

    function findInnerScrollable() {
        var centerX = window.innerWidth / 2;
        var centerY = window.innerHeight / 2;
        var element = document.elementFromPoint(centerX, centerY);
        var fromCenter = findScrollableParent(element);
        if (fromCenter) return fromCenter;

        var fromActive = findScrollableParent(document.activeElement);
        if (fromActive) return fromActive;

        return null;
    }

    // Capture-phase scroll listener: catches ALL inner element scrolls,
    // including finger swipes, so pull-to-refresh is correctly blocked.
    document.addEventListener('scroll', function(e) {
        var target = e.target;
        if (target && target !== document && target !== document.documentElement
            && target !== document.body && typeof androidApp !== 'undefined') {
            androidApp.onInnerScrollChanged(
                target.scrollTop <= 0,
                target.scrollTop,
                target.scrollHeight,
                target.clientHeight
            );
        }
    }, true);

    // Hit-test on touchstart: if the finger landed inside a CSS-scrollable
    // container, suppress pull-to-refresh for the whole gesture even if no
    // scroll event fires (e.g. inner element already at scrollTop=0).
    document.addEventListener('touchstart', function(e) {
        if (typeof androidApp === 'undefined') return;
        try {
            androidApp.setTouchOnInnerScrollable(findScrollableParent(e.target) !== null);
        } catch (ex) {}
    }, true);

    document.addEventListener('touchend', function() {
        if (typeof androidApp === 'undefined') return;
        try { androidApp.setTouchOnInnerScrollable(false); } catch (ex) {}
    }, true);

    document.addEventListener('touchcancel', function() {
        if (typeof androidApp === 'undefined') return;
        try { androidApp.setTouchOnInnerScrollable(false); } catch (ex) {}
    }, true);

    window.__einkbroPageScroll = function(direction, offsetPercent, offsetPx) {
        var scrollable = findInnerScrollable();
        if (!scrollable) return "false";

        var scrollAmount = direction * (scrollable.clientHeight * (1 - offsetPercent) - offsetPx);
        scrollable.scrollBy({
            top: scrollAmount,
            left: 0,
            behavior: 'auto'
        });
        return "true";
    };

    window.__einkbroScrollToTop = function() {
        var scrollable = findInnerScrollable();
        if (scrollable) {
            scrollable.scrollTo({ top: 0, behavior: 'auto' });
        }
    };
})();
