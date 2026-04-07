(function() {
    if (window.__einkbroAutoplayBlocked) return;
    window.__einkbroAutoplayBlocked = true;

    var userGesture = false;

    ['click', 'touchstart', 'touchend'].forEach(function(evt) {
        document.addEventListener(evt, function() {
            userGesture = true;
            setTimeout(function() { userGesture = false; }, 2000);
        }, true);
    });

    // Layer 1: Override .play() via defineProperty (harder to overwrite)
    var _origPlay = HTMLMediaElement.prototype.play;
    Object.defineProperty(HTMLMediaElement.prototype, 'play', {
        configurable: true,
        enumerable: true,
        writable: true,
        value: function() {
            if (userGesture) return _origPlay.call(this);
            this.pause();
            return Promise.resolve();
        }
    });

    // Layer 2: Intercept IntersectionObserver — Instagram's autoplay trigger
    var _OrigIO = window.IntersectionObserver;
    if (_OrigIO) {
        window.IntersectionObserver = function(callback, options) {
            var wrappedCallback = function(entries, observer) {
                if (userGesture) return callback.call(this, entries, observer);
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

    // Layer 3: Intercept video element creation
    var _createElement = document.createElement.bind(document);
    document.createElement = function(tag) {
        var el = _createElement(tag);
        if (tag.toLowerCase() === 'video') {
            el.autoplay = false;
            el.removeAttribute('autoplay');
        }
        return el;
    };
    // Preserve prototype chain and static properties
    document.createElement.__proto__ = _createElement.__proto__;

    // Layer 4: Event-based catch-all
    document.addEventListener('play', function(e) {
        if (!userGesture && e.target instanceof HTMLMediaElement) {
            e.target.pause();
        }
    }, true);

    // Layer 5: Handle existing elements
    function disableAutoplay(el) {
        el.removeAttribute('autoplay');
        el.autoplay = false;
        if (!el.paused && !userGesture) el.pause();
    }
    document.querySelectorAll('video, audio').forEach(disableAutoplay);

    // Layer 6: MutationObserver for dynamically added elements
    new MutationObserver(function(mutations) {
        mutations.forEach(function(m) {
            m.addedNodes.forEach(function(node) {
                if (node.nodeType !== 1) return;
                if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                    disableAutoplay(node);
                }
                if (node.querySelectorAll) {
                    node.querySelectorAll('video, audio').forEach(disableAutoplay);
                }
            });
        });
    }).observe(document.documentElement || document, {childList: true, subtree: true});

    // Layer 7: Periodic sweep as last resort
    setInterval(function() {
        if (userGesture) return;
        document.querySelectorAll('video').forEach(function(v) {
            if (!v.paused) v.pause();
        });
    }, 500);
})();
