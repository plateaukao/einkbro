(function() {
    window.__einkbroEbookTouchEnabled = true;
    if (window.__einkbroEbookTouchInstalled) return;
    window.__einkbroEbookTouchInstalled = true;

    var touchStartX = 0;
    var touchStartY = 0;
    var touchStartTime = 0;
    var touchMoved = false;
    var tapHandled = false;
    var MOVE_THRESHOLD = 15;
    var LONG_PRESS_MS = 400;

    document.addEventListener('touchstart', function(e) {
        if (e.touches.length !== 1) return;
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
        touchStartTime = Date.now();
        touchMoved = false;
        tapHandled = false;
    }, true);

    document.addEventListener('touchmove', function(e) {
        if (touchMoved) return;
        if (e.touches.length !== 1) return;
        var dx = Math.abs(e.touches[0].clientX - touchStartX);
        var dy = Math.abs(e.touches[0].clientY - touchStartY);
        if (dx > MOVE_THRESHOLD || dy > MOVE_THRESHOLD) {
            touchMoved = true;
        }
    }, true);

    document.addEventListener('touchend', function(e) {
        if (!window.__einkbroEbookTouchEnabled) return;
        if (touchMoved) return;
        if (e.changedTouches.length !== 1) return;

        var duration = Date.now() - touchStartTime;
        if (duration > LONG_PRESS_MS) return;

        e.preventDefault();
        tapHandled = true;

        var midX = window.innerWidth / 2;
        if (touchStartX < midX) {
            androidApp.ebookPageUp();
        } else {
            androidApp.ebookPageDown();
        }
    }, true);

    // Block ALL clicks — WebView handles link navigation at native level
    // and touchend preventDefault alone doesn't stop it
    document.addEventListener('click', function(e) {
        if (tapHandled) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
        }
    }, true);
})();
