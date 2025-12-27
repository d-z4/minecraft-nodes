package phonon.nodes.commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.objects.TerritoryId
import phonon.nodes.utils.string.filterByStart

private val SUBCOMMANDS: List<String> = listOf(
    "tier",
    "add",
    "remove",
    "swap",
)

public class NodesSweepCommand :
    CommandExecutor,
    TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        if (args.size == 0) {
            printHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "tier" -> setTier(sender, args)
            "add" -> addResource(sender, args)
            "remove" -> removeResource(sender, args)
            "swap" -> swapResources(sender, args)
            else -> printHelp(sender)
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            return filterByStart(SUBCOMMANDS, args[0])
        } else if (args.size > 1) {
            when (args[0].lowercase()) {
                "add", "remove" -> {
                    if (args.size == 2) {
                        return Nodes.resourceNodes.keys.filter { it.startsWith(args[1], ignoreCase = true) }
                    }
                }
            }
        }

        return listOf()
    }

    private fun printHelp(sender: CommandSender) {
        Message.print(sender, "${ChatColor.BOLD}[Nodes] Territory Resource Editor:")
        Message.print(sender, "/nodesweep tier [level] [id]${ChatColor.WHITE}: Set territory tier")
        Message.print(sender, "/nodesweep add [resource] [id]${ChatColor.WHITE}: Add resource to territory")
        Message.print(sender, "/nodesweep remove [resource] [id]${ChatColor.WHITE}: Remove resource from territory")
        Message.print(sender, "/nodesweep swap [id1] [id2]${ChatColor.WHITE}: Swap resources between two territories")
    }

    /**
     * @command /nodesweep tier [level] [id]
     * Set territory tier (affects resource quality)
     */
    private fun setTier(sender: CommandSender, args: Array<String>) {
        val player: Player? = if (sender is Player) sender else null

        // /nodesweep tier [level]
        // Uses player's current location
        if (args.size == 2) {
            if (player == null) {
                Message.error(sender, "Must specify territory ID when run from console")
                return
            }

            val tier = args[1].toIntOrNull()
            if (tier == null) {
                Message.error(sender, "Invalid tier level")
                return
            }

            val territory = Nodes.getTerritoryFromPlayer(player)
            if (territory == null) {
                Message.error(sender, "No territory at your current location")
                return
            }

            Message.print(sender, "Setting territory (id=${territory.id}) to tier $tier")
            Message.error(sender, "This feature requires saving tier data - not yet implemented")
            // TODO: Need to add tier field to Territory class
        }
        // /nodesweep tier [level] [id]
        else if (args.size >= 3) {
            val tier = args[1].toIntOrNull()
            if (tier == null) {
                Message.error(sender, "Invalid tier level")
                return
            }

            val territoryId = try {
                TerritoryId(args[2].toInt())
            } catch (e: NumberFormatException) {
                Message.error(sender, "Invalid territory ID")
                return
            }

            val territory = Nodes.getTerritoryFromId(territoryId)
            if (territory == null) {
                Message.error(sender, "Territory with ID ${args[2]} does not exist")
                return
            }

            Message.print(sender, "Setting territory (id=${territory.id}) to tier $tier")
            Message.error(sender, "This feature requires saving tier data - not yet implemented")
            // TODO: Need to add tier field to Territory class
        } else {
            Message.error(sender, "Usage: /nodesweep tier [level] or /nodesweep tier [level] [id]")
        }
    }

    /**
     * @command /nodesweep add [resource] [id]
     * Add a resource node to a territory
     */
    private fun addResource(sender: CommandSender, args: Array<String>) {
        val player: Player? = if (sender is Player) sender else null

        // /nodesweep add [resource]
        if (args.size == 2) {
            if (player == null) {
                Message.error(sender, "Must specify territory ID when run from console")
                return
            }

            val resourceName = args[1]
            if (!Nodes.resourceNodes.containsKey(resourceName)) {
                Message.error(sender, "Resource \"$resourceName\" does not exist")
                return
            }

            val territory = Nodes.getTerritoryFromPlayer(player)
            if (territory == null) {
                Message.error(sender, "No territory at your current location")
                return
            }

            Message.print(sender, "Adding resource \"$resourceName\" to territory (id=${territory.id})")
            Message.error(sender, "This feature requires modifying world.json - use /nda reload territory after editing")
        }
        // /nodesweep add [resource] [id]
        else if (args.size >= 3) {
            val resourceName = args[1]
            if (!Nodes.resourceNodes.containsKey(resourceName)) {
                Message.error(sender, "Resource \"$resourceName\" does not exist")
                return
            }

            val territoryId = try {
                TerritoryId(args[2].toInt())
            } catch (e: NumberFormatException) {
                Message.error(sender, "Invalid territory ID")
                return
            }

            val territory = Nodes.getTerritoryFromId(territoryId)
            if (territory == null) {
                Message.error(sender, "Territory with ID ${args[2]} does not exist")
                return
            }

            Message.print(sender, "Adding resource \"$resourceName\" to territory (id=${territory.id})")
            Message.error(sender, "This feature requires modifying world.json - use /nda reload territory after editing")
        } else {
            Message.error(sender, "Usage: /nodesweep add [resource] or /nodesweep add [resource] [id]")
        }
    }

    /**
     * @command /nodesweep remove [resource] [id]
     * Remove a resource node from a territory
     */
    private fun removeResource(sender: CommandSender, args: Array<String>) {
        val player: Player? = if (sender is Player) sender else null

        // Similar implementation to addResource but for removal
        if (args.size == 2) {
            if (player == null) {
                Message.error(sender, "Must specify territory ID when run from console")
                return
            }

            val resourceName = args[1]
            val territory = Nodes.getTerritoryFromPlayer(player)
            if (territory == null) {
                Message.error(sender, "No territory at your current location")
                return
            }

            if (!territory.resourceNodes.contains(resourceName)) {
                Message.error(sender, "Territory does not have resource \"$resourceName\"")
                return
            }

            Message.print(sender, "Removing resource \"$resourceName\" from territory (id=${territory.id})")
            Message.error(sender, "This feature requires modifying world.json - use /nda reload territory after editing")
        } else if (args.size >= 3) {
            val resourceName = args[1]
            val territoryId = try {
                TerritoryId(args[2].toInt())
            } catch (e: NumberFormatException) {
                Message.error(sender, "Invalid territory ID")
                return
            }

            val territory = Nodes.getTerritoryFromId(territoryId)
            if (territory == null) {
                Message.error(sender, "Territory with ID ${args[2]} does not exist")
                return
            }

            if (!territory.resourceNodes.contains(resourceName)) {
                Message.error(sender, "Territory does not have resource \"$resourceName\"")
                return
            }

            Message.print(sender, "Removing resource \"$resourceName\" from territory (id=${territory.id})")
            Message.error(sender, "This feature requires modifying world.json - use /nda reload territory after editing")
        } else {
            Message.error(sender, "Usage: /nodesweep remove [resource] or /nodesweep remove [resource] [id]")
        }
    }

    /**
     * @command /nodesweep swap [id1] [id2]
     * Swap all resources between two territories
     */
    private fun swapResources(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            Message.error(sender, "Usage: /nodesweep swap [id1] [id2]")
            return
        }

        val id1 = try {
            TerritoryId(args[1].toInt())
        } catch (e: NumberFormatException) {
            Message.error(sender, "Invalid territory ID: ${args[1]}")
            return
        }

        val id2 = try {
            TerritoryId(args[2].toInt())
        } catch (e: NumberFormatException) {
            Message.error(sender, "Invalid territory ID: ${args[2]}")
            return
        }

        val terr1 = Nodes.getTerritoryFromId(id1)
        val terr2 = Nodes.getTerritoryFromId(id2)

        if (terr1 == null) {
            Message.error(sender, "Territory with ID ${args[1]} does not exist")
            return
        }
        if (terr2 == null) {
            Message.error(sender, "Territory with ID ${args[2]} does not exist")
            return
        }

        Message.print(sender, "Swapping resources between territory ${id1} and ${id2}")
        Message.error(sender, "This feature requires modifying world.json - use /nda reload territory after editing")
    }
}