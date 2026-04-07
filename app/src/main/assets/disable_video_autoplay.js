(function() {
    if (window.__einkbroAutoplayBlocked) return;
    window.__einkbroAutoplayBlocked = true;

    var blockedError = new DOMException('play() was blocked because user preference', 'NotAllowedError');
    Object.defineProperty(HTMLMediaElement.prototype, 'play', {
        configurable: true,
        enumerable: true,
        writable: true,
        value: function() {
            return Promise.reject(blockedError);
        }
    });

    // Intercept IntersectionObserver — Instagram's autoplay trigger
    var _OrigIO = window.IntersectionObserver;
    if (_OrigIO) {
        window.IntersectionObserver = function(callback, options) {
            var wrappedCallback = function(entries, observer) {
                var modified = entries.map(function(entry) {
                    var target = entry.target;
                    var hasVideo = (target.tagName === 'VIDEO') ||
                        (target.querySelector && target.querySelector('video'));
                    if (hasVideo) {
                        return Object.create(entry, {
                            isIntersecting: { value: false },
                            intersectionRatio: { value: 0 }
                        });
                    }
                    return entry;
                });
                return callback.call(this, modified, observer);
            };
            return new _OrigIO(wrappedCallback, options);
        };
        window.IntersectionObserver.prototype = _OrigIO.prototype;
        Object.keys(_OrigIO).forEach(function(k) {
            window.IntersectionObserver[k] = _OrigIO[k];
        });
    }

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

    // Event-based catch-all
    document.addEventListener('play', function(e) {
        if (e.target instanceof HTMLMediaElement) e.target.pause();
    }, true);

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
