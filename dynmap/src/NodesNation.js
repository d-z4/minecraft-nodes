/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
import { v4 as uuidv4 } from "uuid";

/**
 * Data format for nodes nation objects.
 */
export class NodesNation {
    static defaultProps = Object.freeze({
        uuid: undefined,
        capital: undefined,
        color: [255, 255, 255],
        towns: [],
        allies: [],
        enemies: [],

        // editor state
        numPlayers: 0,
        numTerritories: 0,
    });

    constructor(options = {}) {
        Object.assign(this, NodesNation.defaultProps, options);
        this.original = options;
        Object.seal(this);

        // if uuid is undefined, generate new random uuid
        this.uuid = this.uuid ?? uuidv4();
    }

    /**
     * Return in-game plugin compatible nation data object.
     * Return original object overwritten with editor editable
     * properties.
     */
    export() {
        return Object.assign({}, this.original, {
            uuid: this.uuid,
            capital: this.capital,
            color: this.color,
            towns: this.towns,
            allies: this.allies,
            enemies: this.enemies,
        });
    }
}
