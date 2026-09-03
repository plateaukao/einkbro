(function () {
    {{DEBUG}} console.log('element hiding started on ' + document.location.href);

    var html = document.getElementsByTagName('html')[0];

    // hide by injecting CSS
    var styleSheet = {{BRIDGE}}.getStyleSheet(document.location.href);
    if (styleSheet.length) {
        {{DEBUG}} console.log('stylesheet length: ' + styleSheet.length);
        // Why `html` here? Because the css at the end of `html` usually has a higher priority.
        var style = document.createElement('style');
        html.appendChild(style);
        style.textContent = styleSheet;
        {{DEBUG}} console.log('finished injecting stylesheet');
    } else {
        {{DEBUG}} console.log('stylesheet is empty, skipped');
    }

    // extended selectors (:has etc.) as native CSS, one standalone rule per
    // selector, so an unsupported selector only drops its own rule
    var extCss = {{BRIDGE}}.getExtendedCssStyleSheet(document.location.href);
    if (extCss.length > 0) {
        {{DEBUG}} console.log('extended-selector stylesheet length: ' + extCss.length);
        var extStyle = document.createElement('style');
        html.appendChild(extStyle);
        extStyle.textContent = extCss;
    }

    {{DEBUG}} console.log('element hiding finished');
})();
