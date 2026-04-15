(function () {
    {{DEBUG}} console.log('element hiding started on ' + document.location.href);

    var html = document.getElementsByTagName('html')[0];

    function injectStyle(text) {
        if (!text || !text.length) return;
        var style = document.createElement('style');
        html.appendChild(style);
        style.textContent = text;
    }

    // hide by injecting CSS (url-specific + misc generic)
    var styleSheet = {{BRIDGE}}.getStyleSheet(document.location.href);
    if (styleSheet.length) {
        {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
        injectStyle(styleSheet);
        {{DEBUG}} console.log('finished injecting stylesheet');
    } else {
        {{DEBUG}} console.log('stylesheet is empty, skipped');
    }

    // collect class and id names from the DOM, then ask the engine for
    // matching generic hide selectors (adblock-rust inverted index lookup)
    try {
        var classSet = {};
        var idSet = {};
        var all = document.getElementsByTagName('*');
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            var id = el.id;
            if (id && !idSet[id]) idSet[id] = true;
            var cls = el.className;
            if (cls && typeof cls === 'string') {
                var parts = cls.split(/\s+/);
                for (var j = 0; j < parts.length; j++) {
                    var c = parts[j];
                    if (c && !classSet[c]) classSet[c] = true;
                }
            }
        }
        var classList = Object.keys(classSet);
        var idList = Object.keys(idSet);
        {{DEBUG}} console.log('classes: ' + classList.length + ', ids: ' + idList.length);
        var genericCss = {{BRIDGE}}.getHiddenClassIdStyleSheet(
            classList.join('\u0001'),
            idList.join('\u0001')
        );
        if (genericCss && genericCss.length) {
            {{DEBUG}} console.log('generic hide stylesheet length: ' + genericCss.length);
            injectStyle(genericCss);
        }
    } catch (err) {
        {{DEBUG}} console.log('class/id hide failed: ' + err);
    }

    // hide by ExtendedCss
    try {
        var css = {{BRIDGE}}.getExtendedCssStyleSheet(document.location.href);
        {{DEBUG}} console.log(`ExtendedCss rules(length: ${css.length}) injecting for ${document.location.href}`);
        if (css.length > 0) {
            var extendedCss = new ExtendedCss({ styleSheet: css });
            extendedCss.apply();
        }
        {{DEBUG}} console.log(`ExtendedCss rules success for ${document.location.href}`);
    } catch (err) {
        {{DEBUG}} console.log(`ExtendedCss rules failed '${css}' for ${document.location.href} by ${err}`);
        throw err;
    }

    {{DEBUG}} console.log('element hiding finished');
})();