// Sets the viewport meta tag content. Reader mode's two-column layout pins
// initial/minimum scale here: the horizontal column overflow otherwise makes
// WebView zoom out to fit the whole strip on orientation change.
(function () {
    var meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute('name', 'viewport');
        (document.head || document.documentElement).appendChild(meta);
    }
    if (meta.getAttribute('content') !== '__VIEWPORT_CONTENT__') {
        meta.setAttribute('content', '__VIEWPORT_CONTENT__');
    }
})();
