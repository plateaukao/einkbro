(function() {
    var currentUrl = '%%IMAGE_URL%%';
    var imgs = document.querySelectorAll('img');
    var urls = [];
    var found = false;
    for (var i = 0; i < imgs.length; i++) {
        if (imgs[i].src === currentUrl) {
            found = true;
        }
        if (found) {
            try {
                imgs[i].removeAttribute('loading');
                if (imgs[i].dataset && imgs[i].dataset.src) {
                    imgs[i].src = imgs[i].dataset.src;
                }
            } catch(e) {}
            var src = imgs[i].src;
            if (src && src.startsWith('http') &&
                (src.toLowerCase().includes('jpg') || src.toLowerCase().includes('png'))) {
                urls.push(src);
            }
        }
    }
    return JSON.stringify(urls);
})();
