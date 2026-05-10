(function () {
    if (window.__einkbroBlobHookInstalled) return;
    window.__einkbroBlobHookInstalled = true;
    window.__einkbroBlobRegistry = window.__einkbroBlobRegistry || new Map();

    const originalCreateObjectURL = URL.createObjectURL.bind(URL);
    URL.createObjectURL = function (blob) {
        const url = originalCreateObjectURL(blob);
        try { window.__einkbroBlobRegistry.set(url, blob); } catch (e) {}
        return url;
    };

    const originalRevokeObjectURL = URL.revokeObjectURL.bind(URL);
    URL.revokeObjectURL = function (url) {
        try { window.__einkbroBlobRegistry.delete(url); } catch (e) {}
        return originalRevokeObjectURL(url);
    };

    const CHUNK_SIZE = 50000;

    function streamBlobToAndroid(blob, fileName) {
        const reader = new FileReader();
        const mimeType = blob.type || 'application/octet-stream';
        const downloadId = androidApp.beginBlobDownload(fileName || 'download', mimeType);
        if (!downloadId) return;
        reader.onerror = function () {
            androidApp.onBlobDownloadError(downloadId, 'read_failed');
        };
        reader.onloadend = function () {
            try {
                const result = typeof reader.result === 'string' ? reader.result : '';
                const base64 = result.substring(result.indexOf(',') + 1);
                for (let i = 0; i < base64.length; i += CHUNK_SIZE) {
                    androidApp.onBlobDownloadChunk(downloadId, base64.substring(i, i + CHUNK_SIZE));
                }
                androidApp.onBlobDownloadComplete(downloadId, mimeType);
            } catch (error) {
                androidApp.onBlobDownloadError(downloadId, String(error));
            }
        };
        reader.readAsDataURL(blob);
    }

    document.addEventListener('click', function (event) {
        const anchor = event.target && event.target.closest
            ? event.target.closest('a[href^="blob:"]')
            : null;
        if (!anchor) return;
        const blob = window.__einkbroBlobRegistry.get(anchor.href);
        if (!blob) return;
        event.preventDefault();
        event.stopPropagation();
        event.stopImmediatePropagation();
        streamBlobToAndroid(blob, anchor.download || '');
    }, true);
})();
