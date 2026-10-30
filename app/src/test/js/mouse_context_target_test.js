// Run from the repository root: node app/src/test/js/mouse_context_target_test.js
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/assets/mouse_context_target.js'), 'utf8');
const window = { innerWidth: 400, innerHeight: 800 };
let target = null;
let point;
const document = { elementFromPoint(x, y) { point = [x, y]; return target; } };
const lookup = vm.runInNewContext(source, { window, document, Node: { TEXT_NODE: 3 } });
function element(link, image, editable = false) {
    return { isContentEditable: editable, closest(selector) { return selector === 'a[href]' ? link : selector === 'img' ? image : false; } };
}
assert.equal(lookup(0.5, 0.25), null);
assert.deepEqual(point, [200, 200]);
const link = { href: 'https://example.com/page', innerText: 'Link title' };
const image = { src: 'https://example.com/low.png', currentSrc: 'https://example.com/high.png', alt: 'Image title' };
target = element(link, null);
assert.equal(lookup(0.5, 0.25).url, link.href);
assert.equal(window._touchTarget, link);
target = element(link, image);
assert.equal(lookup(0.5, 0.25).image, image.currentSrc);
target = element(null, image);
assert.equal(lookup(0.5, 0.25).url, image.currentSrc);
target = element(null, null, true);
assert.equal(lookup(0.5, 0.25), null);
assert.equal(lookup(0.5, 0.25, true), null);
target = element(null, null);
const changes = [];
const range = { startContainer: { nodeType: 3 } };
document.caretRangeFromPoint = () => range;
window.getSelection = () => ({
    removeAllRanges() { changes.push('clear'); },
    addRange(value) { assert.equal(value, range); },
    modify(action, direction, granularity) { changes.push([action, direction, granularity]); }
});
assert.equal(lookup(0.5, 0.25, true).selected, true);
assert.deepEqual(changes, ['clear', ['move', 'forward', 'character'], ['move', 'backward', 'word'], ['extend', 'forward', 'word']]);
window.visualViewport = { width: 200, height: 400, offsetLeft: 30, offsetTop: 50 };
lookup(0.5, 0.25);
assert.deepEqual(point, [130, 150]);
console.log('Mouse context target checks passed.');
