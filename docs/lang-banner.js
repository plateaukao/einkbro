(function () {
  'use strict';

  var path = window.location.pathname;
  var hash = window.location.hash || '';
  var isZhTw = /\/zh-tw\//.test(path);
  var filename = path.split('/').pop() || 'index.html';
  if (!filename || filename.indexOf('.') === -1) filename = 'index.html';

  var enHref = (isZhTw ? '../' : '') + filename + hash;
  var zhHref = (isZhTw ? '' : 'zh-tw/') + filename + hash;

  var style = document.createElement('style');
  style.textContent = [
    '.site-nav__lang{display:flex;gap:0.45rem;align-items:center;margin-left:auto;',
    'margin-right:1.2rem;font-size:0.85rem;letter-spacing:0.03em;white-space:nowrap;}',
    '.site-nav__lang a{color:rgba(255,255,255,0.55);text-decoration:none;font-weight:700;',
    'text-transform:uppercase;}',
    '.site-nav__lang a:hover{color:#fff;}',
    '.site-nav__lang a.current{color:#fff;}',
    '.site-nav__lang-sep{color:rgba(255,255,255,0.35);}',
    '@media (min-width:769px){',
    '.site-nav__lang{order:2;margin-left:1.6rem;margin-right:0;}',
    '.site-nav__links{margin-left:auto;}',
    '}'
  ].join('');
  document.head.appendChild(style);

  function injectNavSwitcher() {
    var container = document.querySelector('.site-nav .container');
    if (!container || container.querySelector('.site-nav__lang')) return;
    var el = document.createElement('div');
    el.className = 'site-nav__lang';
    el.innerHTML =
      '<a href="' + enHref + '" class="' + (isZhTw ? '' : 'current') + '" hreflang="en">EN</a>' +
      '<span class="site-nav__lang-sep">|</span>' +
      '<a href="' + zhHref + '" class="' + (isZhTw ? 'current' : '') + '" hreflang="zh-Hant-TW" lang="zh-Hant-TW">中文</a>';
    var toggle = container.querySelector('.nav-toggle');
    if (toggle) container.insertBefore(el, toggle);
    else container.appendChild(el);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', injectNavSwitcher);
  } else {
    injectNavSwitcher();
  }
})();
