// EinkBro userscript GM API shim.
// Templated per-script: __SCRIPT_ID__ and __GM_INFO__ are substituted at inject time.
// Defines GM_* and the promisified GM.* on the page's window so userscripts can use them.

// Some Android System WebView builds don't expose `globalThis` in the injected script
// scope, which breaks modern bundles (e.g. Immersive Translate). Polyfill it first.
if (typeof globalThis === 'undefined') {
    try {
        (typeof self !== 'undefined' ? self : window).globalThis =
            (typeof self !== 'undefined' ? self : window);
    } catch (e) {}
}

// Scripts that route all network through GM_xmlhttpRequest (Immersive Translate deletes
// window.fetch to bypass CORS) wrap each response in `new Response(body, {status})`.
// Null-body statuses (204/205/304) make that throw "Response with null body status
// cannot have body"; subclass so those statuses drop the body.
(function () {
    var Orig = window.Response;
    if (!Orig) return;
    try {
        window.Response = class extends Orig {
            constructor(body, init) {
                init = init || {};
                var s = init.status;
                if (body != null && (s === 204 || s === 205 || s === 304)) super(null, init);
                else super(body, init);
            }
        };
    } catch (e) { /* keep native Response if subclassing unsupported */ }
})();

(function () {
    var SCRIPT_ID = __SCRIPT_ID__;
    var GM_INFO = __GM_INFO__;

    // Shared per-page registry, keyed by script id, on a single hidden global.
    var hub = window.__einkbroGM;
    if (!hub) {
        hub = window.__einkbroGM = {
            xhrCallbacks: {},
            menuCallbacks: {},
            seq: 0,
            handleXhr: function (reqId, eventName, payload) {
                var self = this;
                // Dispatch asynchronously (macrotask). The native bridge delivers this
                // synchronously via evaluateJavascript, which can run inside the same JS
                // turn as a script's `new Promise(executor)` that called GM_xmlhttpRequest.
                // Resolving that promise synchronously from within its own executor leaves
                // it permanently pending in this WebView's V8 — so defer by a turn.
                setTimeout(function () {
                    var cb = self.xhrCallbacks[reqId];
                    if (!cb) return;
                    var resp;
                    try { resp = JSON.parse(payload); } catch (e) { resp = {}; }
                    if (cb.responseType === 'json' && typeof resp.responseText === 'string') {
                        try { resp.response = JSON.parse(resp.responseText); } catch (e2) {}
                    }
                    try {
                        if (eventName === 'progress') {
                            if (cb.onprogress) cb.onprogress(resp);
                        } else if (eventName === 'error') {
                            if (cb.onerror) cb.onerror(resp);
                        } else if (eventName === 'timeout') {
                            if (cb.ontimeout) cb.ontimeout(resp);
                            else if (cb.onerror) cb.onerror(resp);
                        } else if (eventName === 'load') {
                            // Tampermonkey order: onreadystatechange(readyState 4) then onload.
                            resp.readyState = 4;
                            if (cb.onreadystatechange) cb.onreadystatechange(resp);
                            if (cb.onload) cb.onload(resp);
                        }
                    } catch (cbErr) {
                        console.error('einkbro GM_xhr callback error', cbErr);
                    }
                    if (eventName === 'load' || eventName === 'error' || eventName === 'timeout') {
                        delete self.xhrCallbacks[reqId];
                    }
                }, 0);
            },
            invokeMenu: function (fnId) {
                var cb = this.menuCallbacks[fnId];
                if (cb) cb();
            }
        };
    }

    var bridge = window.einkbroGM;

    function GM_getValue(key, defaultValue) {
        try {
            var raw = bridge.gmGetValue(SCRIPT_ID, key);
            if (raw === null || raw === undefined) return defaultValue;
            return JSON.parse(raw);
        } catch (e) {
            return defaultValue;
        }
    }

    function GM_setValue(key, value) {
        bridge.gmSetValue(SCRIPT_ID, key, JSON.stringify(value));
    }

    function GM_deleteValue(key) {
        bridge.gmDeleteValue(SCRIPT_ID, key);
    }

    function GM_listValues() {
        try {
            return JSON.parse(bridge.gmListValues(SCRIPT_ID) || '[]');
        } catch (e) {
            return [];
        }
    }

    function GM_addStyle(css) {
        var style = document.createElement('style');
        style.textContent = css;
        (document.head || document.documentElement).appendChild(style);
        return style;
    }

    function GM_addElement(parentOrTag, tagOrAttrs, maybeAttrs) {
        var parent, tag, attrs;
        if (typeof parentOrTag === 'string') {
            parent = null; tag = parentOrTag; attrs = tagOrAttrs || {};
        } else {
            parent = parentOrTag; tag = tagOrAttrs; attrs = maybeAttrs || {};
        }
        var el = document.createElement(tag);
        for (var k in attrs) {
            if (!attrs.hasOwnProperty(k)) continue;
            if (k === 'textContent') el.textContent = attrs[k];
            else el.setAttribute(k, attrs[k]);
        }
        (parent || document.head || document.documentElement).appendChild(el);
        return el;
    }

    function GM_xmlhttpRequest(details) {
        var reqId = SCRIPT_ID + ':' + (++hub.seq);
        hub.xhrCallbacks[reqId] = {
            onload: details.onload,
            onerror: details.onerror,
            ontimeout: details.ontimeout,
            onprogress: details.onprogress,
            onreadystatechange: details.onreadystatechange,
            responseType: details.responseType || ''
        };
        var wire = {
            method: details.method || 'GET',
            url: details.url,
            headers: details.headers || {},
            data: details.data != null ? details.data : null,
            responseType: details.responseType || '',
            timeout: details.timeout || 0,
            user: details.user || null,
            password: details.password || null
        };
        try {
            bridge.gmXhr(SCRIPT_ID, reqId, JSON.stringify(wire));
        } catch (e) {
            delete hub.xhrCallbacks[reqId];
            if (details.onerror) details.onerror({ error: String(e) });
        }
        return {
            abort: function () { try { bridge.gmAbortXhr(reqId); } catch (e) {} }
        };
    }

    function GM_registerMenuCommand(caption, fn) {
        var fnId = SCRIPT_ID + ':menu:' + (++hub.seq);
        hub.menuCallbacks[fnId] = fn;
        try { bridge.gmRegisterMenuCommand(SCRIPT_ID, String(caption), fnId); } catch (e) {}
        return fnId;
    }

    function GM_unregisterMenuCommand(fnId) {
        delete hub.menuCallbacks[fnId];
        try { bridge.gmUnregisterMenuCommand(fnId); } catch (e) {}
    }

    function GM_openInTab(url, options) {
        var active = true;
        if (typeof options === 'boolean') active = !options; // legacy: openInBackground
        else if (options && typeof options === 'object') active = options.active !== false;
        try { bridge.gmOpenInTab(url, active); } catch (e) {}
        return { closed: false, close: function () {} };
    }

    function GM_setClipboard(text) {
        try { bridge.gmSetClipboard(String(text)); } catch (e) {}
    }

    function GM_log() {
        try { bridge.gmLog(Array.prototype.join.call(arguments, ' ')); } catch (e) {}
    }

    function GM_notification(textOrDetails) {
        var text = typeof textOrDetails === 'object' ? textOrDetails.text : textOrDetails;
        try { bridge.gmNotification(String(text)); } catch (e) {}
    }

    function promisify(fn) {
        return function () {
            var args = arguments;
            return new Promise(function (resolve, reject) {
                try { resolve(fn.apply(null, args)); } catch (e) { reject(e); }
            });
        };
    }

    // GM.xmlHttpRequest must support BOTH styles, exactly like Tampermonkey:
    //  - callback style: caller passes details.onload/onerror; we fire those and return a
    //    control object. Immersive Translate's GM_fetch polyfill uses THIS style.
    //  - promise style: when no onload is given, return a thenable resolving to the response.
    // Implementing only the promise form silently swallows callback-style callers, leaving
    // their wrapping promise pending forever.
    function GM_xmlhttpRequestDual(details) {
        details = details || {};
        if (details.onload || details.onerror || details.onreadystatechange) {
            return GM_xmlhttpRequest(details); // callback style
        }
        // promise style — wire resolve/reject as the callbacks
        return new Promise(function (resolve, reject) {
            var d = {};
            for (var k in details) d[k] = details[k];
            d.onload = resolve;
            d.onerror = reject;
            d.ontimeout = reject;
            GM_xmlhttpRequest(d);
        });
    }

    // Expose globals on window (main world).
    var w = window;
    w.GM_getValue = GM_getValue;
    w.GM_setValue = GM_setValue;
    w.GM_deleteValue = GM_deleteValue;
    w.GM_listValues = GM_listValues;
    w.GM_addStyle = GM_addStyle;
    w.GM_addElement = GM_addElement;
    w.GM_xmlhttpRequest = GM_xmlhttpRequest;
    w.GM_registerMenuCommand = GM_registerMenuCommand;
    w.GM_unregisterMenuCommand = GM_unregisterMenuCommand;
    w.GM_openInTab = GM_openInTab;
    w.GM_setClipboard = GM_setClipboard;
    w.GM_log = GM_log;
    w.GM_notification = GM_notification;
    w.GM_info = GM_INFO;
    w.unsafeWindow = w;

    w.GM = {
        info: GM_INFO,
        getValue: promisify(GM_getValue),
        setValue: promisify(GM_setValue),
        deleteValue: promisify(GM_deleteValue),
        listValues: promisify(GM_listValues),
        addStyle: promisify(GM_addStyle),
        addElement: promisify(GM_addElement),
        registerMenuCommand: GM_registerMenuCommand,
        unregisterMenuCommand: GM_unregisterMenuCommand,
        openInTab: GM_openInTab,
        setClipboard: promisify(GM_setClipboard),
        notification: promisify(GM_notification),
        xmlHttpRequest: GM_xmlhttpRequestDual,
        xmlhttpRequest: GM_xmlhttpRequestDual,
        log: GM_log
    };
})();
