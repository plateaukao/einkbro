(function() {
    // Some sites (e.g. io.google/2026) use a Declarative Shadow DOM
    // progressive-hydration pattern where <body dsd-pending> is hidden by
    // [dsd-pending]{display:none} until a bootstrap script removes it.
    // If any script in that chain is blocked (ad-filter, analytics blocker,
    // CORS, etc.), the attribute is never removed and the whole page stays
    // blank. Force-remove it at page-finished time as a safety net.
    document.querySelectorAll('[dsd-pending]').forEach(function(el) {
        el.removeAttribute('dsd-pending');
    });
})();
