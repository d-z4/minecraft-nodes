package phonon.nodes.tasks

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.commands.progressBar
import phonon.nodes.commands.warpToPort
import phonon.nodes.objects.Port
import java.util.concurrent.TimeUnit

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
) {
    private val locX = initialLoc.getBlockX()
    private val locY = initialLoc.getBlockY()
    private val locZ = initialLoc.getBlockZ()

    // remaining time counter
    private var time = timeWarp
    
    private var task: ScheduledTask? = null

    public fun start(): ScheduledTask {
        val runnable = object : Runnable {
            override fun run() {
                // check if player moved using player's scheduler
                player.scheduler.run(
                    Nodes.plugin!!,
                    { _ ->
                        val location = player.location
                        if (locX != location.getBlockX() || locY != location.getBlockY() || locZ != location.getBlockZ()) {
                            Message.announcement(player, "${ChatColor.RED}Moved! Stopped warping...")
                            task?.cancel()
                            Nodes.playerWarpTasks.remove(player.getUniqueId())
                            return@run
                        }

                        time -= tick

                        if (time <= 0.0) {
                            task?.cancel()
                            Nodes.playerWarpTasks.remove(player.getUniqueId())

                            // do warp
                            warpToPort(
                                destination,
                                playersToWarp,
                                entitiesToWarp,
                            )

                            Message.announcement(player, "${ChatColor.GREEN}Warped to ${destination.name}")
                        } else {
                            val progress = 1.0 - (time / timeWarp)
                            Message.announcement(player, "Warping ${ChatColor.GREEN}${progressBar(progress)}")
                        }
                    },
                    null,
                )
            }
        }

        this.task = Bukkit.getAsyncScheduler().runAtFixedRate(
            Nodes.plugin!!,
            { _ -> runnable.run() },
            (tick * 50).toLong(),
            (tick * 50).toLong(),
            TimeUnit.MILLISECONDS,
        )

        return this.task!!
    }
}
