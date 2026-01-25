/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
/**
 * Data format for nodes port objects.
 */
export class NodesPort {
    constructor(name, group, x, z) {
        this.name = name;
        this.group = group;
        this.groupsString = group.join(", ");
        this.x = x;
        this.z = z;
        this.owner = undefined;

        // Define these here so they aren't "new" properties later cause react is confused
        this.showPorts = true;
        this.portVisible = true;
    }
}
