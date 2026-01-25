/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
import { v4 as uuidv4 } from "uuid";

/**
 * Format for a nodes Town object.
 */
export class NodesTown {
    static defaultProps = Object.freeze({
        uuid: undefined,
        name: undefined,
        color: [255, 255, 255],
        colorTown: [255, 255, 255],
        colorNation: [255, 255, 255],

        // flag that anyone can join
        open: false,

        // residents
        leader: undefined,
        playerNames: [],
        residents: [],
        residentUuids: [],

        // territories
        territories: [],
        annexed: [],
        captured: [],
        home: -1,
        spawn: [0.0, 0.0, 0.0],

        // relations with other towns
        allies: [],
        enemies: [],
        truce: [],

        // nation
        nation: undefined,
    });

    constructor(options = {}) {
        Object.assign(this, NodesTown.defaultProps, options);
        this.original = options; // store copy of original, for export
        Object.seal(this);

        // if uuid is undefined, generate new random uuid
        this.uuid = this.uuid ?? uuidv4();
    }

    /**
     * Return in-game plugin compatible town data object.
     * Returns original object which may contain in-game specific
     * properties not used in the editor. Then overwrite original
     * with exporter editable properties.
     */
    export() {
        return Object.assign({}, this.original, {
            uuid: this.uuid,
            color: this.colorTown,
            open: this.open,
            leader: this.leader,
            residents: this.residentUuids,
            territories: this.territories,
            annexed: this.annexed,
            captured: this.captured,
            home: this.home,
            spawn: this.spawn,
            allies: this.allies,
            enemies: this.enemies,
            truce: this.truce,
            income: this.income,
            incomeEgg: this.incomeEgg,
            claimsBonus: this.claimsBonus,
            claimsPenalty: this.claimsPenalty,
        });
    }
}
