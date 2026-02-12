package phonon.nodes.nametags

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import phonon.nodes.objects.Nametag

/**
 * Listens for player events and updates tab list
 */
class NametagListener(private val plugin: JavaPlugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Update TAB list after a short delay to ensure everything is loaded
        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                // Update tab list (new RGB system)
                TabIntegration.updateTabForPlayer(player)

                // Update nametags above heads (existing system)
                Nametag.updateTextForPlayer(player)
            },
            5L,
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Tab plugin handles its own cleanup
    }
}
