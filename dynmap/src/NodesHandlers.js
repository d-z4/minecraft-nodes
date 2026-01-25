/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
import Nodes from "./nodes.js";

// painting event handlers
export const handleWindowMouseUp = (e) => {
    Nodes._stopPaint();
    window.removeEventListener("mouseup", handleWindowMouseUp);
};
export const handleMouseDown = (e) => {
    e.preventDefault();
    // e.stopPropagation(); // problem: nodes pane is overlaid on top, prevents dragging map
    if (e.button === 2) { // right click only
        Nodes._startPaint();

        // add window event for mouse up
        window.addEventListener("mouseup", handleWindowMouseUp);
    }
};
export const handleMouseUp = (e) => {
    Nodes._stopPaint();
    window.removeEventListener("mouseup", handleWindowMouseUp);
};
