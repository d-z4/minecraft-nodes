/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
import { v4 as uuidv4 } from "uuid";
import { RESIDENT_RANK_NONE } from "./constants";

/**
 * Nodes resident object data format, holds resident uuid and rank.
 * For rank format just use pre-defined magic number ints,
 * defined in `Nodes` object.
 *
 * Leave this unsealed so that ingame specific properties are retained
 * and exported.
 */
export class NodesResident {
    // required properties
    static defaultProps = Object.freeze({
        uuid: undefined,
        name: "Anonymous",
        prefix: "",
        suffix: "",
        town: undefined,
        nation: undefined,
        rank: RESIDENT_RANK_NONE,
    });

    constructor(options = {}) {
        Object.assign(this, NodesResident.defaultProps, options);

        // if uuid is undefined, generate new random uuid
        this.uuid = this.uuid ?? uuidv4();
    }

    /**
     * Return everything for export. This may contain in-game specific
     * properties appended to editor required properties.
     */
    export() {
        return this;
    }
}
