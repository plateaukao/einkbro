/**
 * Idempotency: re-running translate_by_paragraph.js (which mirrors what happens when the
 * same site fires onPageFinished multiple times, e.g. news.daum.net's hash-and-reload chain)
 * must not corrupt state — the previous toggle behaviour wiped pending translations.
 */
const { loadTranslatePipeline, rerunTranslateByParagraph } = require('../helpers/loadScripts');

beforeEach(() => __resetTranslateBridge());

describe('idempotency', () => {
  test('second injection on the same DOM is a no-op (no toggle)', () => {
    document.body.innerHTML = `<p>안녕</p>`;
    loadTranslatePipeline({ inPlace: true });
    expect(document.querySelectorAll('.to-translate')).toHaveLength(1);

    rerunTranslateByParagraph();
    expect(document.querySelectorAll('.to-translate')).toHaveLength(1);
    expect(document.body.classList.contains('translated_but_hide')).toBe(false);
  });

  test('async translation in flight survives a second injection', () => {
    document.body.innerHTML = `<p>안녕</p>`;
    loadTranslatePipeline({ inPlace: true });
    __forceVisible(document);
    __triggerAllIntersections();
    expect(__translateRequests).toHaveLength(1);

    rerunTranslateByParagraph();

    // Resolve the original request — element should still exist, callback should apply.
    __resolveTranslation('Hello');
    const p = document.querySelector('p.to-translate');
    expect(p.textContent.trim()).toBe('Hello');
    expect(p.getAttribute('data-original-html')).toBe('안녕');
  });

  test('stale state recovery: body has class "translated" but no markers (page wiped innerHTML)', () => {
    document.body.innerHTML = `<p>안녕</p>`;
    loadTranslatePipeline({ inPlace: true });
    expect(document.querySelectorAll('.to-translate')).toHaveLength(1);

    // Simulate the SPA replacing its own content after our markers were attached.
    document.body.innerHTML = `<p>새로운 내용</p>`;
    expect(document.body.classList.contains('translated')).toBe(true);
    expect(document.querySelectorAll('.to-translate')).toHaveLength(0);

    rerunTranslateByParagraph();
    // The script should detect the stale state and re-mark from scratch.
    expect(document.querySelectorAll('.to-translate')).toHaveLength(1);
    expect(document.querySelector('.to-translate').textContent.trim()).toBe('새로운 내용');
  });
});
