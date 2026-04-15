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

    // Progressively collect class and id names from the DOM and ask the engine for
    // matching generic hide selectors (adblock-rust inverted index lookup). performScript
    // runs at onPageStarted, BEFORE the page has mounted its body, so a one-shot scan
    // misses everything ads-related. We debounce-scan on DOMContentLoaded, window load,
    // and via a MutationObserver that picks up dynamically-added nodes and attr changes.
    var seenClasses = {};
    var seenIds = {};
    var genericStyle = null;
    var scanPending = false;

    function absorbElement(el) {
        var changed = false;
        var id = el.id;
        if (id && !seenIds[id]) {
            seenIds[id] = true;
            changed = true;
        }
        var cls = el.className;
        if (cls && typeof cls === 'string') {
            var parts = cls.split(/\s+/);
            for (var j = 0; j < parts.length; j++) {
                var c = parts[j];
                if (c && !seenClasses[c]) {
                    seenClasses[c] = true;
                    changed = true;
                }
            }
        }
        return changed;
    }

    function absorbSubtree(root) {
        var changed = false;
        if (!root) return false;
        if (root.nodeType === 1) {
            if (absorbElement(root)) changed = true;
            if (root.getElementsByTagName) {
                var els = root.getElementsByTagName('*');
                for (var i = 0; i < els.length; i++) {
                    if (absorbElement(els[i])) changed = true;
                }
            }
        } else if (root.getElementsByTagName) {
            var els2 = root.getElementsByTagName('*');
            for (var k = 0; k < els2.length; k++) {
                if (absorbElement(els2[k])) changed = true;
            }
        }
        return changed;
    }

    function applyGenericHide() {
        var classList = Object.keys(seenClasses);
        var idList = Object.keys(seenIds);
        if (!classList.length && !idList.length) return;
        var genericCss = {{BRIDGE}}.getHiddenClassIdStyleSheet(
            classList.join('\u0001'),
            idList.join('\u0001')
        );
        if (!genericCss || !genericCss.length) return;
        if (!genericStyle) {
            genericStyle = document.createElement('style');
            (document.head || document.documentElement || html).appendChild(genericStyle);
        }
        genericStyle.textContent = genericCss;
        {{DEBUG}} console.log('generic hide ' + classList.length + 'c/' + idList.length + 'i css=' + genericCss.length);
    }

    function scheduleApply() {
        if (scanPending) return;
        scanPending = true;
        setTimeout(function () {
            scanPending = false;
            applyGenericHide();
        }, 150);
    }

    function rescanFull() {
        if (absorbSubtree(document)) {
            applyGenericHide();
        }
    }

    try {
        if (typeof MutationObserver !== 'undefined') {
            var observer = new MutationObserver(function (mutations) {
                var changed = false;
                for (var m = 0; m < mutations.length; m++) {
                    var mu = mutations[m];
                    if (mu.type === 'attributes') {
                        if (absorbElement(mu.target)) changed = true;
                    } else if (mu.type === 'childList') {
                        var added = mu.addedNodes;
                        for (var a = 0; a < added.length; a++) {
                            if (absorbSubtree(added[a])) changed = true;
                        }
                    }
                }
                if (changed) scheduleApply();
            });
            observer.observe(document.documentElement || document, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['class', 'id']
            });
        }

        rescanFull();

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', rescanFull, false);
        }
        window.addEventListener('load', rescanFull, false);
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