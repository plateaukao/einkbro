(function () {
    var existing = document.getElementById('_einkbro_pdf_print');
    if (existing) existing.remove();
    var style = document.createElement('style');
    style.id = '_einkbro_pdf_print';
    style.textContent =
        '@media print {' +
        '  html { zoom: __ZOOM__ !important; }' +
        '  body { background: #fff !important; color: #000 !important; }' +
        '  * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }' +
        '}';
    document.head.appendChild(style);
})();
