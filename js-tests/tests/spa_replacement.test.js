/**
 * SPA element replacement: between firing a translation request and receiving the callback,
 * the framework swaps the marker element. myCallback's getElementById lookup fails; the
 * fallback should match on normalized text and apply translation to the replacement.
 */
const { loadTranslatePipeline } = require('../helpers/loadScripts');

beforeEach(() => __resetTranslateBridge());

describe('SPA element replacement', () => {
  test('callback recovers via normalized-text match when the original ID is gone', () => {
    document.body.innerHTML = `<article><p>안녕하세요</p></article>`;
    loadTranslatePipeline({ inPlace: true });

    __forceVisible(document);
    __triggerAllIntersections();
    expect(__translateRequests).toHaveLength(1);

    // SPA destroys the element and creates a new one with the same text but no ID.
    const article = document.querySelector('article');
    article.innerHTML = `<p class="to-translate">안녕하세요</p>`;

    // Resolve — original ID no longer exists.
    __resolveTranslation('Hello');

    const p = document.querySelector('article > p');
    expect(p.textContent.trim()).toBe('Hello');
    expect(p.getAttribute('data-original-html')).toBe('안녕하세요');
  });

  test('text cache lets future re-renders apply instantly without round-tripping', () => {
    document.body.innerHTML = `<p>안녕</p>`;
    loadTranslatePipeline({ inPlace: true });

    __forceVisible(document);
    __triggerAllIntersections();
    __resolveTranslation('Hi');

    // New element with the same text appears later (e.g., infinite scroll).
    const fresh = document.createElement('p');
    fresh.textContent = '안녕';
    document.body.appendChild(fresh);

    return new Promise((resolve) => {
      setTimeout(() => {
        __forceVisible(document);
        if (typeof window._translateRebindObserver === 'function') {
          window._translateRebindObserver();
        }

        // Should NOT have fired a new native call — cache should have been used.
        const requestsForHi = __translateRequests.filter((r) => r.text.trim() === '안녕');
        expect(requestsForHi.length).toBeLessThanOrEqual(1);

        // The fresh element should already show the translated text.
        const ps = Array.from(document.querySelectorAll('p')).map((p) => p.textContent.trim());
        expect(ps.filter((t) => t === 'Hi').length).toBeGreaterThanOrEqual(2);
        resolve();
      }, 350);
    });
  });
});
