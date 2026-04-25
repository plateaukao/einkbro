/** By-paragraph mode (window._translateInPlace is unset). */
const { loadTranslatePipeline } = require('../helpers/loadScripts');

beforeEach(() => __resetTranslateBridge());

describe('by-paragraph translation', () => {
  test('injects an empty <p> sibling next to each marker', () => {
    document.body.innerHTML = `<article><p>안녕</p><p>세계</p></article>`;
    loadTranslatePipeline({ inPlace: false });

    const markers = document.querySelectorAll('.to-translate');
    expect(markers).toHaveLength(2);
    markers.forEach((m) => {
      const next = m.nextElementSibling;
      expect(next).not.toBeNull();
      expect(next.tagName).toBe('P');
      expect(next.textContent).toBe('');
    });
  });

  test('callback fills the sibling <p> and tags it with class "translated"', () => {
    document.body.innerHTML = `<article><p>안녕</p></article>`;
    loadTranslatePipeline({ inPlace: false });

    __forceVisible(document);
    __triggerAllIntersections();

    expect(__translateRequests).toHaveLength(1);
    __resolveTranslation('Hello');

    const sibling = document.querySelector('.to-translate').nextElementSibling;
    expect(sibling.tagName).toBe('P');
    expect(sibling.textContent).toBe('Hello');
    expect(sibling.classList.contains('translated')).toBe(true);
  });

  test('translated <p> sibling is NOT re-marked (no recursive translation of translation)', async () => {
    document.body.innerHTML = `<article><p>안녕</p></article>`;
    loadTranslatePipeline({ inPlace: false });

    __forceVisible(document);
    __triggerAllIntersections();
    __resolveTranslation('Hello');

    // Wait for MutationObserver debounce; if the fix is broken, fetchNodesWithText would
    // re-mark the translated sibling, queueing another translation request for "Hello".
    await new Promise((r) => setTimeout(r, 400));

    const markers = document.querySelectorAll('.to-translate');
    expect(markers).toHaveLength(1); // still only the original Korean paragraph

    // No follow-up translation request for the English text.
    const englishRequests = __translateRequests.filter((r) => /Hello/.test(r.text));
    expect(englishRequests).toHaveLength(0);
  });
});
