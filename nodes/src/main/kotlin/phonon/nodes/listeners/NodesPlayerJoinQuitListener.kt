package phonon.nodes.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import phonon.nodes.Config
import phonon.nodes.Nodes
import phonon.nodes.chat.Chat
import phonon.nodes.objects.Nametag
import phonon.nodes.objects.Resident

class NodesPlayerJoinQuitListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Create resident if it doesn't exist yet
        Nodes.createResident(player)

        val resident = Nodes.getResident(player) ?: return  // should never be null after createResident, but safe-guard

        // Rally cap check (only if enabled)
        if (Config.rallyCapEnabled) {
            val town = resident.town

            if (town != null) {
                val entityName: String
                val onlineCount: Int

                if (Config.rallyCapApplyToNations && town.nation != null) {
                    val nation = town.nation!!
                    entityName = nation.name
                    onlineCount = nation.playersOnline.size
                } else {
                    entityName = town.name
                    onlineCount = town.playersOnline.size
                }

                if (onlineCount >= Config.rallyCapSize) {
                    player.kick(
                        Component.text()
                            .color(NamedTextColor.RED)
                            .content("Rally cap reached! Maximum ${Config.rallyCapSize} players online for $entityName")
                            .build()
                    )
                    return
                }
            }
        }

        // Mark player as online
        Nodes.setResidentOnline(resident, player)

        // War-related features
        if (Nodes.war.enabled) {
            Nodes.war.sendWarProgressBarToPlayer(player)

            // Optional: you might also want to re-check/resend attack timers etc. here
        }

        // Update all visible nametags (usually expensive → only when needed)
        Nametag.pipelinedUpdateAllText()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val resident = Nodes.getResident(player) ?: return

        resident.destroyMinimap()
        Nodes.setResidentOffline(resident, player)

        // Re-enable global chat if player was muting it
        Chat.enableGlobalChat(player)

        // Cancel any ongoing chunk attacks by this player
        if (Nodes.war.enabled) {
            Nodes.war.attackers[player.uniqueId]?.forEach { attack ->
                attack.cancel()
            }
            // Optional: clean up the map entry if empty
            // Nodes.war.attackers.remove(player.uniqueId)
        }
    }
}