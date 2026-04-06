(function() {
    var seen = {};
    var count = 0;
    var links = document.querySelectorAll('a[href^="http"]');
    for (var i = 0; i < links.length && count < 20; i++) {
        try {
            var host = new URL(links[i].href).origin;
            if (!seen[host] && host !== location.origin) {
                seen[host] = true;
                count++;
                var link = document.createElement('link');
                link.rel = 'dns-prefetch';
                link.href = host;
                document.head.appendChild(link);
            }
        } catch(e) {}
    }
})();
