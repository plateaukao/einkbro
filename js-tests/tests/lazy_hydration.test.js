/**
 * Lazy hydration: content arrives after the script first runs (the daum.net case where
 * onPageFinished fires when the page is nearly empty, then content streams in). The
 * MutationObserver in translate_by_paragraph.js must mark new content as it appears.
 */
const { loadTranslatePipeline } = require('../helpers/loadScripts');

beforeEach(() => __resetTranslateBridge());

describe('lazy hydration', () => {
  test('MutationObserver marks content added after first injection', async () => {
    document.body.innerHTML = `<p>처음</p>`;
    loadTranslatePipeline({ inPlace: true });
    expect(document.querySelectorAll('.to-translate')).toHaveLength(1);

    // Simulate hydration adding more articles a moment later.
    const article = document.createElement('article');
    article.innerHTML = `<p>두번째</p><p>세번째</p>`;
    document.body.appendChild(article);

    await new Promise((r) => setTimeout(r, 350)); // wait for 300ms debounce
    expect(document.querySelectorAll('.to-translate')).toHaveLength(3);
  });

  test('newly-marked elements get observed and an initial visibility check fires', async () => {
    document.body.innerHTML = `<p>처음</p>`;
    loadTranslatePipeline({ inPlace: true });

    const article = document.createElement('article');
    article.innerHTML = `<p>새로운 글</p>`;
    document.body.appendChild(article);

    await new Promise((r) => setTimeout(r, 350));
    __forceVisible(document); // make the new marker pass the visibility gate
    if (typeof window._translateRebindObserver === 'function') {
      window._translateRebindObserver();
    }

    const newPRequest = __translateRequests.find((r) => r.text.trim() === '새로운 글');
    expect(newPRequest).toBeDefined();
  });
});
