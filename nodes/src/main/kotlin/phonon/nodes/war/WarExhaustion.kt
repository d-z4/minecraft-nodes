/**
 * NEW FILE: WarExhaustion.kt
 *
 * Create this file in: phonon/nodes/war/WarExhaustion.kt
 *
 * War exhaustion system that tracks kills and applies debuffs
 */

package phonon.nodes.war

import org.bukkit.ChatColor
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import phonon.nodes.Config
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.objects.Nation
import phonon.nodes.objects.Town
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks war kills and applies exhaustion debuffs
 */
public object WarExhaustion {

    // Map of Town UUID -> kill count
    private val townKills: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()

    // Map of Nation UUID -> kill count
    private val nationKills: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()

    /**
     * Called when a player dies during war
     */
    public fun onPlayerDeath(victimUUID: UUID) {
        if (!Config.warExhaustionEnabled || !FlagWar.enabled) {
            return
        }

        val victim = Nodes.getResidentFromUUID(victimUUID) ?: return
        val victimTown = victim.town ?: return
        val victimNation = victimTown.nation

        // Increment town kill count
        val townKillCount = townKills.getOrDefault(victimTown.uuid, 0) + 1
        townKills[victimTown.uuid] = townKillCount

        // Increment nation kill count if applicable
        if (victimNation != null && Config.rallyCapApplyToNations) {
            val nationKillCount = nationKills.getOrDefault(victimNation.uuid, 0) + 1
            nationKills[victimNation.uuid] = nationKillCount
        }

        // Check if town has reached exhaustion threshold
        checkAndApplyExhaustion(victimTown, townKillCount)

        // Check nation exhaustion if applicable
        if (victimNation != null && Config.rallyCapApplyToNations) {
            checkAndApplyNationExhaustion(victimNation, nationKills.getOrDefault(victimNation.uuid, 0))
        }
    }

    /**
     * Check if town has reached exhaustion and apply debuffs
     */
    private fun checkAndApplyExhaustion(town: Town, killCount: Int) {
        val onlineCount = town.playersOnline.size

        if (onlineCount == 0) return

        val threshold = onlineCount * Config.warExhaustionDeathMultiplier

        if (killCount >= threshold) {
            // Apply weakness to all online town members
            for (player in town.playersOnline) {
                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.WEAKNESS,
                        Config.warExhaustionWeaknessDuration,
                        Config.warExhaustionWeaknessLevel,
                        false,
                        true,
                        true,
                    ),
                )

                Message.print(
                    player,
                    "${ChatColor.DARK_RED}${ChatColor.BOLD}Your town is suffering from war exhaustion! ($killCount/$threshold deaths)",
                )
            }

            // Broadcast to server
            Message.broadcast(
                "${ChatColor.DARK_RED}${ChatColor.BOLD}${town.name} is suffering from war exhaustion! ($killCount/$threshold deaths)",
            )
        }
    }

    /**
     * Check if nation has reached exhaustion and apply debuffs
     */
    private fun checkAndApplyNationExhaustion(nation: Nation, killCount: Int) {
        var totalOnline = 0

        for (town in nation.towns) {
            totalOnline += town.playersOnline.size
        }

        if (totalOnline == 0) return

        val threshold = totalOnline * Config.warExhaustionDeathMultiplier

        if (killCount >= threshold) {
            // Apply weakness to all online nation members
            for (town in nation.towns) {
                for (player in town.playersOnline) {
                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.WEAKNESS,
                            Config.warExhaustionWeaknessDuration,
                            Config.warExhaustionWeaknessLevel,
                            false,
                            true,
                            true,
                        ),
                    )

                    Message.print(
                        player,
                        "${ChatColor.DARK_RED}${ChatColor.BOLD}Your nation is suffering from war exhaustion! ($killCount/$threshold deaths)",
                    )
                }
            }

            // Broadcast to server
            Message.broadcast(
                "${ChatColor.DARK_RED}${ChatColor.BOLD}${nation.name} is suffering from war exhaustion! ($killCount/$threshold deaths)",
            )
        }
    }

    /**
     * Get current kill count for a town
     */
    public fun getTownKills(townUUID: UUID): Int = townKills.getOrDefault(townUUID, 0)

    /**
     * Get current kill count for a nation
     */
    public fun getNationKills(nationUUID: UUID): Int = nationKills.getOrDefault(nationUUID, 0)

    /**
     * Reset all kill counts (call when war ends)
     */
    public fun reset() {
        townKills.clear()
        nationKills.clear()
    }

    /**
     * Reset kill count for specific town
     */
    public fun resetTown(townUUID: UUID) {
        townKills.remove(townUUID)
    }

    /**
     * Reset kill count for specific nation
     */
    public fun resetNation(nationUUID: UUID) {
        nationKills.remove(nationUUID)
    }

    /**
     * Get exhaustion status for a town
     */
    public fun getTownExhaustionStatus(town: Town): String {
        val killCount = getTownKills(town.uuid)
        val onlineCount = town.playersOnline.size
        val threshold = onlineCount * Config.warExhaustionDeathMultiplier

        return "$killCount / $threshold deaths"
    }

    /**
     * Get exhaustion status for a nation
     */
    public fun getNationExhaustionStatus(nation: Nation): String {
        val killCount = getNationKills(nation.uuid)
        var totalOnline = 0

        for (town in nation.towns) {
            totalOnline += town.playersOnline.size
        }

        val threshold = totalOnline * Config.warExhaustionDeathMultiplier

        return "$killCount / $threshold deaths"
    }
}
