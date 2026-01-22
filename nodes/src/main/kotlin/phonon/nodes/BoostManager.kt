/**
 * Manager for handling mining boosts from micropayments
 */
package phonon.nodes

import org.bukkit.entity.Player
import phonon.nodes.objects.Boost
import phonon.nodes.objects.BoostSaveState
import phonon.nodes.objects.BoostType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active mining boosts for server/towns/nations
 */
object BoostManager {

    // Active boosts mapped by ID
    private val activeBoosts = ConcurrentHashMap<UUID, Boost>()

    // Dirty flag for save system
    @Volatile
    var dirty: Boolean = false

    /**
     * Add a new boost
     */
    fun addBoost(boost: Boost) {
        activeBoosts[boost.id] = boost
        dirty = true
    }

    /**
     * Remove a boost by ID
     */
    fun removeBoost(id: UUID): Boolean {
        val removed = activeBoosts.remove(id) != null
        if (removed) {
            dirty = true
        }
        return removed
    }

    /**
     * Get all active boosts
     */
    fun getActiveBoosts(): List<Boost> = activeBoosts.values.toList()

    /**
     * Get active boosts of a specific type
     */
    fun getActiveBoostsByType(type: BoostType): List<Boost> = activeBoosts.values.filter { it.type == type && it.isActive() }

    /**
     * Get active boosts for a specific town
     */
    fun getActiveBoostsForTown(townUuid: UUID): List<Boost> = activeBoosts.values.filter {
        it.isActive() && (it.type == BoostType.SERVER || (it.type == BoostType.TOWN && it.targetId == townUuid))
    }

    /**
     * Get active boosts for a specific nation
     */
    fun getActiveBoostsForNation(nationUuid: UUID): List<Boost> = activeBoosts.values.filter {
        it.isActive() && (it.type == BoostType.SERVER || (it.type == BoostType.NATION && it.targetId == nationUuid))
    }

    /**
     * Clean up expired boosts
     */
    fun cleanupExpiredBoosts(): Int {
        val initialSize = activeBoosts.size
        activeBoosts.values.removeIf { !it.isActive() }
        val removed = initialSize - activeBoosts.size
        if (removed > 0) {
            dirty = true
        }
        return removed
    }

    /**
     * Get the total drop rate multiplier for a player
     * Add all applicable boosts together (server + nation + town)
     */
    fun getDropRateMultiplier(player: Player): Double {
        val resident = Nodes.getResident(player) ?: return 1.0
        val town = resident.town
        val nation = town?.nation

        var total = 1.0

        // Check all active boosts and multiply them together
        for (boost in activeBoosts.values) {
            if (!boost.isActive()) continue

            val applies = when (boost.type) {
                BoostType.SERVER -> true
                BoostType.TOWN -> town != null && boost.targetId == town.uuid
                BoostType.NATION -> nation != null && boost.targetId == nation.uuid
            }

            if (applies) {
                total += boost.multiplier
            }
        }

        return total
    }

    /**
     * Get server-wide boost multiplier
     */
    fun getServerBoostMultiplier(): Double = activeBoosts.values
        .filter { it.type == BoostType.SERVER && it.isActive() }
        .maxOfOrNull { it.multiplier } ?: 1.0

    /**
     * Get town-specific boost multiplier
     */
    fun getTownBoostMultiplier(townUuid: UUID): Double {
        var maxMultiplier = getServerBoostMultiplier()

        activeBoosts.values
            .filter { it.type == BoostType.TOWN && it.targetId == townUuid && it.isActive() }
            .forEach { boost ->
                if (boost.multiplier > maxMultiplier) {
                    maxMultiplier = boost.multiplier
                }
            }

        return maxMultiplier
    }

    /**
     * Get nation-specific boost multiplier
     */
    fun getNationBoostMultiplier(nationUuid: UUID): Double {
        var maxMultiplier = getServerBoostMultiplier()

        activeBoosts.values
            .filter { it.type == BoostType.NATION && it.targetId == nationUuid && it.isActive() }
            .forEach { boost ->
                if (boost.multiplier > maxMultiplier) {
                    maxMultiplier = boost.multiplier
                }
            }

        return maxMultiplier
    }

    /**
     * Internal helper to create or extend a boost
     */
    private fun createOrExtendBoost(
        type: BoostType,
        targetId: UUID?,
        multiplier: Double,
        durationSeconds: Int,
        purchaser: UUID?,
    ): Boost {
        // Check for existing boost
        val existing = activeBoosts.values.find {
            it.type == type && it.targetId == targetId && it.isActive()
        }

        if (existing != null) {
            // Extend existing boost duration
            val newBoost = existing.copy(
                duration = existing.duration + (durationSeconds * 1000L),
            )
            activeBoosts[existing.id] = newBoost
            dirty = true
            return newBoost
        }

        // Create new boost
        val boost = Boost(
            id = UUID.randomUUID(),
            type = type,
            multiplier = multiplier,
            targetId = targetId,
            startTime = System.currentTimeMillis(),
            duration = durationSeconds * 1000L,
            purchaser = purchaser,
        )
        addBoost(boost)
        return boost
    }

    /**
     * Create a server-wide boost or extend existing one
     */
    fun createServerBoost(multiplier: Double, durationSeconds: Int, purchaser: UUID?): Boost = createOrExtendBoost(BoostType.SERVER, null, multiplier, durationSeconds, purchaser)

    /**
     * Create a town-specific boost or extend existing one
     */
    fun createTownBoost(townUuid: UUID, multiplier: Double, durationSeconds: Int, purchaser: UUID?): Boost = createOrExtendBoost(BoostType.TOWN, townUuid, multiplier, durationSeconds, purchaser)

    /**
     * Create a nation-specific boost or extend existing one
     */
    fun createNationBoost(nationUuid: UUID, multiplier: Double, durationSeconds: Int, purchaser: UUID?): Boost = createOrExtendBoost(BoostType.NATION, nationUuid, multiplier, durationSeconds, purchaser)

    /**
     * Clear all boosts
     */
    fun clearAllBoosts() {
        activeBoosts.clear()
        dirty = true
    }

    /**
     * Load boosts from save state
     */
    fun loadBoosts(boosts: List<Boost>) {
        activeBoosts.clear()
        for (boost in boosts) {
            if (boost.isActive()) {
                activeBoosts[boost.id] = boost
            }
        }
        dirty = false
    }

    /**
     * Get save state for all boosts
     */
    fun getSaveState(): List<BoostSaveState> = activeBoosts.values
        .filter { it.isActive() }
        .map { boost ->
            BoostSaveState(
                id = boost.id.toString(),
                type = boost.type.name,
                multiplier = boost.multiplier,
                targetId = boost.targetId?.toString(),
                startTime = boost.startTime,
                duration = boost.duration,
                purchaser = boost.purchaser?.toString(),
            )
        }

    /**
     * Get all boosts applicable to a player (for boss bar display)
     * Returns map of BoostType -> Boost
     */
    fun getBoostsForPlayer(player: Player): Map<BoostType, Boost> {
        val resident = Nodes.getResident(player)
        val town = resident?.town
        val nation = town?.nation

        val boosts = mutableMapOf<BoostType, Boost>()

        for (boost in activeBoosts.values) {
            if (!boost.isActive()) continue

            when (boost.type) {
                BoostType.SERVER -> {
                    boosts[BoostType.SERVER] = boost
                }
                BoostType.TOWN -> {
                    if (town != null && boost.targetId == town.uuid) {
                        boosts[BoostType.TOWN] = boost
                    }
                }
                BoostType.NATION -> {
                    if (nation != null && boost.targetId == nation.uuid) {
                        boosts[BoostType.NATION] = boost
                    }
                }
            }
        }

        return boosts
    }

    /**
     * Get active server boost
     */
    fun getServerBoost(): Boost? = activeBoosts.values.find {
        it.type == BoostType.SERVER && it.isActive()
    }

    /**
     * Get active town boost for a specific town UUID
     */
    fun getTownBoost(townUuid: UUID): Boost? = activeBoosts.values.find {
        it.type == BoostType.TOWN && it.targetId == townUuid && it.isActive()
    }

    /**
     * Get active nation boost for a specific nation UUID
     */
    fun getNationBoost(nationUuid: UUID): Boost? = activeBoosts.values.find {
        it.type == BoostType.NATION && it.targetId == nationUuid && it.isActive()
    }
}
