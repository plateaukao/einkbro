// https://github.com/plateaukao/einkbro/issues/537
// https://github.com/emvaized/text-reflow-on-zoom-mobile/blob/main/src/text_reflow_on_pinch_zoom.js

(function() {

const firstNearestCommonAncestor_ = (nodeA, nodeB) => {
   if (nodeA === nodeB || nodeA.contains(nodeB)) return nodeA;
   if (nodeB.contains(nodeA)) return nodeB;

   const range = new Range();
   range.setStartBefore(nodeA);
   range.setEndAfter(nodeB);
   if (range.collapsed) {
      range.setStartBefore(nodeB);
      range.setEndAfter(nodeA);
   }
   return range.commonAncestorContainer;
};
const firstNearestCommonAncestor = (nodes) => nodes.reduce((acc, node) => (acc === node ? acc : firstNearestCommonAncestor_(acc, node)), nodes[0]);


const INJECTED_STYLE_ELEMENT_ID = "_ID_STYLE_textReflowWrapToVisibleViewportWidth";
const INJECTED_STYLE_CLASS_DOWN = "_CLASS_DOWN_textReflowWrapToVisibleViewportWidth";
const INJECTED_STYLE_CLASS_UP = "_CLASS_UP_textReflowWrapToVisibleViewportWidth";
const INJECTED_STYLE_CLASS_MARGIN = "_CLASS_MARGIN_textReflowWrapToVisibleViewportWidth";
const INJECTED_STYLE_CLASS_FIT = "_CLASS_FIT_textReflowWrapToVisibleViewportWidth";

if (!document.getElementById(INJECTED_STYLE_ELEMENT_ID)) {

    const styleElement = document.createElement('style');

    styleElement.setAttribute("id", INJECTED_STYLE_ELEMENT_ID);

    styleElement.textContent =
`
:root {
    --text-reflow-wrap-max-width-var: initial;
}

:root body .${INJECTED_STYLE_CLASS_DOWN},
:root[style] body .${INJECTED_STYLE_CLASS_DOWN},
:root body[style] .${INJECTED_STYLE_CLASS_DOWN},
:root[style] body[style] .${INJECTED_STYLE_CLASS_DOWN} {
    outline-color: black !important;
    outline-style: dashed !important;
    outline-width: 2px !important;
    outline-offset: -2px !important;
}

:root body .${INJECTED_STYLE_CLASS_UP},
:root[style] body .${INJECTED_STYLE_CLASS_UP},
:root body[style] .${INJECTED_STYLE_CLASS_UP},
:root[style] body[style] .${INJECTED_STYLE_CLASS_UP} {
    outline-color: black !important;
    outline-style: dotted !important;
    outline-width: 4px !important;
    outline-offset: -4px !important;
}

:root body .${INJECTED_STYLE_CLASS_FIT},
:root[style] body .${INJECTED_STYLE_CLASS_FIT},
:root body[style] .${INJECTED_STYLE_CLASS_FIT},
:root[style] body[style] .${INJECTED_STYLE_CLASS_FIT} {
    word-wrap: break-word !important;
    overflow-wrap: break-word !important;

    max-width: var(--text-reflow-wrap-max-width-var) !important;

    margin: 0 !important;

    padding-left: 1em !important;
    padding-right: 1em !important;
    padding-top: 1em !important;
    padding-bottom: 1em !important;
}

:root body .${INJECTED_STYLE_CLASS_MARGIN},
:root[style] body .${INJECTED_STYLE_CLASS_MARGIN},
:root body[style] .${INJECTED_STYLE_CLASS_MARGIN},
:root[style] body[style] .${INJECTED_STYLE_CLASS_MARGIN} {
    scroll-margin-left: 1vw !important;
}
`;

    document.head.appendChild(styleElement);
}

const _touchDowns = [];

let timeout_INJECTED_STYLE_CLASS_MARGIN_add = undefined;
let timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
let timeout_INJECTED_STYLE_CLASS_UP = undefined;

function onPointerDown(ev) {
    console.log("POINTER DOWN ", ev.pointerId, " -- ", ev.x, " , ", ev.y, " >> ", JSON.stringify(_touchDowns, null, 4));
    ev.stopPropagation();

    if (ev.pointerType !== "touch") {
        return;
    }

//    const elementsDebug = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_DEBUG}`);
//    elementsDebug?.forEach((element) => {
//        element.classList.remove(INJECTED_STYLE_CLASS_DEBUG);
//    });

    const elementsUp = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_UP}`);
    elementsUp?.forEach((element) => {
        element.classList.remove(INJECTED_STYLE_CLASS_UP);
    });

    // ALL released => reset array
    if (!_touchDowns.find((item) => !item.released)) {
        _touchDowns.splice(0, _touchDowns.length);
    }

    _touchDowns.push({
        id: ev.pointerId,
        target: ev.target,
        x: ev.x,
        y: ev.y,
        released: false,
    });

    ev.target.classList.add(INJECTED_STYLE_CLASS_DOWN);
}
function onPointerUp(ev) {
    console.log("POINTER UP ", ev.pointerId, " -- ", ev.x, " , ", ev.y, " >> ", JSON.stringify(_touchDowns, null, 4));
    ev.stopPropagation();

    if (ev.pointerType !== "touch") {
        return;
    }

    let found = undefined;
    let foundScrollOrPinchZoom = false;
    if (!_touchDowns.length || // wtf?
        !(found = _touchDowns.find((item) => item.id === ev.pointerId)) || // wtf?
        (foundScrollOrPinchZoom = (found.x !== 0 && found.y !== 0 && ev.x === 0 && ev.y === 0)) // pinch zoom (dual pointer) or scroll (single pointer)
    ) {
        const preserveFit = foundScrollOrPinchZoom && _touchDowns.length === 1; // scroll

        _touchDowns.splice(0, _touchDowns.length);

        const elementsUp = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_UP}`);
        elementsUp?.forEach((element) => {
            element.classList.remove(INJECTED_STYLE_CLASS_UP);
        });

        const elementsDown = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_DOWN}`);
        elementsDown?.forEach((element) => {
            element.classList.remove(INJECTED_STYLE_CLASS_DOWN);
        });

        if (preserveFit) {
            return;
        }

        const elementsFit = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_FIT}`);
        elementsFit?.forEach((element) => {
            element.classList.remove(INJECTED_STYLE_CLASS_FIT);
        });

        return;
    }

    _touchDowns.forEach((item) => {
        if (item.id === ev.pointerId) {
            item.released = true;
            item.target.classList.remove(INJECTED_STYLE_CLASS_DOWN);
            item.target.classList.add(INJECTED_STYLE_CLASS_UP);
        }
    });

    if (_touchDowns.find((item) => !item.released)) { // not ALL released
        return;
    }

    const elementsDown = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_DOWN}`);
    elementsDown?.forEach((element) => {
        element.classList.remove(INJECTED_STYLE_CLASS_DOWN);
    });

    const elementsUp = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_UP}`);
    elementsUp?.forEach((element) => {
        element.classList.remove(INJECTED_STYLE_CLASS_UP);
    });

    if (_touchDowns.length < 2) {
        return;
    }

    if (_touchDowns.length === 3) {
        const elementsFit = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_FIT}`);
        elementsFit?.forEach((element) => {
            element.classList.remove(INJECTED_STYLE_CLASS_FIT);
        });
        return;
    }

    const element = firstNearestCommonAncestor(_touchDowns.map((item) => item.target)) || _touchDowns[0].target;

    element.classList.add(INJECTED_STYLE_CLASS_UP);
    if (!!timeout_INJECTED_STYLE_CLASS_UP) {
        window.clearTimeout(timeout_INJECTED_STYLE_CLASS_UP);
        timeout_INJECTED_STYLE_CLASS_UP = undefined;
    }
    timeout_INJECTED_STYLE_CLASS_UP = setTimeout(() => {
        timeout_INJECTED_STYLE_CLASS_UP = undefined;
        const elementsUp = document.documentElement.querySelectorAll(`.${INJECTED_STYLE_CLASS_UP}`);
        elementsUp?.forEach((element) => {
            element.classList.remove(INJECTED_STYLE_CLASS_UP);
        });
    }, 800);

    const maxWidth = Math.round(window.visualViewport.width * 0.96);
    document.documentElement.style.setProperty('--text-reflow-wrap-max-width-var', `${maxWidth}px`);

//    const rect = element.getBoundingClientRect();
//    let delay = 0;
//    if (rect.width < maxWidth) {
//        delay = 1000;
//        const ratio = maxWidth / rect.width;
//        console.log("ZOOM ratio: ", ratio);
//        document.documentElement.style.zoom = ratio;
//    }

    element.classList.add(INJECTED_STYLE_CLASS_FIT);

    if (!!timeout_INJECTED_STYLE_CLASS_MARGIN_add) {
        window.clearTimeout(timeout_INJECTED_STYLE_CLASS_MARGIN_add);
        timeout_INJECTED_STYLE_CLASS_MARGIN_add = undefined;
    }
    timeout_INJECTED_STYLE_CLASS_MARGIN_add = setTimeout(() => {
        timeout_INJECTED_STYLE_CLASS_MARGIN_add = undefined;

        element.classList.add(INJECTED_STYLE_CLASS_MARGIN);
        element.scrollIntoView({ behavior: 'instant', block: 'nearest', inline: 'start' });

        if (!!timeout_INJECTED_STYLE_CLASS_MARGIN_remove) {
            window.clearTimeout(timeout_INJECTED_STYLE_CLASS_MARGIN_remove);
            timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
        }
        timeout_INJECTED_STYLE_CLASS_MARGIN_remove = setTimeout(() => {
            timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
            element.classList.remove(INJECTED_STYLE_CLASS_MARGIN);
        }, 500);
    }, 500);
}

//window.visualViewport.addEventListener("resize", onVisualViewportResize);

window.document.addEventListener("pointerdown", onPointerDown, true);

//window.document.addEventListener("pointermove", onPointerMove, true);

window.document.addEventListener("pointerup", onPointerUp, true);
window.document.addEventListener("pointercancel", onPointerUp, true);
//window.document.addEventListener("pointerout", onPointerUp, true);
//window.document.addEventListener("pointerleave", onPointerUp, true);

})();
