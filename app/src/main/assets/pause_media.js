// Pauses every playing media element on the page. Used when a long-running AI
// request (Gemini video transcription) starts, so a live video decoder isn't kept
// around on a low-memory device for the whole wait.
(function () {
    document.querySelectorAll('video, audio').forEach(function (media) {
        if (!media.paused) media.pause();
    });
})();
