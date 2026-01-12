package phonon.nodes.nodesaddons

import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import phonon.nodes.Nodes

class NodesMineBuff :
    CommandExecutor,
    TabCompleter {

    var mineBuff: Int = 1
    var time: Long = 0L
    var buffOn: Boolean = false
    var buffTask: BukkitRunnable? = null
    var buffEnd: Long = 0

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (label.equals("minebuff", ignoreCase = true)) {
            if (args.isNotEmpty() && args[0].equals("time", ignoreCase = true)) {
                if (!buffOn) {
                    sender.sendMessage("${ChatColor.RED}No buff is active.")
                    return true
                }
                val left = ((buffEnd - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                sender.sendMessage("${ChatColor.GOLD}Buff ends in $left min.")
                return true
            }
            if (sender !is Player && sender != getConsoleSender() || (sender is Player && sender.isOp)) {
                if (args.size < 2) {
                    sender.sendMessage("${ChatColor.RED}Usage: /minebuff <multiplier> <time-min>")
                    return true
                }
                val buff = args[0].toIntOrNull()
                val min = args[1].toIntOrNull()
                if (buff == null || min == null || buff < 1 || min < 1) {
                    sender.sendMessage("${ChatColor.RED}Invalid args.")
                    return true
                }
                if (buffOn) {
                    sender.sendMessage("${ChatColor.RED}Buff already active.")
                    return true
                }
                setBuffs(buff, min)
                sender.sendMessage("${ChatColor.GOLD}Buff x$buff for $min min.")
                buffTask = object : BukkitRunnable() {
                    override fun run() {
                        resetBuff()
                        Bukkit.broadcast("${ChatColor.RED}Mine buff ended.", "nodes.minebuff")
                    }
                }
                buffTask?.runTaskLater(Nodes.plugin!!, min * 60 * 20L)
                return true
            } else {
                sender.sendMessage("${ChatColor.RED}No permission.")
                return true
            }
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String> = when {
        args.size == 1 -> listOf("2", "3", "5", "10", "time").filter { it.startsWith(args[0]) }
        args.size == 2 && args[0] != "time" -> listOf("5", "15", "60", "120").filter { it.startsWith(args[1]) }
        else -> emptyList()
    }

    fun setBuffs(buff: Int, min: Int) {
        buffOn = true
        mineBuff = buff
        time = min * 60L * 1000L
        buffEnd = System.currentTimeMillis() + time
        for ((_, territory) in Nodes.iterTerritories()) {
            val ores = territory.ores.ores
            for (i in ores.indices) {
                val ore = ores[i]
                ores[i] = ore.copy(dropChance = ore.dropChance * mineBuff)
            }
        }
        NodesMineBuffBar.start(buff, buffEnd)
    }

    fun resetBuff() {
        for ((_, territory) in Nodes.iterTerritories()) {
            val ores = territory.ores.ores
            for (i in ores.indices) {
                val ore = ores[i]
                ores[i] = ore.copy(dropChance = ore.dropChance / mineBuff)
            }
        }
        NodesMineBuffBar.stop()
        mineBuff = 1
        time = 0
        buffOn = false
        buffTask = null
        buffEnd = 0
    }
}
