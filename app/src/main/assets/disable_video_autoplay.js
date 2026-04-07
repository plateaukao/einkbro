(function() {
    if (window.__einkbroAutoplayBlocked) return;
    window.__einkbroAutoplayBlocked = true;

    // Strip autoplay attribute from dynamically created videos
    var _createElement = document.createElement.bind(document);
    document.createElement = function(tag) {
        var el = _createElement(tag);
        if (tag.toLowerCase() === 'video') {
            el.autoplay = false;
            el.removeAttribute('autoplay');
        }
        return el;
    };
    document.createElement.__proto__ = _createElement.__proto__;

    // Handle existing elements
    document.querySelectorAll('video, audio').forEach(function(el) {
        el.removeAttribute('autoplay');
        el.autoplay = false;
        if (!el.paused) el.pause();
    });

    // Watch for dynamically added video/audio elements
    new MutationObserver(function(mutations) {
        mutations.forEach(function(m) {
            m.addedNodes.forEach(function(node) {
                if (node.nodeType !== 1) return;
                if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                    node.removeAttribute('autoplay');
                    node.autoplay = false;
                }
                if (node.querySelectorAll) {
                    node.querySelectorAll('video, audio').forEach(function(el) {
                        el.removeAttribute('autoplay');
                        el.autoplay = false;
                    });
                }
            });
        });
    }).observe(document.documentElement || document, {childList: true, subtree: true});
})();
