// Audio-only mode: hide video elements, force lowest quality, keep captions visible
(function() {
    if (window.__audioOnlyModeActive) return;
    window.__audioOnlyModeActive = true;

    // Add CSS to hide video elements but keep captions
    var style = document.createElement('style');
    style.id = 'audio-only-mode-css';
    style.textContent = [
        'video { opacity: 0 !important; }',
        // Keep YouTube caption containers visible
        '.ytp-caption-window-container { opacity: 1 !important; z-index: 999 !important; }',
        '.caption-window { opacity: 1 !important; }',
        '.captions-text { opacity: 1 !important; }',
        // Keep the player controls visible
        '.ytp-chrome-bottom { opacity: 1 !important; }',
        '.ytp-progress-bar-container { opacity: 1 !important; }'
    ].join('\n');
    document.head.appendChild(style);

    // Try to set lowest quality on YouTube
    function setLowestQuality() {
        var player = document.querySelector('#movie_player');
        if (player && typeof player.setPlaybackQualityRange === 'function') {
            try {
                player.setPlaybackQualityRange('tiny', 'tiny');
            } catch(e) {
                try {
                    player.setPlaybackQualityRange('small', 'small');
                } catch(e2) {}
            }
        }
    }

    setLowestQuality();
})();
