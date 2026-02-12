/**
 * Centralized handler for long periodic tasks:
 * - Income, backup, cooldowns
 */

package phonon.nodes

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import phonon.nodes.utils.FileWriteTask

public object PeriodicTickManager {

    private var task: ScheduledTask? = null

    // previous tick time
    private var previousTime: Long = 0L

    // run scheduler for saving backups
    public fun start(plugin: Plugin, period: Long) {
        if (this.task !== null) {
            return
        }

        // initialize previous time
        previousTime = System.currentTimeMillis()

        // scheduler for writing backups
        val task = object : Runnable {
            public override fun run() {
                // update time tick
                val currTime = System.currentTimeMillis()
                val capturedPreviousTime = previousTime
                previousTime = currTime

                // =================================
                // income cycle
                // =================================
                if (currTime > Nodes.lastIncomeTime + Config.incomePeriod) {
                    Nodes.lastIncomeTime = currTime

                    if (Config.incomeEnabled) {
                        Nodes.runIncome()
                    }

                    // save current time
                    Bukkit.getAsyncScheduler().runNow(Nodes.plugin!!, { _ -> FileWriteTask(currTime.toString(), Config.pathLastIncomeTime, null).run() })
                }

                // =================================
                // town, resident, truce cooldowns
                // =================================
                val currTime2 = System.currentTimeMillis()
                val dt = currTime2 - capturedPreviousTime
                Nodes.townMoveHomeCooldownTick(dt)
                Nodes.claimsPowerRamp(dt)
                Nodes.claimsPenaltyDecay(dt)
                Nodes.residentTownCreateCooldownTick(dt)
                Nodes.truceTick()
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
