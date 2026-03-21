# EinkBro: Android Browser for E-Ink Devices

EinkBro is designed to fit E-Ink devices' needs; no unnecessary UI transitions and animations, clear B&W icons, useful features for an optimal e-ink reading experience. It's originated from [FOSS Browser](https://codeberg.org/Gaukler_Faun/FOSS_Browser), which is fully free/libre (as in freedom) Android app.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="60">](https://f-droid.org/packages/info.plateaukao.einkbro/)

[<img src="https://badgen.net/github/release/plateaukao/einkbro">](https://github.com/plateaukao/einkbro/releases)   [<img src="https://badgen.net/badge/download/snapshot_zip/green">](https://nightly.link/plateaukao/einkbro/workflows/buid-app-workflow.yaml/main/app-arm64-v8a-release.apk.zip)

## EinkBro has a book about how it was developed!
[<img width=200 src="https://github.com/user-attachments/assets/19c07714-a093-4960-b33e-bebb2ecf4501">](https://play.google.com/store/books/details/%E9%AB%98%E8%8C%82%E5%8E%9F_Daniel_Kao_Android%E9%96%8B%E6%BA%90%E5%B0%88%E6%A1%88_%E7%9C%9F_%E5%AF%A6%E6%88%B0%E5%95%9F%E8%88%AA?id=aOniEAAAQBAJ)

## Screenshots

|Main Screen|Menu Items|Translation|
|----|----|----|
|<img src="screenshots/01_main_browsing.png" width="200"/>|<img src="screenshots/02_menu_panel.png" width="200"/>|<img src="screenshots/03_translate.png" width="200"/>|
|Toolbar Configuration|Touch Setting|Vertical Read|
|<img src="screenshots/06_toolbar_config.png" width="200"/>|<img src="screenshots/07_touch_setting.png" width="200"/>|<img src="screenshots/09_vertical_read.png" width="200"/>|
|Settings|Font Size|Chat with Web|
|<img src="screenshots/08_settings.png" width="200"/>|<img src="screenshots/10_font_size.png" width="200"/>|<img src="screenshots/04_chat_with_web.png" width="200"/>|
|Bookmarks| | |
|<img src="screenshots/05_bookmarks.png" width="200"/>| | |

---

## E-Ink Optimized Reading

EinkBro is built from the ground up for E-Ink displays, with features that make web browsing comfortable on e-readers.

- **Tap screen edges to turn pages** — configurable left/right or top/bottom touch areas for page up/down
- **Volume key navigation** — use physical volume keys to scroll pages
- **Reader mode** — strips away clutter for a clean, distraction-free reading experience
- **Vertical reading mode** — vertical text layout for Chinese/Japanese content
- **Custom fonts** — load local font files; separate font settings for reader mode
- **Bold font toggle** — improve readability on low-contrast E-Ink screens
- **High contrast UI** — all icons and controls designed for E-Ink visibility
- **No animations** — zero unnecessary transitions or dimming effects

## AI Integration

Chat with web pages, summarize content, and run custom AI actions — all within the browser.

- **Chat with Web** — ask questions about the current page content using AI (supports split-screen and new tab modes)
- **Page AI** — run whole-page AI actions with customizable prompts
- **Multiple AI providers** — OpenAI, Google Gemini, Ollama, and any OpenAI-compatible server
- **Custom GPT actions** — define reusable actions with system prompts for text selection or full page context
- **TTS (Text-to-Speech)** — read content aloud using OpenAI TTS, ReadAloud, or system TTS

## Translation

Translate web content without leaving the browser, with multiple translation methods and providers.

- **Paragraph-by-paragraph translation** — inline translated text alongside the original for easy comparison
- **Multiple providers** — Google Translate, DeepL, Papago, OpenAI, and Google Gemini
- **Full page translation** — translate the entire page via Google Translate
- **Image translation** — translate text within images with cached results
- **Dual captions** — display subtitles in two languages on YouTube videos

## Save & Export

Multiple ways to save and share web content for offline reading.

- **Export to EPUB** — save web content as EPUB files with images and table of contents
- **Save as PDF** — configurable paper sizes for PDF export
- **Save as MHT** — archive pages while preserving original layout
- **Full-page screenshots** — capture entire web pages as images
- **Instapaper integration** — save articles directly to Instapaper
- **Share & copy links** — share content to other apps or clipboard

## Customizable UI

Tailor the browser's interface to your workflow with extensive customization options.

- **Configurable toolbar** — choose from 40+ action icons; drag to reorder
- **Toolbar position** — place toolbar at top or bottom; separate configs for portrait/landscape
- **Floating navigation button** — gesture-controlled button in fullscreen mode (swipe up/down/left/right)
- **Tab management** — unlimited tabs with tab bar, preview, and background loading
- **Bookmarks** — grid or list view with folder support and drag-to-reorder
- **Split screen** — side-by-side browsing with AI chat or search results
- **Quick toggle** — fast access to frequently used settings

## Browsing & Privacy

Standard browser features with privacy controls and E-Ink-friendly optimizations.

- **Ad blocking** — built-in ad blocker with customizable filter lists and auto-update
- **Multiple search engines** — Google, DuckDuckGo, Startpage, Bing, and more with search suggestions
- **JavaScript/cookie whitelists** — fine-grained control over per-site permissions
- **Tracking parameter pruning** — automatically strips utm_* and other tracking query parameters
- **Incognito mode** — private browsing with configurable clear-on-exit options
- **Desktop mode** — toggle desktop user agent per site
- **Search on page** — find text within the current page
- **VI key bindings** — keyboard shortcuts for power users
- **Highlight text** — mark and manage text highlights on web pages

## Development environment supported by JetBrains
[<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA_icon.png"
     alt="IDE supported by JetBrains"
     height="80">](https://jb.gg/OpenSourceSupport)

<div>App icon is made by <a href="https://www.flaticon.com/authors/turkkub" title="turkkub">turkkub</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
<a href="https://www.flaticon.com/free-icons/language" title="language icons">Language icons created by Those Icons - Flaticon</a>
<a href="https://www.flaticon.com/free-icons/split-screen" title="split screen icons">Split screen icons created by Fajrul Fitrianto - Flaticon</a>
