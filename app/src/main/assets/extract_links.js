(function () {
  var out = [];
  var seen = {};
  var anchors = document.querySelectorAll('a[href]');
  for (var i = 0; i < anchors.length; i++) {
    var a = anchors[i];
    var href = a.href;
    if (!href || href.indexOf('javascript:') === 0) continue;
    var text = (a.innerText || a.textContent || '').replace(/\s+/g, ' ').trim();
    if (!text) continue;
    var key = href + '||' + text;
    if (seen[key]) continue;
    seen[key] = true;
    out.push({ text: text, href: href });
  }
  return out;
})();
