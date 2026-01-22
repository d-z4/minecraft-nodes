/**
 * Scheduler for saving Nodes world state to towns.json
 *
 * Runs world save to towns.json on a fixed tick schedule.
 * If we save everytime world state updates, players can lag servers
 * by spamming commands. Running on fixed schedules avoids
 * this exploit.
 *
 */

package phonon.nodes.tasks

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import phonon.nodes.Config
import phonon.nodes.Nodes
import phonon.nodes.objects.Nation.NationSaveState
import phonon.nodes.objects.Port.PortSaveState
import phonon.nodes.objects.PortGroup.PortGroupSaveState
import phonon.nodes.objects.Resident.ResidentSaveState
import phonon.nodes.objects.Town.TownSaveState
import phonon.nodes.serdes.Serializer
import phonon.nodes.utils.saveStringToFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Runnable task to save world. This can be run either synchronously or
 * asynchronously by the caller.
 *
 */
public class TaskSaveWorld(
    val residentsSnapshot: List<ResidentSaveState>,
    val townsSnapshot: List<TownSaveState>,
    val nationsSnapshot: List<NationSaveState>,
    val pathTownSave: Path,
    val pathDynmapDir: Path,
    val pathDynmapTowns: Path,
    val pathBackupDir: Path,
    val pathLastBackupTime: Path,
    val copyToDynmap: Boolean,
    val backupTimestamp: Long,
) : Runnable {
    override fun run() {
        // serialize world state
        val jsonStr = Serializer.worldToJson(
            residentsSnapshot,
            townsSnapshot,
            nationsSnapshot,
        )

        saveStringToFile(jsonStr, pathTownSave)

        if (copyToDynmap) {
            Files.createDirectories(pathDynmapDir) // create dynmap folder if it does not exist
            Files.copy(pathTownSave, pathDynmapTowns, StandardCopyOption.REPLACE_EXISTING)
        }

        // if backup timestamp millis timestamp (using System.currentTimeMillis())
        // was provided, copy this saved world state to backup folder
        if (backupTimestamp > 0) {
            TaskSaveBackup(backupTimestamp, pathTownSave, pathBackupDir, pathLastBackupTime).run()
        }
    }
}

// backup format
private val BACKUP_DATE_FORMATTER = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")

/**
 * Save timestamped backup file of towns.json into backup folder.
 */
internal class TaskSaveBackup(
    val timestamp: Long, // millis timestamp from System.currentTimeMillis()
    val pathTowns: Path,
    val pathBackupDir: Path,
    val pathLastBackupTime: Path,
) : Runnable {
    override fun run() {
        // val pathTowns = Config.pathTowns
        // val pathBackupDir = Config.pathBackup
        if (Files.exists(pathTowns)) {
            Files.createDirectories(pathBackupDir) // create backup folder if it does not exist

            // save towns file backup
            val date = Date(timestamp)
            val backupName = "towns.${BACKUP_DATE_FORMATTER.format(date)}.json"
            val pathBackup = pathBackupDir.resolve(backupName)
            Files.copy(pathTowns, pathBackup)
        }

        // save last backup timestamp to file
        saveStringToFile(timestamp.toString(), Config.pathLastBackupTime)
    }
}

public class TaskSavePorts(
    val portsSnapshot: List<PortSaveState>,
    val portGroupsSnapshot: List<PortGroupSaveState>,
    val pathPortSave: Path,
    val pathDynmapDir: Path,
    val pathDynmapPorts: Path,
    val copyToDynmap: Boolean,
) : Runnable {
    override fun run() {
        // serialize port state
        val jsonStr = Serializer.portsToJson(
            portGroupsSnapshot,
            portsSnapshot,
        )

        saveStringToFile(jsonStr, pathPortSave)

        if (copyToDynmap) {
            Files.createDirectories(pathDynmapDir) // create dynmap folder if it does not exist
            Files.copy(pathPortSave, pathDynmapPorts, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/**
 * Async periodic tick scheduler to signal main thread
 * to save world state.
 */
public object SaveManager {

    private var task: BukkitTask? = null

    public fun start(plugin: Plugin, period: Long) {
        if (this.task !== null) {
            return
        }

        // scheduler for saving world
        val task = object : Runnable {

            init {
                // create save folder if it does not exist
                Files.createDirectories(Paths.get(Config.pathPlugin).normalize())
            }

            public override fun run() {
                // schedule main thread to run save
                Bukkit.getScheduler().runTask(
                    plugin,
                    object : Runnable {
                        override fun run() {
                            Nodes.saveWorld(
                                checkIfNeedsSave = true,
                                async = true,
                            )
                        }
                    },
                )
            }
        }

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, period, period)
    }

    public fun stop() {
        val task = this.task
        if (task === null) {
            return
        }

        task.cancel()
        this.task = null
    }
}

/**
 * Async periodic tick scheduler to save boost state
 */
public object BoostSaveManager {

    private var task: ScheduledTask? = null

    public fun start(plugin: Plugin, period: Long) {
        if (this.task !== null) {
            return
        }

        // scheduler for saving boosts
        val task = object : Runnable {
            public override fun run() {
                if (Config.boostEnabled) {
                    // Clean up expired boosts
                    phonon.nodes.BoostManager.cleanupExpiredBoosts()

                    // Save if dirty
                    if (phonon.nodes.BoostManager.dirty) {
                        Nodes.saveBoosts()
                    }
                }
            }
        }

        this.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> task.run() }, period, period)
    }

    public fun stop() {
        val task = this.task
        if (task === null) {
            return
        }

        task.cancel()
        this.task = null
    }
}

/**
 * Async periodic tick scheduler to update boss bars
 */
public object BoostBossBarUpdateManager {

    private var task: ScheduledTask? = null

    public fun start(plugin: Plugin, period: Long) {
        if (this.task !== null) {
            return
        }

        // scheduler for updating boss bars
        val task = object : Runnable {
            public override fun run() {
                if (Config.boostEnabled) {
                    phonon.nodes.BoostBossBarManager.updateAllPlayerBossBars()
                }
            }
        }

        this.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { _ -> task.run() }, period, period)
    }

    public fun stop() {
        val task = this.task
        if (task === null) {
            return
        }

        task.cancel()
        this.task = null
    }
}
