/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
/**
 * Data format for nodes territories.
 * NOTE: this is a frontend wrapper for drawing, actual chunks data is
 * stored in territory object handled in wasm.
 */
export class NodesTerritory {
    static defaultProps = Object.freeze({
        id: undefined,
        name: "", // territory readable name identifier
        core: undefined, // {x: x, y: y}
        coreChunk: undefined, // {x: x, y: y}
        borders: undefined, // borders for rendering
        size: 0, // chunks.length
        neighbors: [], // neighboring ids
        isEdge: false, // territory borders wilderness
        nodes: [], // array of node type names
        terrElement: undefined, // react jsx element to render
        town: undefined, // link to town object
        occupier: undefined, // link to occupying town object
        color: undefined, // integer id to a color index, used by client
        cost: 0, // territory power cost


        // editor internal variables
        selected: false,
    });

    constructor(id) {
        Object.assign(this, NodesTerritory.defaultProps, {
            id: id,
        });
        Object.seal(this);
    }

    /**
     * TODO
     */
    export() {
        return this;
    }
}
