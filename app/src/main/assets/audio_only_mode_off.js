// Disable audio-only mode
(function() {
    window.__audioOnlyModeActive = false;

    // Remove injected CSS
    var style = document.getElementById('audio-only-mode-css');
    if (style) style.remove();

    // Restore video visibility
    var videos = document.querySelectorAll('video');
    for (var i = 0; i < videos.length; i++) {
        videos[i].style.opacity = '';
    }
})();
