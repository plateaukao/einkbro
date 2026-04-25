# js-tests

Jest + jsdom suite for the translation-pipeline JavaScript that lives in
`app/src/main/assets/`.

The translation logic is a moving target — multiple call paths, async callbacks across the
JS/native bridge, fragile interaction with site-specific SPA behaviour. Verifying every
mode by hand on the emulator is slow and error-prone, so these tests stub the WebView
environment (`androidApp.getTranslation`, `IntersectionObserver`, layout rects) and exercise
the JS directly.

## Run

```bash
cd js-tests
npm install   # one-time
npm test
```

Suite runs in <2 seconds.

## What's covered

| File | What it pins down |
| --- | --- |
| `in_place.test.js` | Marker injection, callback applies translation in place, JS-side text cache reuses translations |
| `by_paragraph.test.js` | Sibling `<p>` insertion, translation goes into the sibling, the translated `<p>` is **not** re-marked (regression guard for the recursion bug) |
| `idempotency.test.js` | Re-injecting `translate_by_paragraph.js` doesn't toggle/wipe in-flight translations; stale-state recovery when the page replaced its own innerHTML |
| `spa_replacement.test.js` | When the framework swaps the marker between request and response, `myCallback`'s text-match fallback applies the translation to the replacement |
| `lazy_hydration.test.js` | The `MutationObserver` picks up content rendered after the script first ran, and newly-marked elements get translated |

## What's NOT covered

- The Kotlin bridge (`JsWebInterface.kt`, `WebContentPostProcessor.kt`) — would need
  Robolectric or instrumented tests.
- Real OpenAI / Gemini / DeepL / Papago API calls — irrelevant to the JS pipeline.
- `addGoogleTranslation` (Google in-place widget) — third-party, opaque to us.
- Papago screen translation — image-based path, doesn't go through these scripts.

## Adding tests

Use `__resetTranslateBridge()` in `beforeEach` to reset window state and clean up old
`IntersectionObserver` instances. Helpers exposed by `helpers/setup.js`:

- `__translateRequests` — array of `{ text, elementId, callbackName }` captured from
  `androidApp.getTranslation` calls.
- `__resolveTranslation(translation, index?)` — fires `myCallback(elementId, originalText, translation)`
  the way `JsWebInterface.kt` does, against the most recent (or `index`-th) pending request.
- `__triggerAllIntersections()` — fires the IntersectionObserver callback for every
  observed marker (since jsdom doesn't actually do layout/intersection).
- `__forceVisible(root?)` — overrides `getBoundingClientRect` so visibility-gated code
  considers all `.to-translate` markers on screen.

`helpers/loadScripts.js` reads the production JS files from
`app/src/main/assets/`, so the tests always exercise the exact code that ships.
