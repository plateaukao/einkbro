(function() {
    var imageUrl = '%%IMAGE_URL%%';
    var base64Data = '%%BASE64_DATA%%';
    var imgs = document.querySelectorAll('img');
    var originalImg = null;
    for (var i = 0; i < imgs.length; i++) {
        if (imgs[i].src === imageUrl) {
            originalImg = imgs[i];
            break;
        }
    }
    if (!originalImg) return;

    var existing = originalImg.parentElement.querySelector('.eb-translated-overlay');
    if (existing) {
        existing.remove();
    }

    var wrapper = originalImg.parentElement;
    if (!wrapper.classList.contains('eb-translate-wrapper')) {
        wrapper = document.createElement('div');
        wrapper.className = 'eb-translate-wrapper';
        wrapper.style.cssText = 'position:relative;display:inline-block;';
        originalImg.parentNode.insertBefore(wrapper, originalImg);
        wrapper.appendChild(originalImg);
    }

    var overlay = document.createElement('img');
    overlay.className = 'eb-translated-overlay';
    overlay.src = 'data:image/jpeg;base64,' + base64Data;
    overlay.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;z-index:9999;cursor:pointer;object-fit:fill;';
    wrapper.appendChild(overlay);

    overlay.onclick = function(e) {
        e.stopPropagation();
        e.preventDefault();
        this.style.display = 'none';
    };
    originalImg.style.cursor = 'pointer';
    originalImg.onclick = function(e) {
        e.stopPropagation();
        e.preventDefault();
        overlay.style.display = '';
    };
})();
