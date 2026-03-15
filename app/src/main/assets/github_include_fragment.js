// Polyfill for GitHub's <include-fragment> custom element.
// On older WebView versions, GitHub's JS bundles fail to parse (modern syntax),
// so the custom element never gets defined and assets sections stay "Loading" forever.
// This script uses ES5-compatible code to manually fetch and inject the fragment content.
(function() {
    function loadFragment(fragment) {
        var src = fragment.getAttribute('src');
        if (!src || fragment.getAttribute('data-eb-loaded')) return;
        fragment.setAttribute('data-eb-loaded', 'true');

        var xhr = new XMLHttpRequest();
        xhr.open('GET', src, true);
        xhr.setRequestHeader('Accept', 'text/fragment+html');
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4 && xhr.status === 200) {
                var parent = fragment.parentNode;
                if (!parent) return;
                var temp = document.createElement('div');
                temp.innerHTML = xhr.responseText;
                while (temp.firstChild) {
                    parent.insertBefore(temp.firstChild, fragment);
                }
                parent.removeChild(fragment);
            }
        };
        xhr.send();
    }

    function isVisible(el) {
        var node = el;
        while (node) {
            if (node.tagName === 'DETAILS' && !node.hasAttribute('open')) {
                return false;
            }
            node = node.parentElement;
        }
        return true;
    }

    function processFragments() {
        var fragments = document.querySelectorAll('include-fragment[src]:not([data-eb-loaded])');
        for (var i = 0; i < fragments.length; i++) {
            if (isVisible(fragments[i])) {
                loadFragment(fragments[i]);
            }
        }
    }

    // Wait for GitHub's own JS to have a chance to handle things first.
    // If include-fragment elements still exist after 3 seconds, our polyfill kicks in.
    setTimeout(function() {
        var fragments = document.querySelectorAll('include-fragment[src]');
        if (fragments.length === 0) return;

        processFragments();

        // When a <details> element is toggled open, load any fragments inside it
        document.addEventListener('toggle', function(e) {
            if (e.target && e.target.tagName === 'DETAILS' && e.target.hasAttribute('open')) {
                var frags = e.target.querySelectorAll('include-fragment[src]:not([data-eb-loaded])');
                for (var i = 0; i < frags.length; i++) {
                    loadFragment(frags[i]);
                }
            }
        }, true);
    }, 3000);
})();
