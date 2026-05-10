// CSP-restricted sites (e.g. github.com) will block fetch on blob: URLs and
// fall through to the error path. The capture-phase click hook in
// blob_download_hook.js handles those cases first by reading the in-memory
// Blob object directly, so this fallback only fires for blob URLs that
// reach onDownloadStart without ever appearing in the page DOM.
(function (blobUrl, fallbackMimeType, downloadId) {
    const CHUNK_SIZE = 50000;
    fetch(blobUrl).then(function (response) {
        return response.blob();
    }).then(function (blob) {
        return new Promise(function (resolve, reject) {
            const reader = new FileReader();
            reader.onerror = function () { reject(reader.error); };
            reader.onloadend = function () {
                const result = typeof reader.result === 'string' ? reader.result : '';
                const base64 = result.substring(result.indexOf(',') + 1);
                for (let i = 0; i < base64.length; i += CHUNK_SIZE) {
                    androidApp.onBlobDownloadChunk(downloadId, base64.substring(i, i + CHUNK_SIZE));
                }
                androidApp.onBlobDownloadComplete(
                    downloadId,
                    blob.type || fallbackMimeType || 'application/octet-stream'
                );
                resolve();
            };
            reader.readAsDataURL(blob);
        });
    }).catch(function (error) {
        androidApp.onBlobDownloadError(downloadId, String(error));
    });
})(__BLOB_URL__, __MIME_TYPE__, __DOWNLOAD_ID__);
