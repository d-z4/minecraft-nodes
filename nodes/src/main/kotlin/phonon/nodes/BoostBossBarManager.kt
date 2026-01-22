/**
 * Manages boss bars for displaying active mining boosts
 */
package phonon.nodes

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import phonon.nodes.objects.Boost
import phonon.nodes.objects.BoostType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages boss bars showing active boosts to players
 */
object BoostBossBarManager {

    // Boss bars for each player, mapped by player UUID and boost type
    private val playerBossBars = ConcurrentHashMap<UUID, MutableMap<BoostType, BossBar>>()

    /**
     * Update boss bars for a player based on their active boosts
     */
    fun updatePlayerBossBars(player: Player) {
        if (!Config.boostEnabled) {
            removePlayerBossBars(player)
            return
        }

        val boosts = BoostManager.getBoostsForPlayer(player)
        val playerBars = playerBossBars.getOrPut(player.uniqueId) { mutableMapOf() }

        // Get or create boss bars for each active boost
        for ((type, boost) in boosts) {
            val bossBar = playerBars.getOrPut(type) {
                createBossBar(type)
            }

            // Update boss bar
            updateBossBar(bossBar, type, boost, player)

            // Add player to boss bar if not already added
            if (!bossBar.players.contains(player)) {
                bossBar.addPlayer(player)
            }
        }

        // Remove boss bars for boosts that are no longer active
        val inactiveTypes = playerBars.keys.filter { it !in boosts.keys }
        for (type in inactiveTypes) {
            val bossBar = playerBars.remove(type)
            bossBar?.removeAll()
        }
    }

    /**
     * Create a new boss bar for a boost type
     */
    private fun createBossBar(type: BoostType): BossBar {
        val color = when (type) {
            BoostType.SERVER -> BarColor.GREEN
            BoostType.TOWN -> BarColor.YELLOW
            BoostType.NATION -> BarColor.BLUE
        }

        return Bukkit.createBossBar(
            "Loading...",
            color,
            BarStyle.SOLID,
        )
    }

    /**
     * Update boss bar text and progress
     */
    private fun updateBossBar(bossBar: BossBar, type: BoostType, boost: Boost, player: Player) {
        // Get title based on type
        val title = when (type) {
            BoostType.SERVER -> "World mining boost: ${boost.multiplier.toInt()}x"
            BoostType.TOWN -> {
                val town = Nodes.getResident(player)?.town
                "${town?.name ?: "Town"} mining boost: ${boost.multiplier.toInt()}x"
            }
            BoostType.NATION -> {
                val nation = Nodes.getResident(player)?.town?.nation
                "${nation?.name ?: "Nation"} mining boost: ${boost.multiplier.toInt()}x"
            }
        }

        bossBar.setTitle(title)

        // Calculate progress (time spent / total duration)
        val timeSpent = System.currentTimeMillis() - boost.startTime
        val progress = (timeSpent.toDouble() / boost.duration.toDouble()).coerceIn(0.0, 1.0)
        bossBar.progress = progress
    }

    /**
     * Remove all boss bars for a player
     */
    fun removePlayerBossBars(player: Player) {
        val bars = playerBossBars.remove(player.uniqueId)
        if (bars != null) {
            for (bossBar in bars.values) {
                bossBar.removePlayer(player)
                if (bossBar.players.isEmpty()) {
                    bossBar.removeAll()
                }
            }
        }
    }

    /**
     * Update boss bars for all online players
     */
    fun updateAllPlayerBossBars() {
        for (player in Bukkit.getOnlinePlayers()) {
            updatePlayerBossBars(player)
        }
    }

    /**
     * Clean up all boss bars
     */
    fun cleanup() {
        for (bars in playerBossBars.values) {
            for (bossBar in bars.values) {
                bossBar.removeAll()
            }
        }
        playerBossBars.clear()
    }
}
