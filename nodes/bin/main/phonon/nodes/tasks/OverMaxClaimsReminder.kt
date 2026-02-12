/**
 * Income background scheduler to periodically run
 * message that towns are over max claims
 */

package phonon.nodes.tasks

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import phonon.nodes.Nodes

public object OverMaxClaimsReminder {

    private var task: ScheduledTask? = null

    // run scheduler for reminders
    public fun start(plugin: Plugin, period: Long) {
        if (this.task !== null) {
            return
        }

        // scheduler for reminders
        val task = object : Runnable {
            public override fun run() {
                Nodes.overMaxClaimsReminder()
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
