/**
 * Port commands
 * /port [command]
 */

package phonon.nodes.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import phonon.nodes.Config
import phonon.nodes.Message
import phonon.nodes.Nodes
import phonon.nodes.constants.DiplomaticRelationship
import phonon.nodes.objects.Port
import phonon.nodes.tasks.PortWarpTask
import phonon.nodes.utils.string.filterByStart
import phonon.nodes.utils.string.filterPort

// list of all subcommands
private val PORT_COMMANDS: List<String> = listOf(
    "help",
    "list",
    "info",
    "warp",
)

/**
 * Port command executor
 */
public class PortCommand :
    CommandExecutor,
    TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, commandLabel: String, args: Array<String>): Boolean {
        // no args, print help
        if (args.size == 0) {
            printHelp(sender)
            return true
        }

        // parse subcommand
        val arg = args[0].lowercase()
        when (arg) {
            "help" -> printHelp(sender)
            "list" -> list(sender)
            "info" -> info(sender, args)
            "warp" -> doWarp(sender, args)
            else -> {
                Message.error(sender, "Invalid port command")
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size == 1) {
            return filterByStart(PORT_COMMANDS, args[0])
        }
        // match each subcommand format
        else if (args.size > 1) {
            // handle specific subcommands
            when (args[0].lowercase()) {
                "info" -> {
                    if (args.size == 2) {
                        return filterPort(args[1])
                    }
                }

                "warp" -> {
                    if (args.size == 2) {
                        return filterPort(args[1])
                    }
                }
            }
        }

        return listOf()
    }

    private fun printHelp(sender: CommandSender) {
        Message.print(sender, "${ChatColor.AQUA}${ChatColor.BOLD}[Nodes] Port Commands:")
        Message.print(sender, "${ChatColor.AQUA}/port help${ChatColor.WHITE}: help")
        Message.print(sender, "${ChatColor.AQUA}/port list${ChatColor.WHITE}: list all ports")
        Message.print(sender, "${ChatColor.AQUA}/port info${ChatColor.WHITE}: print info about a port")
        Message.print(sender, "${ChatColor.AQUA}/port warp${ChatColor.WHITE}: warp to a port")
        return
    }

    /**
     * Print list of all ports
     */
    private fun list(sender: CommandSender) {
        Message.print(sender, "${ChatColor.AQUA}${ChatColor.BOLD}List of ports:")
        val ports = Nodes.ports
        for (group in Nodes.portGroups.values) {
            Message.print(sender, "${ChatColor.AQUA}${group.name}:")
            for (port in ports.values) {
                if (port.groups.contains(group)) {
                    // comma-separated group names for this port
                    val groupNames = port.groups.joinToString(", ") { it.name }

                    val status = if (port.isPublic) {
                        "(public)"
                    } else {
                        "(owned)"
                    }

                    Message.print(
                        sender,
                        "- ${ChatColor.AQUA}${port.name} ${ChatColor.GRAY}- $groupNames $status",
                    )
                }
            }
        }

        Message.print(sender, "${ChatColor.DARK_AQUA}Use \"/port info [name]\" for details")
    }

    /**
     * Print port info
     */
    private fun info(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            Message.error(sender, "Usage: /port info [name]")
            return
        }

        val portName = args[1].lowercase()
        val port = Nodes.ports.get(portName)
        if (port !== null) {
            Message.print(sender, "${ChatColor.AQUA}${ChatColor.BOLD}Port ${port.name}:")
            Message.print(sender, "${ChatColor.AQUA}- (x,z): (${port.locX}, ${port.locZ})")
            Message.print(sender, "${ChatColor.AQUA}- Groups:")
            for (group in port.groups) {
                Message.print(sender, "${ChatColor.AQUA}  - ${group.name}")
            }

            if (port.isPublic) {
                Message.print(sender, "${ChatColor.AQUA}- Public")
            } else {
                // get owner
                val owner = Nodes.getPortOwner(port)
                val ownerName = if (owner !== null) {
                    owner.name
                } else {
                    "${ChatColor.GRAY}None"
                }
                Message.print(sender, "${ChatColor.AQUA}- Owner: $ownerName")
                Message.print(sender, "${ChatColor.AQUA}- Access: Allies only")
            }
        }
    }

    /**
     * Initiate port warp
     */
    private fun doWarp(sender: CommandSender, args: Array<String>) {
        val player = if (sender is Player) sender else null
        if (player === null) {
            Message.error(sender, "Only players in game can use")
            return
        }

        if (args.size < 2) {
            Message.error(sender, "Usage: /port warp [destination]")
            return
        }

        val portName = args[1].lowercase()

        doPortWarp(player, portName)
    }
}

/**
 * Player try to warp to a port
 */
internal fun doPortWarp(
    player: Player,
    destinationName: String,
) {
    val playerUuid = player.getUniqueId()

    // check if player is already warping
    if (Nodes.playerWarpTasks.contains(playerUuid)) {
        Message.print(player, "${ChatColor.RED}You are already warping somewhere")
        return
    }

    // get port player is at
    val locPlayer = player.location
    val source = Nodes.chunkToPort.get(listOf(Math.floorDiv(locPlayer.x.toInt(), 16), Math.floorDiv(locPlayer.z.toInt(), 16)))
    if (source === null) {
        Message.print(player, "${ChatColor.RED}You must be in the same chunk as a port to warp")
        return
    }

    // check if port exists
    val destination = Nodes.ports.get(destinationName.lowercase())
    if (destination === null) {
        Message.error(player, "Port does not exist...")
        return
    }

    // check if port is same
    if (source === destination) {
        Message.error(player, "You are already at this port...")
        return
    }

    // verify ports share groups
    if (!Nodes.sharePortGroups(source, destination)) {
        Message.error(player, "These ports are not in the same region group...")
        return
    }

    // check port access
    if (!destination.isPublic) {
        val owner = Nodes.getPortOwner(destination)
        if (owner !== null) {
            val relation = Nodes.getRelationshipOfPlayerToTown(player, owner)

            // Only allow allies (town members, nation members, and allies) to use the port
            val canAccess: Boolean = when (relation) {
                DiplomaticRelationship.TOWN,
                DiplomaticRelationship.NATION,
                DiplomaticRelationship.ALLY,
                -> true
                else -> false
            }

            if (!canAccess) {
                Message.error(player, "Port $destinationName's owner ${owner.name} only allows allies to warp (you are $relation)")
                return
            }
        }
    }

    // determine who is warping and time:
    val playersToWarp: ArrayList<Player> = arrayListOf()
    val entitiesToWarp: ArrayList<Entity> = arrayListOf()

    // 1. player by itself, no vehicle
    val entityVehicle = player.getVehicle()
    if (entityVehicle === null) {
        if (!Config.allowPortWarpWithoutBoat) {
            Message.error(player, "You must be in a boat or a ship vehicle to warp to ports")
            return
        }

        playersToWarp.add(player)
    } else {
        // 2. player in a boat
        if (entityVehicle.type.toString().contains("_BOAT")) {
            entitiesToWarp.add(entityVehicle)
        }
    }

    // do warp
    val task = PortWarpTask(
        player,
        destination,
        playersToWarp.toList(),
        entitiesToWarp.toList(),
        player.location.clone(),
        Config.portWarpTime,
        2.0,
    )

    // run asynchronous warp timer
    Nodes.playerWarpTasks.put(
        player.getUniqueId(),
        task.runTaskTimerAsynchronously(Nodes.plugin!!, 2, 2),
    )
}

/**
 * Actually runs warp on player/vehicle
 */
internal fun warpToPort(
    destination: Port,
    playersToWarp: List<Player>,
    entitiesToWarp: List<Entity>,
) {
    val defaultWorld = Bukkit.getWorlds().get(0)
    val x = destination.locX.toDouble()
    val y = Config.seaLevel
    val z = destination.locZ.toDouble()

    for (player in playersToWarp) {
        player.teleport(Location(defaultWorld, x, y, z))
    }

    for (entity in entitiesToWarp) {
        teleportEntity(entity, Location(defaultWorld, x, y, z))
    }
}

/**
 * Handle warping entity which may have passenger
 */
internal fun teleportEntity(entity: Entity, destination: Location) {
    val passengers = entity.getPassengers()

    // remove players from boats and teleport to destination
    for (p in passengers) {
        p.eject()
        entity.removePassenger(p)
        p.teleport(destination)
    }

    // schedule entity teleport (after players already teleported)
    Bukkit.getScheduler().runTaskLater(
        Nodes.plugin!!,
        object : Runnable {
            override fun run() {
                entity.teleport(destination)

                // force the chunk to load at destination to makes sure the entity syncs to the client
                val chunk = entity.location.chunk
                if (!chunk.isLoaded) {
                    chunk.load()
                }
            }
        },
        1L,
    )

    // schedule re-attaching player to boat
    Bukkit.getScheduler().runTaskLater(
        Nodes.plugin!!,
        object : Runnable {
            override fun run() {
                for (p in passengers) {
                    entity.addPassenger(p)
                }
            }
        },
        2L,
    )
}

/**
 * Create progress bar string. Input should be double
 * in range [0.0, 1.0] marking progress.
 */
internal fun progressBar(progress: Double): String = when (Math.round(progress * 10.0).toInt()) {
    0 -> "\u2503\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2503"
    1 -> "\u2503\u2588\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2503"
    2 -> "\u2503\u2588\u2588\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2503"
    3 -> "\u2503\u2588\u2588\u2588\u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2503"
    4 -> "\u2503\u2588\u2588\u2588\u2588\u2592\u2592\u2592\u2592\u2592\u2592\u2503"
    5 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2592\u2592\u2592\u2592\u2592\u2503"
    6 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2588\u2592\u2592\u2592\u2592\u2503"
    7 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2592\u2592\u2592\u2503"
    8 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2592\u2592\u2503"
    9 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2592\u2503"
    10 -> "\u2503\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2503"
    else -> ""
}
