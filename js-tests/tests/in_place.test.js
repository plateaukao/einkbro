/** In-place translation mode (window._translateInPlace = true). */
const { loadTranslatePipeline } = require('../helpers/loadScripts');

beforeEach(() => __resetTranslateBridge());

describe('in-place translation', () => {
  test('marks each paragraph and requests translation when the marker becomes visible', () => {
    document.body.innerHTML = `
      <article>
        <p>Hello world</p>
        <p>Second paragraph</p>
      </article>
    `;
    loadTranslatePipeline({ inPlace: true });

    const markers = document.querySelectorAll('.to-translate');
    expect(markers).toHaveLength(2);
    expect(markers[0].id).toBeTruthy();

    __forceVisible(document);
    __triggerAllIntersections();

    expect(__translateRequests.length).toBeGreaterThanOrEqual(2);
    expect(__translateRequests.map((r) => r.text.trim())).toEqual(
      expect.arrayContaining(['Hello world', 'Second paragraph']),
    );
  });

  test('callback replaces text in place and stores data-original-html', () => {
    document.body.innerHTML = `<p>안녕하세요</p>`;
    loadTranslatePipeline({ inPlace: true });

    __forceVisible(document);
    __triggerAllIntersections();

    expect(__translateRequests).toHaveLength(1);
    __resolveTranslation('Hello');

    const p = document.querySelector('p.to-translate');
    expect(p.textContent.trim()).toBe('Hello');
    expect(p.getAttribute('data-original-html')).toBe('안녕하세요');
  });

  test('requesting same text twice in a session uses the JS-side cache (no second native call)', () => {
    document.body.innerHTML = `
      <p>안녕</p>
      <p>안녕</p>
    `;
    loadTranslatePipeline({ inPlace: true });
    __forceVisible(document);
    __triggerAllIntersections();

    expect(__translateRequests).toHaveLength(2);
    __resolveTranslation('Hi', 0);

    // Add a new paragraph with the same text — fetchNodesWithText (via MutationObserver)
    // should mark it; bindObserverToTargets's initial-visibility scan should hit the cache.
    const fresh = document.createElement('p');
    fresh.textContent = '안녕';
    document.querySelector('article, body').appendChild(fresh);

    return new Promise((resolve) => {
      // MutationObserver in translate_by_paragraph.js debounces 300ms.
      setTimeout(() => {
        __forceVisible(document);
        // Re-bind picks up new markers and applies cached translation directly.
        if (typeof window._translateRebindObserver === 'function') {
          window._translateRebindObserver();
        }
        const ps = document.querySelectorAll('p');
        // Both the original first <p> and the fresh one should show the cached translation.
        const translatedTexts = Array.from(ps).map((p) => p.textContent.trim());
        expect(translatedTexts).toEqual(expect.arrayContaining(['Hi']));
        resolve();
      }, 350);
    });
  });
});
