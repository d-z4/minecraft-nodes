/**
 * Boost system for micropayments
 * Allows players to purchase temporary mining boosts for server/town/nation
 */
package phonon.nodes.objects

import java.util.UUID

/**
 * Types of boosts available
 */
enum class BoostType {
    SERVER, // Affects all players on the server
    TOWN, // Affects all residents of a specific town
    NATION, // Affects all residents of a specific nation
}

/**
 * Represents an active boost
 */
data class Boost(
    val id: UUID, // Unique identifier
    val type: BoostType, // Type of boost
    val multiplier: Double, // Drop rate multiplier (e.g., 10.0 for 10x)
    val targetId: UUID?, // Town/Nation UUID (null for SERVER type)
    val startTime: Long, // System time when boost started (milliseconds)
    val duration: Long, // Duration in milliseconds
    val purchaser: UUID?, // UUID of player who purchased (optional)
) {
    /**
     * Check if this boost is still active
     */
    fun isActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime < (startTime + duration)
    }

    /**
     * Get remaining time in milliseconds
     */
    fun getRemainingTime(): Long {
        val currentTime = System.currentTimeMillis()
        val endTime = startTime + duration
        return maxOf(0, endTime - currentTime)
    }

    /**
     * Get remaining time in seconds
     */
    fun getRemainingSeconds(): Long = getRemainingTime() / 1000

    /**
     * Check if this boost applies to a specific town
     */
    fun appliesToTown(townUuid: UUID): Boolean = when (type) {
        BoostType.SERVER -> true
        BoostType.TOWN -> targetId == townUuid
        BoostType.NATION -> false // Handled separately via nation lookup
    }

    /**
     * Check if this boost applies to a specific nation
     */
    fun appliesToNation(nationUuid: UUID): Boolean = when (type) {
        BoostType.SERVER -> true
        BoostType.TOWN -> false
        BoostType.NATION -> targetId == nationUuid
    }
}

/**
 * Configuration for boost types
 */
data class BoostConfig(
    val enabled: Boolean,
    val multiplier: Double,
    val durationSeconds: Int,
    val price: Double,
)

/**
 * Save state for boost serialization
 */
data class BoostSaveState(
    val id: String,
    val type: String,
    val multiplier: Double,
    val targetId: String?,
    val startTime: Long,
    val duration: Long,
    val purchaser: String?,
)
