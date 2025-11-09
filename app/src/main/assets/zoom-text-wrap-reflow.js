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
      const zoom = document.documentElement.style.zoom || 1;

      const height = round(window.visualViewport.height / 3 / zoom); // window.visualViewport.scale
      const top = round(
        (window.visualViewport.offsetTop +
          document.scrollingElement.scrollTop) /
          zoom +
          height,
      );
      const width = round(
        WIDTH_TOUCH_OVERLAY / window.visualViewport.scale / zoom,
      );
      const borderWidth = round(
        BORDER_TOUCH_OVERLAY / window.visualViewport.scale,
      );

      let bodyLeft = window.getComputedStyle(document.body).left;
      // console.log("bodyLeft", bodyLeft);

      let bodyRight = window.getComputedStyle(document.body).right;
      // console.log("bodyRight", bodyRight);

      bodyLeft = parseInt(bodyLeft);
      bodyRight = parseInt(bodyRight);

      bodyLeft = isNaN(bodyLeft) ? 0 : bodyLeft;
      bodyRight = isNaN(bodyRight) ? 0 : bodyRight;

      const scrollbarWidth =
        document.scrollingElement.clientWidth -
        document.scrollingElement.offsetWidth;

      const leftLeft =
        round(
          window.visualViewport.offsetLeft +
            document.scrollingElement.scrollLeft -
            bodyLeft,
        ) / zoom;

      const rightLeft =
        leftLeft +
        round(
          window.visualViewport.width -
            WIDTH_TOUCH_OVERLAY / window.visualViewport.scale,
        ) /
          zoom;
      // round(
      //   scrollbarWidth / window.visualViewport.scale +
      //     window.visualViewport.offsetLeft +
      //     window.visualViewport.width +
      //     document.scrollingElement.scrollLeft -
      //     bodyRight -
      //     WIDTH_TOUCH_OVERLAY / window.visualViewport.scale,
      // ) / zoom;

      // console.log(
      //   "document.scrollingElement.offsetWidth",
      //   document.scrollingElement.offsetWidth,
      // );
      // console.log(
      //   "document.scrollingElement.clientWidth",
      //   document.scrollingElement.clientWidth,
      // );
      // console.log("scrollbarWidth", scrollbarWidth);

      // console.log("window.innerWidth", window.innerWidth);
      // console.log(
      //   "document.documentElement.offsetWidth",
      //   document.documentElement.offsetWidth,
      // );
      // console.log("document.body.clientWidth", document.body.clientWidth);

      const left = document.getElementById(INJECTED_LEFT_ELEMENT_ID);
      left.style.setProperty("left", `${leftLeft}px`, "important");
      left.style.setProperty("top", `${top}px`, "important");
      left.style.setProperty("width", `${width}px`, "important");
      left.style.setProperty(
        "border-width",
        `${round(borderWidth / zoom)}px`,
        "important",
      );
      left.style.setProperty(
        "border-top-right-radius",
        `${round((borderWidth * 10) / zoom)}px`,
        "important",
      );
      left.style.setProperty(
        "border-bottom-right-radius",
        `${round((borderWidth * 10) / zoom)}px`,
        "important",
      );
      left.style.setProperty("height", `${height}px`, "important");

      left.style.display = "block";

      const right = document.getElementById(INJECTED_RIGHT_ELEMENT_ID);
      right.style.setProperty("left", `${rightLeft}px`, "important");
      right.style.setProperty("top", `${top}px`, "important");
      right.style.setProperty("width", `${width}px`, "important");
      right.style.setProperty(
        "border-width",
        `${round(borderWidth / zoom)}px`,
        "important",
      );
      right.style.setProperty(
        "border-top-left-radius",
        `${round((borderWidth * 10) / zoom)}px`,
        "important",
      );
      right.style.setProperty(
        "border-bottom-left-radius",
        `${round((borderWidth * 10) / zoom)}px`,
        "important",
      );
      right.style.setProperty("height", `${height}px`, "important");

      right.style.display = "block";
    }

    const relayout = (cause) => {
      if (DEBUG) console.log(`[${NAME}] relayout CAUSE: ${cause}`);

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
  /*outline-color: black !important;
  outline-style: dashed !important;
  outline-width: 0.2em !important;
  outline-offset: -0.2em !important;*/
}

:root body .${INJECTED_STYLE_CLASS_UP},
:root[style] body .${INJECTED_STYLE_CLASS_UP},
:root body[style] .${INJECTED_STYLE_CLASS_UP},
:root[style] body[style] .${INJECTED_STYLE_CLASS_UP} {
  outline-color: black !important;
  outline-style: dotted !important;
  outline-width: 0.2em !important;
  outline-offset: -0.2em !important;
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

  /*
  padding-left: 1em !important;
  padding-right: 1em !important;
  padding-top: 1em !important;
  padding-bottom: 1em !important;
  */
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
  border: ${BORDER_TOUCH_OVERLAY}px dotted black !important;
  border-top-right-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
  border-bottom-right-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
}
#${INJECTED_RIGHT_ELEMENT_ID} {
  all: initial;
  box-sizing: border-box !important;
  position: absolute !important;
  background-color: transparent !important;
  border: ${BORDER_TOUCH_OVERLAY}px dotted black !important;
  border-top-left-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
  border-bottom-left-radius: ${BORDER_TOUCH_OVERLAY * 10}px !important;
}
`;

      document.head.appendChild(styleElement);

      if (DEBUG) console.log(`[${NAME}] Pausing audio/video...`);

      const medias = document.body.querySelectorAll("video, audio");
      medias?.forEach((media) => {
        try {
          media.pause();
        } catch (e) {}
      });

      if (DEBUG)
        console.log(`[${NAME}] Checking meta viewport in document HEAD...`);

      const viewports_ = document.head.querySelectorAll(
        'meta[name="viewport"]',
      );
      const viewports = viewports_?.length ? Array.from(viewports_) : [];
      let viewportToInsert = undefined;
      if (!viewports.length) {
        const viewport = document.createElement("meta");
        viewport.setAttribute("name", "viewport");
        viewportToInsert = viewport;
        viewports.push(viewport);
      }
      viewports.forEach((viewport) => {
        viewport.setAttribute("content", "initial-scale=1,maximum-scale=10.0");
      });
      if (viewportToInsert) {
        if (DEBUG)
          console.log(
            `[${NAME}] Injecting missing meta viewport in document HEAD...`,
          );
        document.head.appendChild(viewportToInsert);
      }

      if (DEBUG)
        console.log(`[${NAME}] Injecting user interface in document BODY...`);

      const leftElement = document.createElement("div");
      leftElement.setAttribute("id", INJECTED_LEFT_ELEMENT_ID);
      document.body.appendChild(leftElement);

      const rightElement = document.createElement("div");
      rightElement.setAttribute("id", INJECTED_RIGHT_ELEMENT_ID);
      document.body.appendChild(rightElement);
    }

    relayout("bootstrap");

    const _touchDowns = [];
    let _doubleTap = undefined;

    let timeout_INJECTED_STYLE_CLASS_MARGIN_add = undefined;
    let timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
    let timeout_INJECTED_STYLE_CLASS_UP = undefined;

    // ####################################################################################
    function onTouchStart(ev) {
      ev.stopPropagation();
      relayout("onTouchStart");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchstart" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );
    }
    window.document.addEventListener("touchstart", onTouchStart, true);

    // ####################################################################################
    function onTouchEnd(ev) {
      ev.stopPropagation();
      relayout("onTouchEnd");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchend" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );
    }
    window.document.addEventListener("touchend", onTouchEnd, true);

    // ####################################################################################
    function onTouchCancel(ev) {
      ev.stopPropagation();
      relayout("onTouchCancel");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchcancel" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );
    }
    window.document.addEventListener("touchcancel", onTouchCancel, true);

    // ####################################################################################
    function onTouchMove(ev) {
      ev.stopPropagation();
      relayout("onTouchMove");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "touchmove" ${ev.touches?.length} (${JSON.stringify(ev.touches, null, 4)})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );
    }
    window.document.addEventListener("touchmove", onTouchMove, true);

    // ####################################################################################
    function onPointerLeave(ev) {
      ev.stopPropagation();
      // relayout("onPointerLeave");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointerleave" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );
    }
    window.document.addEventListener("pointerleave", onPointerLeave, true);

    // ####################################################################################
    function onPointerOut(ev) {
      ev.stopPropagation();
      relayout("onPointerOut");
      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointerout" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

      // if (ev.pointerType !== "touch") {
      //   return;
      // }
    }
    window.document.addEventListener("pointerout", onPointerOut, true);

    // ####################################################################################
    function onPointerDown(ev) {
      relayout("onPointerDown");

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
        timestamp: Date.now(),
      });

      ev.target.classList.add(INJECTED_STYLE_CLASS_DOWN);
    }
    window.document.addEventListener("pointerdown", onPointerDown, true);

    // ####################################################################################
    function onPointerMove(ev) {
      ev.stopPropagation();
      relayout("onPointerMove");

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] DOCUMENT EVENT: "pointermove" ${ev.pointerId} (${ev.x}, ${ev.y})`,
      //   );

      // if (DEBUG)
      //   console.log(
      //     `[${NAME}] data log: ${JSON.stringify(_touchDowns, null, 4)}`,
      //   );

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
      relayout("onPointerUp");

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

      let doubleTap = false;
      const now = Date.now();
      if (_touchDowns.length !== 2) {
        _doubleTap = undefined;
      } else if (_doubleTap) {
        const delta = now - _doubleTap;
        if (delta > 500) {
          _doubleTap = now;
        } else {
          _doubleTap = undefined;
          doubleTap = true;
        }
      } else {
        _doubleTap = now;
      }

      const scrolling = _touchDowns.length === 1;
      const zooming = _touchDowns.length === 2 && !doubleTap;
      const cancelling = _touchDowns.length >= 3;
      if (scrolling || zooming || cancelling) {
        _touchDowns.splice(0, _touchDowns.length);

        if (scrolling || zooming) {
          if (DEBUG)
            console.log(
              `[${NAME}] 1-finger SCROLL or 2-finger ZOOM ==> PRESERVE WRAP FIT`,
            );
          return;
        }

        const elementsFit = document.documentElement.querySelectorAll(
          `.${INJECTED_STYLE_CLASS_FIT}`,
        );
        elementsFit?.forEach((element) => {
          element.classList.remove(INJECTED_STYLE_CLASS_FIT);
        });

        document.documentElement.style.zoom = 1;

        if (DEBUG) console.log(`[${NAME}] 3+ finger TAP ==> CANCEL WRAP FIT`);
        return;
      }

      // _touchDowns.length === 2 && doubleTap

      if (DEBUG) console.log(`[${NAME}] double 2-finger TAP ==> WRAP!`);

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
      }, 2000);

      const zoom = document.documentElement.style.zoom || 1;
      const maxWidth = round(window.visualViewport.width * 0.96);
      document.documentElement.style.setProperty(
        "--window-visualViewport-width",
        `${round(maxWidth / zoom)}px`,
      );

      const rect = element.getBoundingClientRect();
      if (DEBUG)
        console.log(
          `[${NAME}] documentElement.style.zoom: ${document.documentElement.style.zoom}`,
        );
      if (DEBUG) console.log(`[${NAME}] maxWidth: ${maxWidth}`);
      if (DEBUG) console.log(`[${NAME}] rect.width: ${rect.width}`);
      if (Math.round(maxWidth) - Math.round(rect.width) > 10) {
        const ratio = maxWidth / rect.width;
        if (DEBUG) console.log(`[${NAME}] ratio: ${ratio}`);
        document.documentElement.style.zoom =
          ratio * (document.documentElement.style.zoom || 1);
      }

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
          block: "center",
          inline: "center",
        });
        relayout("scrollIntoView");

        if (!!timeout_INJECTED_STYLE_CLASS_MARGIN_remove) {
          window.clearTimeout(timeout_INJECTED_STYLE_CLASS_MARGIN_remove);
          timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
        }
        timeout_INJECTED_STYLE_CLASS_MARGIN_remove = setTimeout(() => {
          timeout_INJECTED_STYLE_CLASS_MARGIN_remove = undefined;
          element.classList.remove(INJECTED_STYLE_CLASS_MARGIN);
          relayout("timeout_INJECTED_STYLE_CLASS_MARGIN_remove");
        }, 500);
      }, 500);
    }
    window.document.addEventListener("pointerup", onPointerUp, true);

    window.addEventListener("scroll", function () {
      if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "scroll"`);
      relayout("scroll");
    });

    window.visualViewport.addEventListener("resize", function () {
      relayout("visualViewport resize");

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
      relayout("window resize");

      // if (DEBUG) console.log(`[${NAME}] WINDOW EVENT: "resize"`);
    });

    let tearDownMediaQuery = undefined;
    const updatePixelRatio = () => {
      relayout("updatePixelRatio");

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
