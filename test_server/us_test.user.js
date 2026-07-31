// ==UserScript==
// @name         EinkBro iOS Inject Test
// @namespace    einkbro-test
// @version      1.0
// @description  Verifies injection, GM_setValue/getValue persistence, GM_addStyle, and menu commands
// @match        http://localhost:8000/*
// @run-at       document-end
// @grant        GM_setValue
// @grant        GM_getValue
// @grant        GM_addStyle
// @grant        GM_registerMenuCommand
// @grant        GM_notification
// ==/UserScript==
(function () {
    'use strict';
    var count = (GM_getValue('runs', 0) || 0) + 1;
    GM_setValue('runs', count);
    GM_addStyle('#eb-banner{position:fixed;top:0;left:0;right:0;z-index:99999;' +
        'background:#c0392b;color:#fff;font:bold 22px sans-serif;padding:18px;text-align:center;}');
    var b = document.createElement('div');
    b.id = 'eb-banner';
    b.textContent = 'USERSCRIPT RAN #' + count;
    document.body.appendChild(b);
    GM_registerMenuCommand('Turn page green', function () {
        document.body.style.background = '#27ae60';
        GM_notification('turned green');
    });
})();
