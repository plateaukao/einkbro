(function() {
    // In vertical-rl each text line is a vertical strip; the line advance is
    // the horizontal distance between neighboring strips. Measure it from the
    // rendered geometry (immune to textZoom and font fallback) as the median
    // delta between the deduplicated left edges of a paragraph's line boxes.
    function lineAdvance(el) {
        var range = document.createRange();
        range.selectNodeContents(el);
        var rects = range.getClientRects();
        if (!rects || rects.length < 2) return 0;

        var lefts = [];
        for (var i = 0; i < rects.length; i++) {
            if (rects[i].height < 1) continue;
            var left = rects[i].left;
            var isDup = false;
            for (var j = 0; j < lefts.length; j++) {
                if (Math.abs(lefts[j] - left) < 1) { isDup = true; break; }
            }
            if (!isDup) lefts.push(left);
        }
        if (lefts.length < 2) return 0;

        // First line is the rightmost strip.
        lefts.sort(function(a, b) { return b - a; });
        var deltas = [];
        for (var i = 1; i < lefts.length; i++) {
            deltas.push(lefts[i - 1] - lefts[i]);
        }
        deltas.sort(function(a, b) { return a - b; });
        return deltas[Math.floor(deltas.length / 2)];
    }

    var paragraphs = document.getElementsByTagName('p');
    for (var i = 0; i < paragraphs.length; i++) {
        var advance = lineAdvance(paragraphs[i]);
        if (advance > 1) return advance.toFixed(2);
    }
    var bodyAdvance = lineAdvance(document.body);
    return bodyAdvance > 1 ? bodyAdvance.toFixed(2) : '0';
})()
