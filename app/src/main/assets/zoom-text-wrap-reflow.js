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
    outline-offset: 2px !important;
}

:root body .${INJECTED_STYLE_CLASS_UP},
:root[style] body .${INJECTED_STYLE_CLASS_UP},
:root body[style] .${INJECTED_STYLE_CLASS_UP},
:root[style] body[style] .${INJECTED_STYLE_CLASS_UP} {
    outline-color: black !important;
    outline-style: dotted !important;
    outline-width: 4px !important;
    outline-offset: 4px !important;

    word-wrap: break-word !important;
    overflow-wrap: break-word !important;
    max-width: var(--text-reflow-wrap-max-width-var) !important;
}

:root body .${INJECTED_STYLE_CLASS_FIT},
:root[style] body .${INJECTED_STYLE_CLASS_FIT},
:root body[style] .${INJECTED_STYLE_CLASS_FIT},
:root[style] body[style] .${INJECTED_STYLE_CLASS_FIT} {
    word-wrap: break-word !important;
    overflow-wrap: break-word !important;
    max-width: var(--text-reflow-wrap-max-width-var) !important;
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

    const maxWidth = Math.round(window.visualViewport.width * 0.96);
    document.documentElement.style.setProperty('--text-reflow-wrap-max-width-var', `${maxWidth}px`);

    element.classList.add(INJECTED_STYLE_CLASS_FIT);

    element.classList.add(INJECTED_STYLE_CLASS_MARGIN);
    element.scrollIntoView({ behavior: 'instant', block: 'nearest', inline: 'start' });
    setTimeout(() => { element.classList.remove(INJECTED_STYLE_CLASS_MARGIN); }, 500);
}

window.document.addEventListener("pointerdown", onPointerDown, true);

//window.document.addEventListener("pointermove", onPointerMove, true);

window.document.addEventListener("pointerup", onPointerUp, true);
window.document.addEventListener("pointercancel", onPointerUp, true);
//window.document.addEventListener("pointerout", onPointerUp, true);
//window.document.addEventListener("pointerleave", onPointerUp, true);

})();

//const xpathSelector = `
////p |
////a[normalize-space(text())] |
////h1 |
////h2 |
////h3 |
////h4 |
////h5 |
////h6 |
////li |
////pre |
////div[b or em or i] |
////div[normalize-space(text())] |
////div[span[normalize-space(text())]]
//`;
//
//const allTextElements = new Set();

//function textReflowWrapToVisibleViewportWidth(targetElement) {
//
//    if (!document.getElementById("")) {
//    const styleContent = `.text-reflow-userscript { word-wrap: break-word !important; overflow-wrap:break-word !important; max-width:var(--text-reflow-max-width) !important; }
//    .text-reflow-scroll-padding {scroll-margin-left: 1vw !important;}`;
//    const styleElement = document.createElement('style');
//    styleElement.textContent = styleContent;
//    document.head.appendChild(styleElement);
//    isCssInjected = true;
//    }
//
//    const maxAllowedWidth = Math.round(window.visualViewport.width * 0.96);
//    document.documentElement.style.setProperty('--text-reflow-max-width', `${'$'}{maxAllowedWidth}px`);
//
//    // Select elements likely to contain text
//    const xpathResult = document.evaluate(xpathSelector, document, null, XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE, null);
//    allTextElements.clear();
//
//    for (let i = 0, n = xpathResult.snapshotLength, el; i < n; i++) {
//    el = xpathResult.snapshotItem(i);
//
//    if (!el.offsetParent) continue;
//    if (!el.textContent.trim()) continue;
//
//    // Proccess only top-level text elements
//    let isTopLevel = true;
//    let parent = el.parentElement;
//
//    while (parent) {
//    if (elementIsTextElement(parent)) {
//    isTopLevel = false;
//    break;
//    }
//    parent = parent.parentElement;
//    }
//    if (isTopLevel) {
//    // Apply CSS styles to element and skip it next time
//    el.classList.add('text-reflow-userscript');
//    allTextElements.add(el);
//    }
//    }
//
//    /// Scroll initial target element into view
//    if (zoomTarget && targetDyOffsetRatio) {
//    // Scroll to element vertically, according to new page layout
//    const targetOffset = targetDyOffsetRatio * window.innerHeight;
//    const rect = zoomTarget.getBoundingClientRect();
//    const targetTop = rect.top + window.pageYOffset;
//    const scrollToPosition = targetTop - targetOffset;
//
//    window.scrollTo({
//    top: scrollToPosition,
//    behavior: 'instant'
//    });
//
//    // Scroll element into view horizontally
//    // if (elementIsTextElement(zoomTarget)) {
//    if (zoomTarget.nodeName !== 'IMG' && zoomTarget.nodeName !== 'VIDEO' && zoomTarget.nodeName !== 'IFRAME'){
//    zoomTarget.classList.add('text-reflow-scroll-padding')
//    zoomTarget.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' });
//    zoomTarget.classList.remove('text-reflow-scroll-padding')
//    }
//
//    // Reset the target and offset after scrolling
//    zoomTarget = null;
//    targetDyOffsetRatio = null;
//    }
//}

//function elementIsTextElement(element) {
//return allTextElements.has(element);
//}
//
//// Detect start of multi-touch (pinch) gesture
//function handleTouchStart(event) {
//if (event.touches && event.touches.length >= 2) {
//isPinching = true;
//
//// Store possible target of zoom gesture
//if (event.target) zoomTarget = event.target;
//
//// Calculate the midpoint between the two touch points
//const touch1 = event.touches[0];
//const touch2 = event.touches[1];
//const midpointX = (touch1.clientX + touch2.clientX) / 2;
//const midpointY = (touch1.clientY + touch2.clientY) / 2;
//
//// Use document.elementFromPoint to get the element at the midpoint
//let possibleZoomTarget;
//const elementsFromPoint = document.elementsFromPoint(midpointX, midpointY);
//for (let i = 0, n = elementsFromPoint.length, element; i < n; i++) {
//element = elementsFromPoint[i];
//if (elementIsTextElement(element)) {
//possibleZoomTarget = element;
//break;
//}
//}
//if (!possibleZoomTarget) possibleZoomTarget = elementsFromPoint[0];
//if (possibleZoomTarget) zoomTarget = possibleZoomTarget;
//
//// Store screen coordinates of target to scroll it into view after reflow
//const targetRect= zoomTarget.getBoundingClientRect();
//targetDyOffsetRatio = targetRect.top / window.innerHeight;
//}
//}
//
//// Detect end of multi-touch (pinch) gesture
//function handleTouchEnd(event) {
//if (isPinching && (event.touches && event.touches.length === 0)) {
//isPinching = false;
//reflowText();
//}
//}
//
//// Add event listeners
//window.addEventListener('touchstart', handleTouchStart);
//window.addEventListener('touchend', handleTouchEnd);

/// Uncomment to test on PC
// window.visualViewport.addEventListener('resize', reflowText);