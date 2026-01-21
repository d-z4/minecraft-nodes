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
import phonon.nodes.objects.OreDeposit
import phonon.nodes.objects.OreSampler

class NodesMineBuff :
    CommandExecutor,
    TabCompleter {

    var mineBuff: Int = 1
    var time: Long = 0L
    var buffOn: Boolean = false
    var buffTask: BukkitRunnable? = null
    var buffEnd: Long = 0

    // Store original ore rates before applying buff
    private val originalOres: HashMap<Int, List<OreDeposit>> = hashMapOf()

    companion object {
        // Global multiplier accessible to ore drop events
        @JvmStatic
        var globalMultiplier: Int = 1
            private set
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (label.equals("minebuff", ignoreCase = true)) {
            // Check time remaining
            if (args.isNotEmpty() && args[0].equals("time", ignoreCase = true)) {
                if (!buffOn) {
                    sender.sendMessage("${ChatColor.RED}No buff is active.")
                    return true
                }
                val left = ((buffEnd - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                sender.sendMessage("${ChatColor.GOLD}Buff ends in $left min.")
                return true
            }

            // Remove/cancel active buff
            if (args.isNotEmpty() && args[0].equals("remove", ignoreCase = true)) {
                if (sender !is Player && sender != getConsoleSender() || (sender is Player && sender.isOp)) {
                    if (!buffOn) {
                        sender.sendMessage("${ChatColor.RED}No buff is active.")
                        return true
                    }
                    // Cancel the scheduled task
                    buffTask?.cancel()
                    // Reset the buff
                    resetBuff()
                    sender.sendMessage("${ChatColor.GOLD}Mine buff removed.")
                    Bukkit.broadcast("${ChatColor.RED}Mine buff was manually removed.", "nodes.minebuff")
                    return true
                } else {
                    sender.sendMessage("${ChatColor.RED}No permission.")
                    return true
                }
            }

            // Set new buff
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
                Bukkit.broadcast("${ChatColor.GOLD}Mine buff x$buff activated for $min minutes! (applies everywhere including wilderness)", "nodes.minebuff")
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
        args.size == 1 -> listOf("2", "3", "5", "10", "time", "remove").filter { it.startsWith(args[0]) }
        args.size == 2 && args[0] != "time" && args[0] != "remove" -> listOf("5", "15", "60", "120").filter { it.startsWith(args[1]) }
        else -> emptyList()
    }

    fun setBuffs(buff: Int, min: Int) {
        buffOn = true
        mineBuff = buff
        globalMultiplier = buff // Set global multiplier for wilderness
        time = min * 60L * 1000L
        buffEnd = System.currentTimeMillis() + time

        // Clear any previous backup
        originalOres.clear()

        // Iterate over ALL territories (including wilderness)
        val allTerritories = Nodes.territories
        if (allTerritories != null) {
            for ((territoryId, territory) in allTerritories) {
                val ores = territory.ores.ores

                // Store original ore values BEFORE modifying
                originalOres[territoryId.toInt()] = ores.map { it.copy() }

                // Apply buff multiplier
                for (i in ores.indices) {
                    val ore = ores[i]
                    ores[i] = ore.copy(dropChance = ore.dropChance * mineBuff)
                }

                // CRITICAL: Rebuild the OreSampler with modified ore values
                // The OreSampler caches ore probabilities internally, so we must recreate it
                val field = territory.javaClass.getDeclaredField("ores")
                field.isAccessible = true
                field.set(territory, OreSampler(ores))
            }
        }

        NodesMineBuffBar.start(buff, buffEnd)
    }

    fun resetBuff() {
        // Restore original ore values from backup for ALL territories
        val allTerritories = Nodes.territories
        if (allTerritories != null) {
            for ((territoryId, territory) in allTerritories) {
                val ores = territory.ores.ores
                val originalTerritoryOres = originalOres[territoryId.toInt()]

                if (originalTerritoryOres != null && originalTerritoryOres.size == ores.size) {
                    // Restore exact original values
                    for (i in ores.indices) {
                        ores[i] = originalTerritoryOres[i].copy()
                    }
                } else {
                    // Fallback: divide by multiplier (less accurate)
                    for (i in ores.indices) {
                        val ore = ores[i]
                        ores[i] = ore.copy(dropChance = ore.dropChance / mineBuff)
                    }
                }

                // CRITICAL: Rebuild the OreSampler with restored ore values
                // The OreSampler caches ore probabilities internally, so we must recreate it
                val field = territory.javaClass.getDeclaredField("ores")
                field.isAccessible = true
                field.set(territory, OreSampler(ores))
            }
        }

        // Clear backup
        originalOres.clear()

        // Reset global multiplier
        globalMultiplier = 1

        NodesMineBuffBar.stop()
        mineBuff = 1
        time = 0
        buffOn = false
        buffTask = null
        buffEnd = 0
    }
}
