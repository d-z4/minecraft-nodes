/**
 * 1.13+ Player town nametag
 *
 * NOTE: this conflicts with any other plugin doing nametag prefix/suffix (e.g. TAB)
 * Make sure all other plugins that affect prefix/suffix are disabled
 *
 * TODO: make sure name is not too long (may cause bukkit error)
 */

package phonon.nodes.objects

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import phonon.nodes.Config
import phonon.nodes.Nodes
import phonon.nodes.nms.sendTeamAddPlayers
import phonon.nodes.nms.sendTeamCreate
import phonon.nodes.nms.sendTeamRemove

/**
 * Get armor stand custom name as VIEWED by input player
 */
public fun townNametagViewedByPlayer(town: Town, viewer: Player): String {
    // get input player relation to this.player
    val otherTown = Nodes.getResident(viewer)?.town
    if (otherTown !== null) {
        if (town === otherTown) {
            return town.nametagTown
        } else if (town.nation !== null && town.nation === otherTown.nation) {
            return town.nametagNation
        } else if (town.allies.contains(otherTown)) {
            return town.nametagAlly
        } else if (town.enemies.contains(otherTown)) {
            return town.nametagEnemy
        }
    }

    return town.nametagNeutral
}

public object Nametag {

    // lock for pipelined nametag update
    private var updateLock: Boolean = false

    /**
     * Update nametag text for player
     * Sends team packets directly to the client
     */
    public fun updateTextForPlayer(player: Player) {
        // unregister towns
        for (town in Nodes.towns.values) {
            val townNametagId = "t${town.townNametagId}"
            try {
                player.sendTeamRemove(townNametagId)
            } catch (e: Exception) {
                // ignore if team doesn't exist
            }
        }

        // re create teams from town names
        for (town in Nodes.towns.values) {
            val townNametagId = "t${town.townNametagId}"
            val prefix = townNametagViewedByPlayer(town, player)

            // create the team with town prefix
            player.sendTeamCreate(townNametagId, prefix, "")
        }

        // add other players to teams
        for (otherPlayer in Bukkit.getOnlinePlayers()) {
            val town = Nodes.getTownFromPlayer(otherPlayer)
            if (town !== null) {
                val townNametagId = "t${town.townNametagId}"
                player.sendTeamAddPlayers(townNametagId, listOf(otherPlayer.name))
            }
        }
    }

    /**
     * Update all player nametags using a pipeline:
     * - only update subset of online players each time
     */
    public fun pipelinedUpdateAllText() {
        if (Nametag.updateLock == true) {
            return
        }

        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        if (onlinePlayers.size <= 0) {
            return
        }

        Nametag.updateLock = true

        val updatesPerTick: Int = Math.max(1, Math.ceil(onlinePlayers.size.toDouble() / Config.nametagPipelineTicks.toDouble()).toInt())
        var index = 0
        var tickOffset = 1L // folia requires delay > 0

        while (index < onlinePlayers.size) {
            val idxStart = index
            val idxEnd = Math.min(index + updatesPerTick, onlinePlayers.size)
            Bukkit.getGlobalRegionScheduler().runDelayed(
                Nodes.plugin!!,
                { _ ->
                    for (i in idxStart until idxEnd) {
                        val player = onlinePlayers[i]
                        if (player.isOnline()) {
                            Nametag.updateTextForPlayer(player)
                        }
                    }
                },
                tickOffset,
            )

            index += updatesPerTick
            tickOffset += 1L
        }

        // finish after next tick
        Bukkit.getGlobalRegionScheduler().runDelayed(
            Nodes.plugin!!,
            { _ ->
                Nametag.updateLock = false
            },
            tickOffset,
        )
    }
}
