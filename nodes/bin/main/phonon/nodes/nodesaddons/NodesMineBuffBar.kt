package phonon.nodes.nodesaddons

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable
import phonon.nodes.Nodes

object NodesMineBuffBar : Listener {
    private var bossBar: BossBar? = null
    private var updateTask: BukkitRunnable? = null
    private var totalDuration: Long = 0L
    private var endTime: Long = 0L
    private var multiplier: Int = 1
    private var registered = false

    fun start(buff: Int, endMillis: Long) {
        stop()

        multiplier = buff
        endTime = endMillis
        totalDuration = (endTime - System.currentTimeMillis()).coerceAtLeast(1L)

        bossBar = Bukkit.createBossBar(
            formatTitle(),
            BarColor.YELLOW,
            BarStyle.SOLID,
        )

        // add current online players
        Bukkit.getOnlinePlayers().forEach { bossBar?.addPlayer(it) }

        // register join listener once
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, Nodes.plugin!!)
            registered = true
        }

        // update every second
        updateTask = object : BukkitRunnable() {
            override fun run() {
                val now = System.currentTimeMillis()
                val remaining = (endTime - now).coerceAtLeast(0L)
                val progress = (remaining.toDouble() / totalDuration.toDouble()).coerceAtLeast(0.0).toFloat()
                bossBar?.progress = progress.toDouble()
                bossBar?.setTitle(formatTitle(remaining))

                if (remaining <= 0L) {
                    stop()
                }
            }
        }
        updateTask?.runTaskTimer(Nodes.plugin!!, 0L, 20L)
    }

    fun stop() {
        updateTask?.cancel()
        updateTask = null

        bossBar?.players?.toList()?.forEach { bossBar?.removePlayer(it) }
        bossBar?.removeAll()
        bossBar = null

        if (registered) {
            HandlerList.unregisterAll(this)
            registered = false
        }

        totalDuration = 0L
        endTime = 0L
        multiplier = 1
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        bossBar?.addPlayer(e.player)
    }

    private fun formatTitle(remainingMillis: Long = endTime - System.currentTimeMillis()): String {
        val rem = remainingMillis.coerceAtLeast(0L)
        val minutes = (rem / 60000L)
        val seconds = (rem / 1000L) % 60
        return "${ChatColor.GOLD}Mine Buff x$multiplier ${ChatColor.WHITE}- ${ChatColor.AQUA}${minutes}m ${seconds}s"
    }
}
