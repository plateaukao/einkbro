// ==UserScript==
// @name         Menu Command Test
// @namespace    einkbro-test
// @version      1.0
// @description  Registers toolbar menu commands for testing
// @match        http://localhost:8000/*
// @grant        GM_registerMenuCommand
// @grant        GM_notification
// ==/UserScript==

(function () {
    'use strict';
    GM_registerMenuCommand('Turn page red', function () {
        document.body.style.background = 'red';
        GM_notification('Page turned red');
    });
    GM_registerMenuCommand('Show greeting', function () {
        GM_notification('Hello from userscript menu command!');
    });
})();
