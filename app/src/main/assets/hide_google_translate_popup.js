/* Google's translate element widget shows an "Original text" balloon (#goog-gt-tt)
   whenever a translated sentence is tapped, which hijacks touch-scrolling and text
   selection on e-ink devices. Hide it and the tap highlight; CSS applies even though
   the balloon element is created later by the async widget. */
if (!document.getElementById('einkbro-hide-gt-popup')) {
    var style = document.createElement('style');
    style.id = 'einkbro-hide-gt-popup';
    style.textContent =
        '#goog-gt-tt, .goog-te-balloon-frame { display: none !important; pointer-events: none !important; }' +
        '.goog-text-highlight, .VIpgJd-yAWNEb-VIpgJd-fmcmS-sn54Q, .VIpgJd-yAWNEb-VIpgJd-fmcmS-OWXEXe-ja0Pqe { background: none !important; box-shadow: none !important; }';
    (document.head || document.documentElement).appendChild(style);
}
