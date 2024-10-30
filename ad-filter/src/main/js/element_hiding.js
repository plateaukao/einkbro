(function () {
    {{DEBUG}} console.log('element hiding started on ' + document.location.href);

    // hide by injecting CSS
    var styleSheet = {{BRIDGE}}.getStyleSheet(document.location.href);
    if (styleSheet.length) {
        {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
        // Why `html` here? Because the css at the end of `html` usually has a higher priority.
        var html = document.getElementsByTagName('html')[0];
        var style = document.createElement('style');
        html.appendChild(style);
        style.textContent = styleSheet;
        {{DEBUG}} console.log('finished injecting stylesheet');
    } else {
        {{DEBUG}} console.log('stylesheet is empty, skipped');
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