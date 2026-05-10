(function () {
    var W = __WIDTH__;
    if (!W || W < 1) return;

    function setViewport() {
        var head = document.head;
        if (!head) return;
        var meta = head.querySelector('meta[name="viewport"]');
        var desired = 'width=' + W + ', initial-scale=1.0, user-scalable=yes';
        if (!meta) {
            meta = document.createElement('meta');
            meta.setAttribute('name', 'viewport');
            meta.setAttribute('content', desired);
            head.appendChild(meta);
        } else if (meta.getAttribute('content') !== desired) {
            meta.setAttribute('content', desired);
        }
    }

    setViewport();

    function watch(target) {
        try {
            new MutationObserver(setViewport).observe(target, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['content', 'name']
            });
        } catch (e) {}
    }

    if (document.head) {
        watch(document.head);
    } else if (document.documentElement) {
        var bootstrap = new MutationObserver(function () {
            if (document.head) {
                setViewport();
                watch(document.head);
                bootstrap.disconnect();
            }
        });
        bootstrap.observe(document.documentElement, { childList: true, subtree: true });
    }
})();
