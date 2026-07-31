(function () {
    if (typeof enableSiteStyleSheets === 'function') enableSiteStyleSheets();
    // innerHTMLCache is an expando saved by reader mode before it replaced the
    // body; a document that never entered reader mode doesn't have it, and
    // assigning it anyway would blank the page to the literal string "undefined".
    if (typeof document.innerHTMLCache === 'string') {
        document.body.innerHTML = document.innerHTMLCache;
    }
    document.body.classList.remove('mozac-readerview-body');
    window.scrollTo(0, 0);
})();
