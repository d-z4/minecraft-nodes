package phonon.nodes.tasks

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.commands.progressBar
import phonon.nodes.commands.warpToPort
import phonon.nodes.objects.Port

/**
 * Task for running warp
 */
public class PortWarpTask(
    val player: Player,
    val destination: Port,
    val playersToWarp: List<Player>,
    val entitiesToWarp: List<Entity>,
    val initialLoc: Location,
    val timeWarp: Double,
    val tick: Double,
) : BukkitRunnable() {
    private val locX = initialLoc.getBlockX()
    private val locY = initialLoc.getBlockY()
    private val locZ = initialLoc.getBlockZ()

    // remaining time counter
    private var time = timeWarp

    override fun run() {
        // check if player moved
        val location = player.location
        if (locX != location.getBlockX() || locY != location.getBlockY() || locZ != location.getBlockZ()) {
            Message.announcement(player, "${ChatColor.RED}Moved! Stopped warping...")
            this.cancel()

            // schedule main thread to remove cancelled task
            Bukkit.getScheduler().runTask(
                Nodes.plugin!!,
                object : Runnable {
                    public override fun run() {
                        Nodes.playerWarpTasks.remove(player.getUniqueId())
                    }
                },
            )

            return
        }

        time -= tick

        if (time <= 0.0) {
            this.cancel()

            // schedule main thread to finish warp
            Bukkit.getScheduler().runTask(
                Nodes.plugin!!,
                object : Runnable {
                    public override fun run() {
                        Nodes.playerWarpTasks.remove(player.getUniqueId())

                        // do warp
                        warpToPort(
                            destination,
                            playersToWarp,
                            entitiesToWarp,
                        )

                        Message.announcement(player, "${ChatColor.GREEN}Warped to ${destination.name}")
                    }
                },
            )
        } else {
            val progress = 1.0 - (time / timeWarp)
            Message.announcement(player, "Warping ${ChatColor.GREEN}${progressBar(progress)}")
        }
    }
}
