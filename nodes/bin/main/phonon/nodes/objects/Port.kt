/**
 * Port objects and data structures
 */

package phonon.nodes.objects

import phonon.nodes.serdes.SaveState

/**
 * Player warpable port
 */
data class Port(
    val name: String,
    val locX: Int,
    val locZ: Int,
    val groups: HashSet<PortGroup> = hashSetOf(),
    val isPublic: Boolean,
) {
    val chunkX = Math.floorDiv(locX, 16)
    val chunkZ = Math.floorDiv(locZ, 16)

    // json string and memoization flag
    private var saveState: PortSaveState

    @Suppress("PropertyName")
    private var _needsUpdate = false

    init {
        this.saveState = PortSaveState(this)
    }

    /**
     * Port save state for JSON serialization
     */
    public class PortSaveState(p: Port) : SaveState {
        public val name = p.name
        public val locX = p.locX
        public val locZ = p.locZ
        public val groups = p.groups
        public val isPublic = p.isPublic

        public override var jsonString: String? = null

        public override fun createJsonString(): String {
            // serialize groups as JSON array of strings
            val groupsJson = groups.joinToString(",", "[", "]") { "\"${it.name}\"" }

            val jsonString = (
                "{" +
                    "\"name\":\"${this.name}\"," +
                    "\"x\":$locX," +
                    "\"z\":$locZ," +
                    "\"groups\":$groupsJson," +
                    "\"isPublic\":$isPublic" +
                    "}"
                )

            return jsonString
        }

        public override fun hashCode(): Int = this.name.hashCode()
    }

    // function to let client flag this object as dirty
    public fun needsUpdate() {
        this._needsUpdate = true
    }

    // wrapper to return self as savestate
    // - returns memoized copy if needsUpdate false
    // - otherwise, parses self
    public fun getSaveState(): PortSaveState {
        if (this._needsUpdate) {
            this.saveState = PortSaveState(this)
            this._needsUpdate = false
        }
        return this.saveState
    }
}

/**
 * Group of ports
 */
data class PortGroup(
    val name: String,
) {
    // json string and memoization flag
    private var saveState: PortGroupSaveState

    @Suppress("PropertyName")
    private var _needsUpdate = false

    init {
        this.saveState = PortGroupSaveState(this)
    }

    /**
     * Port group save state for JSON serialization
     */
    public class PortGroupSaveState(p: PortGroup) : SaveState {
        public val name = p.name

        public override var jsonString: String? = null

        public override fun createJsonString(): String {
            val jsonString = (
                "{" +
                    "\"name\":\"${this.name}\"" +
                    "}"
                )

            return jsonString
        }

        public override fun hashCode(): Int = this.name.hashCode()
    }

    // function to let client flag this object as dirty
    public fun needsUpdate() {
        this._needsUpdate = true
    }

    // wrapper to return self as savestate
    // - returns memoized copy if needsUpdate false
    // - otherwise, parses self
    public fun getSaveState(): PortGroupSaveState {
        if (this._needsUpdate) {
            this.saveState = PortGroupSaveState(this)
            this._needsUpdate = false
        }
        return this.saveState
    }
}
