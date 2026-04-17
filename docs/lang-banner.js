(function () {
  'use strict';

  var DISMISS_KEY = 'einkbro-lang-banner-dismissed';

  var path = window.location.pathname;
  var hash = window.location.hash || '';
  var isZhTw = /\/zh-tw\//.test(path);
  var filename = path.split('/').pop() || 'index.html';
  if (!filename || filename.indexOf('.') === -1) filename = 'index.html';

  var enHref = (isZhTw ? '../' : '') + filename + hash;
  var zhHref = (isZhTw ? '' : 'zh-tw/') + filename + hash;

  var langs = (navigator.languages && navigator.languages.length
    ? navigator.languages
    : [navigator.language || navigator.userLanguage || '']).map(function (l) {
    return String(l).toLowerCase();
  });
  var prefersZh = langs.some(function (l) { return l.indexOf('zh') === 0; });

  var shouldSuggest = isZhTw ? !prefersZh : prefersZh;

  var dismissed = false;
  try { dismissed = window.localStorage.getItem(DISMISS_KEY) === '1'; } catch (e) {}

  var style = document.createElement('style');
  style.textContent = [
    '.eb-lang-banner{background:#111;color:#fff;font-family:inherit;font-size:0.9rem;',
    'padding:0.6rem 1.5rem;display:flex;justify-content:space-between;align-items:center;',
    'gap:1rem;border-bottom:1px solid #fff;}',
    '.eb-lang-banner a{color:#fff;text-decoration:underline;font-weight:700;}',
    '.eb-lang-banner__close{background:none;border:1px solid #fff;color:#fff;cursor:pointer;',
    'padding:2px 10px;font-size:0.9rem;line-height:1;margin-left:1rem;font-family:inherit;}',
    '.eb-lang-banner__close:hover{background:#fff;color:#111;}',
    '.site-nav__lang{display:flex;gap:0.4rem;align-items:center;margin-left:1rem;',
    'font-size:0.85rem;letter-spacing:0.03em;}',
    '.site-nav__lang a{color:rgba(255,255,255,0.55);text-decoration:none;font-weight:700;',
    'text-transform:uppercase;}',
    '.site-nav__lang a:hover{color:#fff;}',
    '.site-nav__lang a.current{color:#fff;}',
    '.site-nav__lang-sep{color:rgba(255,255,255,0.35);}'
  ].join('');
  document.head.appendChild(style);

  function injectNavSwitcher() {
    var navLinks = document.querySelector('.site-nav__links');
    if (!navLinks || navLinks.querySelector('.site-nav__lang')) return;
    var li = document.createElement('li');
    li.className = 'site-nav__lang';
    li.innerHTML =
      '<a href="' + enHref + '" class="' + (isZhTw ? '' : 'current') + '" hreflang="en">EN</a>' +
      '<span class="site-nav__lang-sep">|</span>' +
      '<a href="' + zhHref + '" class="' + (isZhTw ? 'current' : '') + '" hreflang="zh-Hant-TW" lang="zh-Hant-TW">\u4E2D\u6587</a>';
    navLinks.appendChild(li);
  }

  function injectSuggestBanner() {
    if (!shouldSuggest || dismissed) return;
    var body = document.body;
    if (!body) return;
    var banner = document.createElement('div');
    banner.className = 'eb-lang-banner';
    var linkText = isZhTw
      ? 'View English version \u2192'
      : '\u770B\u7E41\u9AD4\u4E2D\u6587\u7248 \u2192';
    var targetHref = isZhTw ? enHref : zhHref;
    var msgLang = isZhTw ? 'en' : 'zh-Hant-TW';
    banner.innerHTML =
      '<span lang="' + msgLang + '"><a href="' + targetHref + '">' + linkText + '</a></span>' +
      '<button type="button" class="eb-lang-banner__close" aria-label="Dismiss">\u00D7</button>';
    body.insertBefore(banner, body.firstChild);
    var closeBtn = banner.querySelector('.eb-lang-banner__close');
    closeBtn.addEventListener('click', function () {
      try { window.localStorage.setItem(DISMISS_KEY, '1'); } catch (e) {}
      banner.parentNode && banner.parentNode.removeChild(banner);
    });
  }

  function run() {
    injectNavSwitcher();
    injectSuggestBanner();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run);
  } else {
    run();
  }
})();
