// WebView 131+ starts a system drag when an image (or link) is long-pressed,
// and on Android 8.x releasing that drag freezes the whole device until a
// forced reboot (issue #629; chromium android-webview-dev "Severe freeze bug -
// drag image in WebView freezes entire UI"). Blink always fires a cancelable
// dragstart before starting any drag, so cancelling it here stops chromium
// from ever opening the system drag session. Installed only on Android 8.x.
window.addEventListener('dragstart', function (e) {
    e.preventDefault();
}, true);
