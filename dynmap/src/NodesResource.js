/**
 * nodes.js
 * --------------------------------------------------
 * TODO: better comments + consistent style
 */
"use strict";
/**
 * Required format properties for nodes resources.
 * Note: resources are fully configurable objects (so in future other
 * libraries can extend definitions in resources). So, the format
 * is left unfrozen here.
 */
export class NodesResource {
    static defaultProps = Object.freeze({
        name: undefined,
        icon: null,
        cost: {
            scale: 1.0,
            constant: 0,
        },
        priority: 0,
    });

    constructor(options = {}) {
        Object.assign(this, NodesResource.defaultProps, options);
    }

    /**
     * Return game compatible object for json serialization.
     * For resources, return everything (allow full customization in
     * editor).
     */
    export() {
        return this;
    }
}
