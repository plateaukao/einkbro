// Run from the repository root: node app/src/test/js/gm_shim_test.js
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const shim = fs.readFileSync(path.join(__dirname, '../../main/assets/gm_shim.js'), 'utf8');

const calls = [];
const window = {
    einkbroGM: {
        gmGetValue: (token, key) => { calls.push(['read', token, key]); return '42'; },
        gmSetValue: (token, key, value) => calls.push(['write', token, key, value]),
        gmXhr: (token, id) => calls.push(['xhr', token, id]),
    },
};
const context = vm.createContext({ window, self: window, setTimeout, console });
function inject(grants, token = 'token') {
    return vm.runInContext(shim
        .replaceAll('__SCRIPT_ID__', '1')
        .replaceAll('__GM_TOKEN__', JSON.stringify(token))
        .replaceAll('__GM_INFO__', JSON.stringify({ script: { grant: grants } })), context);
}

const read = inject(['GM_getValue'], 'reader');
assert.equal(read.GM_getValue('key'), 42);
assert.equal(read.GM_setValue, undefined);
assert.equal(read.GM.setValue, undefined);
assert.equal(window.GM_getValue, undefined);
assert.equal(window.GM, undefined);

const write = inject(['GM.setValue'], 'writer');
write.GM_setValue('key', 7);
assert.equal(write.GM_getValue, undefined);
assert.equal(read.GM_setValue, undefined);
assert.deepEqual(calls, [['read', 'reader', 'key'], ['write', 'writer', 'key', '7']]);

for (const grants of [[], ['none'], ['none', 'GM_getValue']]) {
    const api = inject(grants);
    assert.equal(api.GM_getValue, undefined);
    assert.equal(api.GM.getValue, undefined);
    assert.equal(api.GM_xmlhttpRequest, undefined);
    assert.ok(api.GM_info);
}
for (const grant of ['GM_xmlhttpRequest', 'GM.xmlHttpRequest', 'GM.xmlhttpRequest']) {
    const api = inject([grant]);
    assert.equal(typeof api.GM_xmlhttpRequest, 'function');
    assert.equal(typeof api.GM.xmlHttpRequest, 'function');
    assert.equal(api.GM_setClipboard, undefined);
}

// Exercise the private binding used around the script body and its dependencies.
context.api = read;
vm.runInContext('(function(){ const {GM_getValue, GM_setValue} = api; ' +
    'if (GM_getValue("private") !== 42 || GM_setValue !== undefined) throw Error("grants"); ' +
    'var scriptLocal = true; }).call(window);', context);
assert.equal(context.scriptLocal, undefined);
console.log('GM shim permission checks passed');
