// Creates or updates a per-slot <style> element identified by a stable id, so
// repeated style changes replace the previous CSS instead of accumulating in
// <head>, and an empty CSS string reverts the slot without reloading the page.
(function () {
    var id = 'einkbro-css-__SLOT_ID__';
    var css = '';
    try {
        css = decodeURIComponent(escape(window.atob('__CSS_B64__')));
    } catch (e) {
        console.error('einkbro css slot decode', e);
        return;
    }
    var el = document.getElementById(id);
    if (!css) {
        if (el && el.parentNode) el.parentNode.removeChild(el);
        return;
    }
    // Skip the DOM mutation when nothing changed to avoid e-ink repaints.
    if (el && el.textContent === css) return;
    if (!el) {
        el = document.createElement('style');
        el.id = id;
        el.type = 'text/css';
        (document.head || document.documentElement).appendChild(el);
    }
    el.textContent = css;
})();
