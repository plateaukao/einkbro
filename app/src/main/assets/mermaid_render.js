// Renders ```mermaid fences in AI chat answers (chat.html) into SVG.
//
// Mermaid is loaded from the CDN on first use only: the library is 3.4 MB
// (about 1 MB over the wire), far too much to bundle for something most answers
// never need, and the chat page only exists after an AI API call, so the network
// is there. When the load or a render fails the fence stays a readable code block.
(function () {
    const MERMAID_SRC = 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js';
    let loading = null;
    let seq = 0;

    function load() {
        if (window.mermaid) return Promise.resolve(window.mermaid);
        if (loading) return loading;
        loading = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = MERMAID_SRC;
            script.onload = () => {
                try {
                    // Neutral theme: grayscale, reads well on e-ink.
                    window.mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'strict' });
                    resolve(window.mermaid);
                } catch (e) {
                    reject(e);
                }
            };
            script.onerror = () => {
                loading = null;
                reject(new Error('mermaid failed to load from ' + MERMAID_SRC));
            };
            document.head.appendChild(script);
        });
        return loading;
    }

    async function renderSvg(source) {
        const mermaid = await load();
        const { svg } = await mermaid.render('einkbro-mermaid-' + (++seq), source);
        const diagram = document.createElement('div');
        diagram.className = 'mermaid-diagram';
        diagram.innerHTML = svg;
        return diagram;
    }

    // Replaces every <pre><code class="language-mermaid"> inside container (what
    // marked emits for a ```mermaid fence) with its diagram. Blocks whose source
    // doesn't parse are left as code blocks.
    async function renderBlocks(container) {
        const blocks = Array.from(container.querySelectorAll('pre > code'))
            .filter(code => /^language-mermaid$/i.test(code.className.trim()));
        for (const code of blocks) {
            const pre = code.parentElement;
            if (!pre || !pre.isConnected) continue;
            try {
                pre.replaceWith(await renderSvg(code.textContent));
            } catch (e) {
                console.warn('mermaid:', e.message || e);
            }
        }
    }

    window.EinkbroMermaid = { load, renderBlocks };
})();
