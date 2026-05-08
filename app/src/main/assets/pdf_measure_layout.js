(function () {
    var doc = document.documentElement;
    var bodyWidth = document.body ? document.body.scrollWidth : 0;
    return Math.max(window.innerWidth || 0, doc.clientWidth || 0, bodyWidth);
})();
