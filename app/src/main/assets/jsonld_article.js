(function () {
    const ARTICLE_TYPES = [
        "NewsArticle",
        "Article",
        "BlogPosting",
        "ReportageNewsArticle",
        "OpinionNewsArticle",
        "ReviewArticle",
        "TechArticle",
        "ScholarlyArticle",
        "BackgroundNewsArticle",
        "AnalysisNewsArticle",
    ];

    function matchesArticleType(type) {
        if (!type) return false;
        const list = Array.isArray(type) ? type : [type];
        return list.some(function (t) { return ARTICLE_TYPES.indexOf(String(t)) !== -1; });
    }

    function pickArticle(data) {
        const items = Array.isArray(data)
            ? data
            : (data && data["@graph"] ? data["@graph"] : [data]);
        for (let i = 0; i < items.length; i++) {
            const it = items[i];
            if (it && typeof it === "object" && matchesArticleType(it["@type"]) && it.articleBody) {
                return it;
            }
        }
        return null;
    }

    function findJsonLdArticle() {
        const scripts = document.querySelectorAll('script[type="application/ld+json"]');
        for (let i = 0; i < scripts.length; i++) {
            let data;
            try {
                data = JSON.parse(scripts[i].textContent);
            } catch (e) {
                continue;
            }
            const art = pickArticle(data);
            if (art) return art;
        }
        return null;
    }

    function normalize(s) {
        return String(s || "").replace(/\s+/g, " ").trim();
    }

    const CONTAINER_SELECTORS = [
        '[itemprop="articleBody"]',
        'article[id]',
        'section[id*="article"]',
        'article',
        'main[id]',
        '[class*="article-body"]',
        '[class*="article-content"]',
        '[id*="article-content"]',
        '[itemtype*="NewsArticle"]',
        '[itemtype*="Article"]',
        'section',
        'main',
    ];

    function findArticleContainer(articleBody) {
        const sig = normalize(articleBody).slice(0, 80);
        if (sig.length < 30) return null;

        const seen = new Set();
        const candidates = [];
        for (let i = 0; i < CONTAINER_SELECTORS.length; i++) {
            const nodes = document.querySelectorAll(CONTAINER_SELECTORS[i]);
            for (let j = 0; j < nodes.length; j++) {
                if (!seen.has(nodes[j])) { seen.add(nodes[j]); candidates.push(nodes[j]); }
            }
        }

        let best = null;
        let bestDepth = -1;
        for (let i = 0; i < candidates.length; i++) {
            const el = candidates[i];
            if (!normalize(el.innerText || el.textContent).includes(sig)) continue;
            let depth = 0, n = el;
            while (n.parentElement) { depth++; n = n.parentElement; }
            if (depth > bestDepth) { best = el; bestDepth = depth; }
        }
        return best;
    }

    // Build a Document that contains only the correct article container, so
    // Readability's scoring sees one article instead of several. Structural
    // HTML (images, headings, links) is preserved because we clone the real
    // DOM element rather than synthesizing from plain text.
    window.getReadabilityScopedDocument = function () {
        try {
            const jsonLd = findJsonLdArticle();
            if (!jsonLd || !jsonLd.articleBody) return null;
            const container = findArticleContainer(jsonLd.articleBody);
            if (!container) return null;

            const doc = document.implementation.createHTMLDocument(document.title || "");
            if (document.documentElement.lang) {
                doc.documentElement.lang = document.documentElement.lang;
            }
            const base = doc.createElement("base");
            base.href = document.baseURI || location.href;
            doc.head.appendChild(base);

            // Copy metadata so Readability can derive title/byline/excerpt
            const META_SELECTOR =
                'meta[property^="og:"], meta[name^="twitter:"], meta[name="description"], ' +
                'meta[name="author"], meta[name="keywords"], meta[itemprop], link[rel="canonical"]';
            const metas = document.head ? document.head.querySelectorAll(META_SELECTOR) : [];
            for (let i = 0; i < metas.length; i++) {
                doc.head.appendChild(metas[i].cloneNode(true));
            }
            // Preserve JSON-LD so Readability's metadata extractor sees it
            const jsonLdNodes = document.querySelectorAll('script[type="application/ld+json"]');
            for (let i = 0; i < jsonLdNodes.length; i++) {
                doc.head.appendChild(jsonLdNodes[i].cloneNode(true));
            }

            const wrapper = doc.createElement("article");
            wrapper.appendChild(container.cloneNode(true));
            doc.body.appendChild(wrapper);
            return doc;
        } catch (e) {
            return null;
        }
    };
})();
