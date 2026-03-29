// Disable audio-only mode
(function() {
    window.__audioOnlyModeActive = false;

    // Remove injected CSS
    var style = document.getElementById('audio-only-mode-css');
    if (style) style.remove();

    // Disconnect observer
    if (window.__audioOnlyObserver) {
        window.__audioOnlyObserver.disconnect();
        window.__audioOnlyObserver = null;
    }

    // Restore video visibility
    var videos = document.querySelectorAll('video');
    for (var i = 0; i < videos.length; i++) {
        videos[i].style.opacity = '';
    }
})();
