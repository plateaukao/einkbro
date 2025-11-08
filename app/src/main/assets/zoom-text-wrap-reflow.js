// ======================== CREDITS
// --- CSS styles from:
// https://github.com/emvaized/text-reflow-on-zoom-mobile/blob/main/src/text_reflow_on_pinch_zoom.js
//     word-wrap: break-word;
//     overflow-wrap: break-word;
//     max-width: var(--window-visualViewport-width); /* window.visualViewport.width */
// --- Pagination / scroll offset from:
// https://github.com/plateaukao/einkbro/

(function () {
  "use strict";

  const debounce = (callback, wait) => {
    let timeoutId = null;
    const func = (...args) => {
      if (timeoutId) window.clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => {
        if (!timeoutId) {
          return;
        }
        callback(...args);
      }, wait);
    };
    func.clear = () => {
      if (timeoutId) window.clearTimeout(timeoutId);
      timeoutId = null;
    };
    return func;
  };

  const NAME = "e-ink_friend";
  const DEBUG = true;

  if (DEBUG) console.log(`[${NAME}] Starting...`);

  function bootstrap() {
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
    const firstNearestCommonAncestor = (nodes) =>
      nodes.reduce(
        (acc, node) =>
          acc === node ? acc : firstNearestCommonAncestor_(acc, node),
        nodes[0],
      );

    const INJECTED_LEFT_ELEMENT_ID =
      "_ID_LEFT_textReflowWrapToVisibleViewportWidth";
    const INJECTED_RIGHT_ELEMENT_ID =
      "_ID_RIGHT_textReflowWrapToVisibleViewportWidth";
    const INJECTED_STYLE_ELEMENT_ID =
      "_ID_STYLE_textReflowWrapToVisibleViewportWidth";
    const INJECTED_STYLE_CLASS_DOWN =
      "_CLASS_DOWN_textReflowWrapToVisibleViewportWidth";
    const INJECTED_STYLE_CLASS_UP =
      "_CLASS_UP_textReflowWrapToVisibleViewportWidth";
    const INJECTED_STYLE_CLASS_MARGIN =
      "_CLASS_MARGIN_textReflowWrapToVisibleViewportWidth";
    const INJECTED_STYLE_CLASS_FIT =
      "_CLASS_FIT_textReflowWrapToVisibleViewportWidth";

    const WIDTH_TOUCH_OVERLAY = 100;
    const BORDER_TOUCH_OVERLAY = 2;

    function round(v) {
      return Math.round(v * 1000) / 1000;
    }

    function doLayout() {
      const height = round(window.visualViewport.height / 3); // window.visualViewport.scale
      const top = round(
        window.visualViewport.offsetTop +
          document.scrollingElement.scrollTop +
          height,
      );
      const width = round(WIDTH_TOUCH_OVERLAY / window.visualViewport.scale);
      const borderWidth = round(
        BORDER_TOUCH_OVERLAY / window.visualViewport.scale,
      );

      const left = document.getElementById(INJECTED_LEFT_ELEMENT_ID);
      left.style.setProperty(
        "left",
        `${round(window.visualViewport.offsetLeft + document.scrollingElement.scrollLeft)}px`,
        "important",
      );
      left.style.setProperty("top", `${top}px`, "important");
      left.style.setProperty("width", `${width}px`, "important");
      left.style.setProperty("border-width", `${borderWidth}px`, "important");
      left.style.setProperty(
        "border-top-right-radius",
        `${borderWidth * 10}px`,
        "important",
      );
      left.style.setProperty(
        "border-bottom-right-radius",
        `${borderWidth * 10}px`,
        "important",
      );
      left.style.setProperty("height", `${height}px`, "important");

      left.style.display = "block";

      const scrollbarWidth =
        document.scrollingElement.clientWidth -
        document.scrollingElement.offsetWidth;

      // console.log("window.innerWidth", window.innerWidth);
      // console.log(
      //   "document.documentElement.offsetWidth",
      //   document.documentElement.offsetWidth,
      // );
      // console.log("document.body.clientWidth", document.body.clientWidth);

      // console.log(
      //   "document.scrollingElement.offsetWidth",
      //   document.scrollingElement.offsetWidth,
      // );
      // console.log(
      //   "document.scrollingElement.clientWidth",
      //   document.scrollingElement.clientWidth,
      // );

      const right = document.getElementById(INJECTED_RIGHT_ELEMENT_ID);
      right.style.setProperty(
        "left",
        `${round(
          scrollbarWidth / window.visualViewport.scale +
            window.visualViewport.offsetLeft +
            window.visualViewport.width +
            document.scrollingElement.scrollLeft -
            110 / window.visualViewport.scale,
        )}px`,
        "important",
      );
      right.style.setProperty("top", `${top}px`, "important");
      right.style.setProperty("width", `${width}px`, "important");
      right.style.setProperty("border-width", `${borderWidth}px`, "important");
      right.style.setProperty(
        "border-top-left-radius",
        `${borderWidth * 10}px`,
        "important",
      );
      right.style.setProperty(
        "border-bottom-left-radius",
        `${borderWidth * 10}px`,
        "important",
      );
      right.style.setProperty("height", `${height}px`, "important");

      right.style.display = "block";
    }

    const relayout = () => {
      doLayoutDebounced.clear();

      const left = document.getElementById(INJECTED_LEFT_ELEMENT_ID);
      left.style.display = "none";
      const right = document.getElementById(INJECTED_RIGHT_ELEMENT_ID);
      right.style.display = "none";

      doLayoutDebounced();
    };

    const doLayoutDebounced = debounce(() => {
      window.requestAnimationFrame(() => {
        doLayout();
      });
    }, 500);

    if (!document.getElementById(INJECTED_STYLE_ELEMENT_ID)) {
      if (DEBUG)
        console.log(`[${NAME}] Injecting CSS styles in document HEAD...`);

      const styleElement = document.createElement("style");

      styleElement.setAttribute("id", INJECTED_STYLE_ELEMENT_ID);

      styleElement.textContent = `
:root {
  --window-visualViewport-width: initial;
}

:root body *,
:root body *[class],
:root[style] body *,
:root[style] body *[class],
:root body[style] *,
:root body[style] *[class],
:root[style] body[style] *,
:root[style] body[style] *[class] {
  color: black !important;
  background-color: white !important;
  line-height: 2em !important;
}

:root body .${INJECTED_STYLE_CLASS_DOWN},
:root[style] body .${INJECTED_STYLE_CLASS_DOWN},
:root body[style] .${INJECTED_STYLE_CLASS_DOWN},
:root[style] body[style] .${INJECTED_STYLE_CLASS_DOWN} {
  outline-color: black !important;
  outline-style: dashed !important;
  outline-width: 0.2em !important;
  outline-offset: -0.2em !important;
}

:root body .${INJECTED_STYLE_CLASS_UP},
:root[style] body .${INJECTED_STYLE_CLASS_UP},
:root body[style] .${INJECTED_STYLE_CLASS_UP},
:root[style] body[style] .${INJECTED_STYLE_CLASS_UP} {
  outline-color: black !important;
  outline-style: dotted !important;
  outline-width: 0.4em !important;
  outline-offset: -0.4em !important;
}

:root body .${INJECTED_STYLE_CLASS_FIT},
:root[style] body .${INJECTED_STYLE_CLASS_FIT},
:root body[style] .${INJECTED_STYLE_CLASS_FIT},
:root[style] body[style] .${INJECTED_STYLE_CLASS_FIT} {
  word-wrap: break-word !important;
  overflow-wrap: break-word !important;

  max-width: var(--window-visualViewport-width) !important;

  box-sizing: border-box !important;

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

#${INJECTED_LEFT_ELEMENT_ID} {
  all: initial;
  box-sizing: border-box !important;
  position: absolute !important;
  background-color: transparent !important;
  border: ${BORDER_TOUCH_OVERLAY}px dotted transparent !important;
  border-top-right-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
  border-bottom-right-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
}
#${INJECTED_RIGHT_ELEMENT_ID} {
  all: initial;
  box-sizing: border-box !important;
  position: absolute !important;
  background-color: transparent !important;
  border: ${BORDER_TOUCH_OVERLAY}px dotted transparent !important;
  border-top-left-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
  border-bottom-left-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
}
`;

      document.head.appendChild(styleElement);

      if (DEBUG)
        console.log(`[${NAME}] Injecting user interface in document BODY...`);

      const leftElement = document.createElement("div");
      leftElement.setAttribute("id", INJECTED_LEFT_ELEMENT_ID);
      document.body.appendChild(leftElement);

      const rightElement = document.createElement("div");
      rightElement.setAttribute("id", INJECTED_RIGHT_ELEMENT_ID);
      document.body.appendChild(rightElement);
    }

    relayout();

    const _touchDowns = [];

    let timeout_INJECTED_STYLE_CLASS_MARGIN_add = undefined;
    let timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
    let timeout_INJECTED_STYLE_CLASS_UP = undefined;

    // ####################################################################################
    function onTouchStart(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchstart" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();
    }
    window.document.addEventListener("touchstart", onTouchStart, true);

    // ####################################################################################
    function onTouchEnd(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchend" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();
    }
    window.document.addEventListener("touchend", onTouchEnd, true);

    // ####################################################################################
    function onTouchCancel(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchcancel" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();
    }
    window.document.addEventListener("touchcancel", onTouchCancel, true);

    // ####################################################################################
    function onTouchMove(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchmove" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();
    }
    window.document.addEventListener("touchmove", onTouchMove, true);

    // ####################################################################################
    function onPointerLeave(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointerleave" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();
    }
    window.document.addEventListener("pointerleave", onPointerLeave, true);

    // ####################################################################################
    function onPointerOut(ev) {
      relayout();
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointerout" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();

      // if (ev.pointerType !== "touch") {
      //   return;
      // }
    }
    window.document.addEventListener("pointerout", onPointerOut, true);

    // ####################################################################################
    function onPointerDown(ev) {
      relayout();

      if (DEBUG)
        console.log(
          `[${NAME}] DOCUMENT EVENT: "pointerdown" ${ev.pointerId} (${ev.x}, ${ev.y})`,
        );

      if (DEBUG)
        console.log(
          `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
        );

      ev.stopPropagation();

      if (ev.pointerType !== "touch") {
        return;
      }

      if (
        ev.target === document.getElementById(INJECTED_LEFT_ELEMENT_ID) ||
        ev.target === document.getElementById(INJECTED_RIGHT_ELEMENT_ID)
      ) {
        return;
      }

      // ALL released => reset array
      if (!_touchDowns.find((item) => !item.released)) {
        _touchDowns.splice(0, _touchDowns.length);

        const elementsUp = document.documentElement.querySelectorAll(
          `.${INJECTED_STYLE_CLASS_UP}`,
        );
        elementsUp?.forEach((element) => {
          element.classList.remove(INJECTED_STYLE_CLASS_UP);
        });
      }

      _touchDowns.push({
        id: ev.pointerId,
        target: ev.target,
        x: ev.x,
        y: ev.y,
        // mx: ev.x,
        // my: ev.y,
        released: false,
      });

      ev.target.classList.add(INJECTED_STYLE_CLASS_DOWN);
    }
    window.document.addEventListener("pointerdown", onPointerDown, true);

    // ####################################################################################
    function onPointerMove(ev) {
      relayout();

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointermove" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // // ev.stopPropagation();

      // if (ev.pointerType !== "touch") {
      //   return;
      // }

      // _touchDowns.forEach((item) => {
      //   if (item.id === ev.pointerId && item.target === ev.target) {
      //     item.mx = ev.x;
      //     item.my = ev.y;
      //   }
      // });
    }
    window.document.addEventListener("pointermove", onPointerMove, true);

    // ####################################################################################
    function onPointerCancel(ev) {
      if (DEBUG)
        console.log(
          `[${NAME}] DOCUMENT EVENT: "pointercancel" ${ev.pointerId} (${ev.x}, ${ev.y})`,
        );
      onPointerUp(ev);

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // ev.stopPropagation();

      // if (ev.pointerType !== "touch") {
      //   return;
      // }

      // const found = _touchDowns.find(
      //   (item) => item.id === ev.pointerId && item.target === ev.target,
      // );
      // if (!found) {
      //   if (DEBUG) console.log(`[${NAME}] ====> not found`);
      //   return;
      // }

      // // if (
      // //   round(found.x) === round(found.mx) &&
      // //   round(found.y) === round(found.my)
      // // ) {
      // //   if (DEBUG) console.log(`[${NAME}] ====> onPointerUp() NO MOVE`);
      // //   onPointerUp(ev);
      // //   return;
      // // }

      // if (DEBUG)
      //   console.log(`[${NAME}] ====> onPointerUp() MOVED, scroll or zoom`);
      // onPointerUp({
      //   pointerId: ev.pointerId,
      //   target: ev.target,
      //   x: 0,
      //   y: 0,
      //   pointerType: "touch",
      //   stopPropagation: () => {},
      // });
    }
    window.document.addEventListener("pointercancel", onPointerCancel, true);

    // ####################################################################################
    function onPointerUp(ev) {
      relayout();

      if (DEBUG)
        console.log(
          `[${NAME}] DOCUMENT EVENT: "pointerup" ${ev.pointerId} (${ev.x}, ${ev.y})`,
        );

      if (DEBUG)
        console.log(
          `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
        );

      ev.stopPropagation();

      if (ev.pointerType !== "touch") {
        return;
      }

      if (
        ev.target === document.getElementById(INJECTED_LEFT_ELEMENT_ID) ||
        ev.target === document.getElementById(INJECTED_RIGHT_ELEMENT_ID)
      ) {
        return;
      }

      _touchDowns.forEach((item) => {
        if (item.id === ev.pointerId && item.target === ev.target) {
          item.released = true;
          item.target.classList.remove(INJECTED_STYLE_CLASS_DOWN);
          item.target.classList.add(INJECTED_STYLE_CLASS_UP);
        }
      });
      // const found = _touchDowns.find(
      //   (item) => item.id === ev.pointerId && item.target === ev.target,
      // );
      // if (!found) {
      //   if (DEBUG) console.log(`[${NAME}] ====> not found`);
      //   return;
      // }

      if (_touchDowns.find((item) => !item.released)) {
        if (DEBUG) console.log(`[${NAME}] --NOT ALL RELEASED`);
        return;
      }

      if (_touchDowns.length === 1) {
        if (DEBUG) console.log(`[${NAME}] ...SCROLL/PAN`);
      } else if (_touchDowns.length === 2) {
        if (DEBUG) console.log(`[${NAME}] ...ZOOM`);
      } else {
        if (DEBUG) console.log(`[${NAME}] ...ACTION`);
      }

      const elementsUp = document.documentElement.querySelectorAll(
        `.${INJECTED_STYLE_CLASS_UP}`,
      );
      elementsUp?.forEach((element) => {
        element.classList.remove(INJECTED_STYLE_CLASS_UP);
      });

      const elementsDown = document.documentElement.querySelectorAll(
        `.${INJECTED_STYLE_CLASS_DOWN}`,
      );
      elementsDown?.forEach((element) => {
        element.classList.remove(INJECTED_STYLE_CLASS_DOWN);
      });

      const scrolling = _touchDowns.length === 1;
      if (
        _touchDowns.length === 1 ||
        _touchDowns.length === 2 ||
        _touchDowns.length >= 4
      ) {
        _touchDowns.splice(0, _touchDowns.length);

        if (scrolling) {
          if (DEBUG)
            console.log(`[${NAME}] 1-finger SCROLL ==> PRESERVE WRAP FIT`);
          return;
        }

        const elementsFit = document.documentElement.querySelectorAll(
          `.${INJECTED_STYLE_CLASS_FIT}`,
        );
        elementsFit?.forEach((element) => {
          element.classList.remove(INJECTED_STYLE_CLASS_FIT);
        });

        if (DEBUG)
          console.log(
            `[${NAME}] 2-finger ZOOM or 4-finger TAP ==> CANCEL WRAP FIT`,
          );
        return;
      }

      // _touchDowns.length === 3

      if (DEBUG) console.log(`[${NAME}] 3-finger TAP ==> WRAP!`);

      const element =
        firstNearestCommonAncestor(
          Array.from(new Set(_touchDowns.map((item) => item.target))),
        ) || _touchDowns[0].target;

      element.classList.add(INJECTED_STYLE_CLASS_UP);
      if (!!timeout_INJECTED_STYLE_CLASS_UP) {
        window.clearTimeout(timeout_INJECTED_STYLE_CLASS_UP);
        timeout_INJECTED_STYLE_CLASS_UP = undefined;
      }
      timeout_INJECTED_STYLE_CLASS_UP = setTimeout(() => {
        timeout_INJECTED_STYLE_CLASS_UP = undefined;
        const elementsUp = document.documentElement.querySelectorAll(
          `.${INJECTED_STYLE_CLASS_UP}`,
        );
        elementsUp?.forEach((element) => {
          element.classList.remove(INJECTED_STYLE_CLASS_UP);
        });
      }, 800);

      const maxWidth = round(window.visualViewport.width * 0.96);
      document.documentElement.style.setProperty(
        "--window-visualViewport-width",
        `${maxWidth}px`,
      );

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
        element.scrollIntoView({
          behavior: "instant",
          block: "nearest",
          inline: "start",
        });
        relayout();

        if (!!timeout_INJECTED_STYLE_CLASS_MARGIN_remove) {
          window.clearTimeout(timeout_INJECTED_STYLE_CLASS_MARGIN_remove);
          timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
        }
        timeout_INJECTED_STYLE_CLASS_MARGIN_remove = setTimeout(() => {
          timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
          element.classList.remove(INJECTED_STYLE_CLASS_MARGIN);
          relayout();
        }, 500);
      }, 500);
    }
    window.document.addEventListener("pointerup", onPointerUp, true);

    window.addEventListener("scroll", function () {
      if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "scroll"`);
      relayout();
    });

    window.visualViewport.addEventListener("resize", function () {
      relayout();

      if (DEBUG)
        console.log(`[${NAME}] WINDOW VISUAL VIEWPORT EVENT: "resize"`);
      if (DEBUG)
        console.log(`window.devicePixelRatio: ${window.devicePixelRatio}`);
      if (DEBUG)
        console.log(`window.screen.availWidth: ${window.screen.availWidth}`);
      if (DEBUG)
        console.log(`window.screen.availHeight: ${window.screen.availHeight}`);
      if (DEBUG)
        console.log(
          `window.visualViewport.width: ${window.visualViewport.width}`,
        );
      if (DEBUG)
        console.log(
          `window.visualViewport.height: ${window.visualViewport.height}`,
        );
      if (DEBUG)
        console.log(
          `window.visualViewport.scale: ${window.visualViewport.scale}`,
        );
      if (DEBUG)
        console.log(
          `window.visualViewport.offsetLeft: ${window.visualViewport.offsetLeft}`,
        );
      if (DEBUG)
        console.log(
          `window.visualViewport.offsetTop: ${window.visualViewport.offsetTop}`,
        );
      if (DEBUG) console.log(`window.pageXOffset: ${window.pageXOffset}`);
      if (DEBUG) console.log(`window.pageYOffset: ${window.pageYOffset}`);
      if (DEBUG) console.log(`window.innerWidth: ${window.innerWidth}`);
      if (DEBUG) console.log(`window.innerHeight: ${window.innerHeight}`);
      if (DEBUG)
        console.log(
          `document.documentElement.clientWidth: ${document.documentElement.clientWidth}`,
        );
      if (DEBUG)
        console.log(
          `document.documentElement.clientHeight: ${document.documentElement.clientHeight}`,
        );
    });
    window.addEventListener("resize", function () {
      relayout();

      // if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "resize"`);
    });

    let tearDownMediaQuery = undefined;
    const updatePixelRatio = () => {
      relayout();

      tearDownMediaQuery?.();
      const media = window.matchMedia(
        `(resolution: ${window.devicePixelRatio}dppx)`,
      );
      media.addEventListener("change", updatePixelRatio);
      tearDownMediaQuery = () => {
        media.removeEventListener("change", updatePixelRatio);
      };
      if (DEBUG)
        console.log(
          `window.devicePixelRatio MEDIA QUERY: ${window.devicePixelRatio}`,
        );
    };
    updatePixelRatio();
  }

  if (document.readyState === "complete") {
    if (DEBUG) console.log(`[${NAME}] WINDOW is "load"`);
    bootstrap();
  } else if (document.readyState === "interactive") {
    if (DEBUG) console.log(`[${NAME}] WINDOW is "DOMContentLoaded"`);

    window.addEventListener("load", function () {
      if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "load"`);
      bootstrap();
    });
  } else {
    window.addEventListener("DOMContentLoaded", function () {
      if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "DOMContentLoaded"`);
    });

    window.addEventListener("load", function () {
      if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "load"`);
      bootstrap();
    });
  }
})();
