// Web Speech API (speechSynthesis) polyfill backed by the app's TTS engines.
// Android WebView ships no speech-synthesis backend, so pages that implement
// their own read-aloud (language-learning sites, articles with a "listen"
// button) are silent. This shim forwards utterances over the androidApp bridge
// (JsWebInterface.ttsSpeak/ttsCancel/ttsGetVoices) to WebSpeechHandler, which
// speaks them with the engine chosen in the app's TTS settings.
//
// Injected at document start (WebViewConfigApplier) so it wins the race against
// page scripts that capture window.speechSynthesis at load time.
//
// Simplifications vs the spec: boundary/mark events are never fired, and
// pause() only pauses the byte-audio engines (system TTS cannot pause).
// Utterance queueing is honored (JS-side queue, one native utterance at a
// time).
(function () {
    'use strict';
    if (window.__ebTts) return;
    if (typeof androidApp === 'undefined' || !androidApp.ttsSpeak) return;

    // Must match MAX_TTS_TEXT_LENGTH in JsWebInterface: longer texts would be
    // silently rejected by the bridge and stall the queue.
    var MAX_TEXT_LENGTH = 4000;

    function SpeechSynthesisUtterance(text) {
        this.text = text === undefined || text === null ? '' : String(text);
        this.lang = '';
        this.voice = null;
        this.volume = 1;
        this.rate = 1;
        this.pitch = 1;
        this.onstart = null;
        this.onend = null;
        this.onerror = null;
        this.onpause = null;
        this.onresume = null;
        this.onmark = null;
        this.onboundary = null;
        this._listeners = {};
    }
    SpeechSynthesisUtterance.prototype.addEventListener = function (type, fn) {
        if (!fn) return;
        (this._listeners[type] = this._listeners[type] || []).push(fn);
    };
    SpeechSynthesisUtterance.prototype.removeEventListener = function (type, fn) {
        var list = this._listeners[type];
        if (!list) return;
        var i = list.indexOf(fn);
        if (i >= 0) list.splice(i, 1);
    };

    function fire(utterance, type, extra) {
        var ev = { type: type, utterance: utterance, charIndex: 0, charLength: 0, elapsedTime: 0, name: '' };
        if (extra) for (var k in extra) ev[k] = extra[k];
        var handler = utterance['on' + type];
        if (typeof handler === 'function') {
            try { handler.call(utterance, ev); } catch (e) { console.error('einkbro tts ' + type + ' handler', e); }
        }
        var list = (utterance._listeners && utterance._listeners[type]) || [];
        for (var i = 0; i < list.length; i++) {
            try { list[i].call(utterance, ev); } catch (e) { console.error('einkbro tts ' + type + ' listener', e); }
        }
    }

    var voices = [];
    var voicesRequested = false;
    var voicesChangedListeners = [];

    function Voice(data) {
        this.voiceURI = data.name;
        this.name = data.name;
        this.lang = data.lang;
        this.localService = !!data.localService;
        this.default = !!data['default'];
    }

    function fireVoicesChanged() {
        var ev = { type: 'voiceschanged' };
        if (typeof synth.onvoiceschanged === 'function') {
            try { synth.onvoiceschanged(ev); } catch (e) { console.error('einkbro tts voiceschanged', e); }
        }
        for (var i = 0; i < voicesChangedListeners.length; i++) {
            try { voicesChangedListeners[i](ev); } catch (e) { console.error('einkbro tts voiceschanged', e); }
        }
    }

    // The native voice list may not be ready on the first call (system TTS
    // engine initializes asynchronously), so retry a few times and announce
    // late arrivals via voiceschanged — same contract as Chrome's async voices.
    function refreshVoices(attempt) {
        var list = [];
        try { list = JSON.parse(androidApp.ttsGetVoices() || '[]') || []; } catch (e) { list = []; }
        if (list.length) {
            voices = [];
            for (var i = 0; i < list.length; i++) voices.push(new Voice(list[i]));
            setTimeout(fireVoicesChanged, 0);
        } else if (attempt < 6) {
            setTimeout(function () { refreshVoices(attempt + 1); }, 500);
        }
    }

    var queue = [];      // {id, u} waiting to be spoken
    var current = null;  // {id, u} handed to the native side
    var counter = 0;

    function processQueue() {
        if (current || !queue.length) return;
        var next = queue.shift();
        synth.pending = queue.length > 0;
        var u = next.u;
        var text = String(u.text == null ? '' : u.text).slice(0, MAX_TEXT_LENGTH);
        if (!text.trim()) {
            // Spec: empty utterances still fire start/end.
            setTimeout(function () { fire(u, 'start'); fire(u, 'end'); processQueue(); }, 0);
            return;
        }
        current = next;
        var lang = u.lang || (u.voice && u.voice.lang) || '';
        var voiceName = (u.voice && u.voice.name) || '';
        try {
            androidApp.ttsSpeak(text, String(lang), Number(u.rate) || 1, Number(u.pitch) || 1, String(voiceName), next.id);
        } catch (e) {
            console.error('einkbro tts bridge', e);
            current = null;
            setTimeout(function () { fire(u, 'error', { error: 'synthesis-failed' }); processQueue(); }, 0);
        }
    }

    var synth = {
        speaking: false,
        pending: false,
        paused: false,
        onvoiceschanged: null,
        getVoices: function () {
            if (!voicesRequested) {
                voicesRequested = true;
                refreshVoices(0);
            }
            return voices.slice();
        },
        speak: function (u) {
            if (!u || typeof u.text === 'undefined') return;
            queue.push({ id: String(++counter), u: u });
            synth.pending = true;
            processQueue();
        },
        cancel: function () {
            var interrupted = current;
            var dropped = queue;
            queue = [];
            current = null;
            synth.speaking = false;
            synth.pending = false;
            synth.paused = false;
            try { androidApp.ttsCancel(); } catch (e) {}
            // Spec: removed utterances get an error event — 'interrupted' for
            // the one being spoken, 'canceled' for the still-queued ones.
            // Fired synchronously, before cancel() returns: the common
            // cancel-then-speak pattern reuses one cleanup handler for
            // onerror, and deferring these events would make that handler
            // fire after the next utterance's UI state is already set up.
            if (interrupted) fire(interrupted.u, 'error', { error: 'interrupted' });
            for (var i = 0; i < dropped.length; i++) fire(dropped[i].u, 'error', { error: 'canceled' });
        },
        // Pauses byte-engine playback natively; the system TTS engine cannot
        // pause and keeps speaking, but the paused flag still toggles so the
        // common pause/resume button pattern doesn't wedge.
        pause: function () {
            if (!current || synth.paused) return;
            synth.paused = true;
            try { androidApp.ttsPause(); } catch (e) {}
            fire(current.u, 'pause');
        },
        resume: function () {
            if (!synth.paused) return;
            synth.paused = false;
            try { androidApp.ttsResume(); } catch (e) {}
            if (current) fire(current.u, 'resume');
        },
        addEventListener: function (type, fn) {
            if (type === 'voiceschanged' && fn) voicesChangedListeners.push(fn);
        },
        removeEventListener: function (type, fn) {
            var i = voicesChangedListeners.indexOf(fn);
            if (type === 'voiceschanged' && i >= 0) voicesChangedListeners.splice(i, 1);
        }
    };

    window.__ebTts = {
        // Called from native (WebSpeechHandler.dispatchEvent). Events for
        // utterances that were cancelled meanwhile are stale — drop them.
        dispatch: function (id, type) {
            if (!current || current.id !== id) return;
            var u = current.u;
            if (type === 'start') {
                synth.speaking = true;
                fire(u, 'start');
            } else if (type === 'end') {
                current = null;
                synth.speaking = false;
                synth.paused = false;
                fire(u, 'end');
                processQueue();
            } else if (type === 'error') {
                current = null;
                synth.speaking = false;
                synth.paused = false;
                fire(u, 'error', { error: 'synthesis-failed' });
                processQueue();
            }
        }
    };

    // WebView may expose a non-functional native speechSynthesis (empty voice
    // list, speak() does nothing), so always replace it.
    try {
        Object.defineProperty(window, 'speechSynthesis', { value: synth, configurable: true, writable: true });
    } catch (e) {
        window.speechSynthesis = synth;
    }
    window.SpeechSynthesisUtterance = SpeechSynthesisUtterance;
})();
