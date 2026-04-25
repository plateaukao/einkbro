/**
 * Stubs that mirror the WebView environment for the translation JS.
 *
 * - androidApp: the @JavascriptInterface bridge from JsWebInterface.kt. We capture each
 *   getTranslation call so tests can assert on text/elementId, and we expose a helper
 *   `__resolveTranslation(text, translation)` that fires the callback the same way the
 *   native side does (with the `originalText, translation` payload added in our recent fix).
 *
 * - IntersectionObserver: jsdom doesn't ship one. We use a controllable stub that fires
 *   immediately when `observe()` is called and the element has non-zero size & a viewport
 *   intersection. Tests can also call `__triggerIntersection(node)` to force-fire.
 *
 * Reset between tests via `__resetTranslateBridge()`.
 */

global.__translateRequests = [];

global.__resetTranslateBridge = function () {
  global.__translateRequests.length = 0;
  // Fresh window-level state for each test
  for (const k of Object.keys(window)) {
    if (k.startsWith('_translate') || k === 'androidApp') delete window[k];
  }
  // Tear down any IntersectionObserver instances that the previous test created — their
  // `targets` Set holds references to detached DOM nodes; leaving them around causes
  // __triggerAllIntersections to fire callbacks against stale elements.
  if (global.IntersectionObserver && global.IntersectionObserver.instances) {
    for (const inst of global.IntersectionObserver.instances) inst.disconnect();
    global.IntersectionObserver.instances.length = 0;
  }
  // Same story for myCallback — defined fresh each pipeline load.
  delete window.myCallback;
  delete document.originalInnerHTML;
  delete document.translatedInnerHTML;
  document.body.className = '';
  document.body.innerHTML = '';

  // androidApp bridge
  window.androidApp = {
    getTranslation(text, elementId, callbackName) {
      global.__translateRequests.push({ text, elementId, callbackName });
    },
  };
};

// Resolve a pending translation call by index (the most recent one by default).
// Mirrors the JsWebInterface call: callback(elementId, originalText, translation).
global.__resolveTranslation = function (translation, index) {
  const i = typeof index === 'number' ? index : global.__translateRequests.length - 1;
  const req = global.__translateRequests[i];
  if (!req) throw new Error('No pending translation request at index ' + i);
  const cb = window[req.callbackName];
  if (typeof cb !== 'function') throw new Error('Callback ' + req.callbackName + ' not defined on window');
  cb(req.elementId, req.text, translation);
};

// Synchronous, controllable IntersectionObserver stub.
class StubIntersectionObserver {
  constructor(callback) {
    this.callback = callback;
    this.targets = new Set();
    StubIntersectionObserver.instances.push(this);
  }
  observe(target) {
    this.targets.add(target);
    // Fire if currently in viewport per its bounding rect (which jsdom defaults to 0x0;
    // tests force an intersection by calling __triggerIntersection).
  }
  unobserve(target) {
    this.targets.delete(target);
  }
  disconnect() {
    this.targets.clear();
  }
  // Test hook: trigger an intersection event for `target`.
  __fire(target, isIntersecting = true) {
    if (!this.targets.has(target)) return;
    this.callback([{ isIntersecting, target }]);
  }
}
StubIntersectionObserver.instances = [];
global.IntersectionObserver = StubIntersectionObserver;
window.IntersectionObserver = StubIntersectionObserver;

// Force-fire intersection for a node across all instances.
global.__triggerIntersection = function (node) {
  for (const inst of StubIntersectionObserver.instances) inst.__fire(node, true);
};

// Force every observed `.to-translate` to be considered intersecting.
global.__triggerAllIntersections = function () {
  for (const inst of StubIntersectionObserver.instances) {
    for (const t of inst.targets) inst.__fire(t, true);
  }
};

// Force boundingClientRect to look on-screen for `maybeRequestTranslation`'s visibility check.
global.__forceVisible = function (root) {
  const rect = { top: 100, bottom: 200, left: 0, right: 100, width: 100, height: 100, x: 0, y: 100 };
  (root || document).querySelectorAll('.to-translate').forEach((el) => {
    el.getBoundingClientRect = () => ({ ...rect, toJSON: () => rect });
  });
};

// jsdom defines window.innerHeight; ensure it's a sane value for visibility math.
Object.defineProperty(window, 'innerHeight', { configurable: true, value: 800 });
Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1200 });
