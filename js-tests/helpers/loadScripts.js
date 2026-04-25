/**
 * Loads the production JS files into the current jsdom document by reading them from
 * `app/src/main/assets/` and `eval`-ing in the global context.
 *
 * The Android side wraps `translate_by_paragraph.js` in an IIFE (`wrapJsFunction()` in
 * WebViewJsBridge.kt) so its function declarations stay local. For tests we want direct
 * access to the helpers, so we DON'T wrap. The bootstrap IIFE at the bottom of the file
 * still runs the same way.
 *
 * `text_node_monitor.js` is loaded with `withPrefix=false` in production, i.e. as a plain
 * script — same here.
 */
const fs = require('fs');
const path = require('path');

const ASSETS = path.resolve(__dirname, '../../app/src/main/assets');

function loadAsset(name) {
  return fs.readFileSync(path.join(ASSETS, name), 'utf8');
}

function evalInWindow(code) {
  // `(0, eval)` runs in indirect mode = global scope, which gives function declarations
  // global visibility (matching how Android WebView treats unwrapped scripts).
  // eslint-disable-next-line no-eval
  return (0, eval)(code);
}

function loadTranslatePipeline({ inPlace }) {
  // Set the in-place flag the way `translateByParagraphInPlaceReplace()` does in
  // WebViewJsBridge.kt (it `evaluateJavascript("window._translateInPlace = true;")` first).
  if (inPlace) {
    window._translateInPlace = true;
  } else {
    delete window._translateInPlace;
  }
  evalInWindow(loadAsset('translate_by_paragraph.js'));
  evalInWindow(loadAsset('text_node_monitor.js'));
}

// Re-runs translate_by_paragraph.js (as if Android fired showTranslation again).
function rerunTranslateByParagraph() {
  evalInWindow(loadAsset('translate_by_paragraph.js'));
}

module.exports = { loadTranslatePipeline, rerunTranslateByParagraph, loadAsset };
