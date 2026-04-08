(function() {
    window.__einkbroEbookTouchEnabled = true;
    // Remove existing listeners before (re-)installing
    if (window.__einkbroEbookTouchCleanup) {
        window.__einkbroEbookTouchCleanup();
    }

    var touchStartX = 0;
    var touchStartY = 0;
    var touchStartTime = 0;
    var touchMoved = false;
    var tapHandled = false;
    var MOVE_THRESHOLD = 15;
    var LONG_PRESS_MS = 400;

    function onTouchStart(e) {
        if (e.touches.length !== 1) return;
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
        touchStartTime = Date.now();
        touchMoved = false;
        tapHandled = false;
    }

    function onTouchMove(e) {
        if (touchMoved) return;
        if (e.touches.length !== 1) return;
        var dx = Math.abs(e.touches[0].clientX - touchStartX);
        var dy = Math.abs(e.touches[0].clientY - touchStartY);
        if (dx > MOVE_THRESHOLD || dy > MOVE_THRESHOLD) {
            touchMoved = true;
        }
    }

    function onTouchEnd(e) {
        if (!window.__einkbroEbookTouchEnabled) return;
        if (touchMoved) return;
        if (e.changedTouches.length !== 1) return;

        var duration = Date.now() - touchStartTime;
        if (duration > LONG_PRESS_MS) return;

        e.preventDefault();
        tapHandled = true;

        // if action mode (text selection menu) is active, dismiss it instead of scrolling
        if (androidApp.dismissActionMode()) return;

        var midX = window.innerWidth / 2;
        if (touchStartX < midX) {
            androidApp.ebookPageUp();
        } else {
            androidApp.ebookPageDown();
        }
    }

    // Block ALL clicks — WebView handles link navigation at native level
    // and touchend preventDefault alone doesn't stop it
    function onClick(e) {
        if (tapHandled) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            tapHandled = false;
        }
    }

    document.addEventListener('touchstart', onTouchStart, true);
    document.addEventListener('touchmove', onTouchMove, true);
    document.addEventListener('touchend', onTouchEnd, true);
    document.addEventListener('click', onClick, true);

    window.__einkbroEbookTouchCleanup = function() {
        document.removeEventListener('touchstart', onTouchStart, true);
        document.removeEventListener('touchmove', onTouchMove, true);
        document.removeEventListener('touchend', onTouchEnd, true);
        document.removeEventListener('click', onClick, true);
        window.__einkbroEbookTouchCleanup = null;
    };
})();
