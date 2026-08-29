// Collects the icon <link> candidates of the current document together with the
// document's own hostname, so the native side can store the icon under the host
// the candidates actually belong to. Returns a JSON string or null.
(function () {
    try {
        var links = document.querySelectorAll(
            'link[rel~="icon"], link[rel~="apple-touch-icon"], link[rel~="apple-touch-icon-precomposed"]'
        );
        var icons = [];
        for (var i = 0; i < links.length; i++) {
            var link = links[i];
            if (!link.href) continue;
            icons.push({
                href: link.href,
                rel: link.rel || '',
                sizes: link.getAttribute('sizes') || '',
                type: link.type || ''
            });
        }
        return JSON.stringify({ host: location.hostname, icons: icons });
    } catch (e) {
        return null;
    }
})();
