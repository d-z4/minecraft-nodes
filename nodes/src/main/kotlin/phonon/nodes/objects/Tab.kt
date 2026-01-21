/**
 * Advanced player tab list with sections and LuckPerms integration
 *
 * Tab is divided into sections:
 * - Staff Team (sorted by LuckPerms weight)
 * - Your Nation (X players)
 * - Your Town (X players)
 * - Rest of World (X players) - Allies/Enemies/Neutral
 */

package phonon.nodes.objects

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minecraft.ChatFormatting
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import phonon.nodes.Config
import phonon.nodes.Nodes
import phonon.nodes.nms.sendTeamCreate
import phonon.nodes.nms.sendTeamRemove

/**
 * Configuration for tab list appearance
 */
public object TabConfig {
    // Section headers
    public var staffHeader: String = "§6§l⚔ STAFF TEAM ⚔"
    public var nationHeader: String = "§a§l⚜ YOUR NATION ⚜"
    public var townHeader: String = "§b§l🏰 YOUR TOWN 🏰"
    public var worldHeader: String = "§7§l🌍 REST OF WORLD 🌍"

    // Section footers (optional separators)
    public var sectionSeparator: String = "§8§m                    "

    // Show player counts in headers
    public var showPlayerCounts: Boolean = true

    // Staff detection settings
    public var staffMinWeight: Int = 50 // Minimum LuckPerms weight to be considered staff
    public var staffPermissionGroups: List<String> = listOf("admin", "moderator", "helper") // Permission groups that count as staff
    public var useWeightForStaff: Boolean = true // Use weight instead of permission groups

    // Tab list update period
    public var tabUpdatePeriod: Long = 100L // Update every 5 seconds (100 ticks)

    // Color settings
    public var staffColor: ChatFormatting = ChatFormatting.GOLD
    public var nationColor: ChatFormatting = ChatFormatting.GREEN
    public var townColor: ChatFormatting = ChatFormatting.AQUA
    public var allyColor: ChatFormatting = ChatFormatting.BLUE
    public var enemyColor: ChatFormatting = ChatFormatting.RED
    public var neutralColor: ChatFormatting = ChatFormatting.WHITE
}

/**
 * Player data holder for tab list sorting
 */
private data class TabPlayer(
    val player: Player,
    val town: Town?,
    val luckPermsWeight: Int,
    val luckPermsPrefix: String,
    val isStaff: Boolean,
)

/**
 * Get LuckPerms API instance
 */
private fun getLuckPerms(): LuckPerms? = try {
    LuckPermsProvider.get()
} catch (e: Exception) {
    null
}

/**
 * Get player's LuckPerms weight
 */
private fun getPlayerWeight(player: Player): Int {
    val lp = getLuckPerms() ?: return 0
    val user = lp.userManager.getUser(player.uniqueId) ?: return 0

    // Get primary group name
    val primaryGroupName = user.cachedData.metaData.primaryGroup ?: return 0

    // Get the group and its weight
    val group = lp.groupManager.getGroup(primaryGroupName) ?: return 0

    // Try to get weight from group's data
    return try {
        group.weight.orElse(0)
    } catch (e: Exception) {
        // Fallback: try to get from cached data
        try {
            val weightNode = group.cachedData.metaData.getMetaValue("weight")
            weightNode?.toIntOrNull() ?: 0
        } catch (e2: Exception) {
            0
        }
    }
}

/**
 * Check if player is staff based on configuration
 */
private fun isPlayerStaff(player: Player): Boolean {
    val lp = getLuckPerms() ?: return false
    val user = lp.userManager.getUser(player.uniqueId) ?: return false

    // Method 1: Check by weight
    if (TabConfig.useWeightForStaff) {
        val weight = getPlayerWeight(player)
        return weight >= TabConfig.staffMinWeight
    }

    // Method 2: Check by permission group
    val primaryGroup = user.cachedData.metaData.primaryGroup ?: return false
    return TabConfig.staffPermissionGroups.any { groupName ->
        primaryGroup.equals(groupName, ignoreCase = true)
    }
}

/**
 * Get player's LuckPerms prefix
 */
private fun getPlayerPrefix(player: Player): String {
    val lp = getLuckPerms() ?: return ""
    val user = lp.userManager.getUser(player.uniqueId) ?: return ""

    return user.cachedData.metaData.prefix ?: ""
}

/**
 * Get tab list color for a town as viewed by a specific player
 */
private fun getTabColorForTown(town: Town, viewer: Player, isStaff: Boolean): ChatFormatting {
    if (isStaff) {
        return TabConfig.staffColor
    }

    val viewerTown = Nodes.getResident(viewer)?.town

    if (viewerTown !== null) {
        if (town === viewerTown) {
            return TabConfig.townColor
        } else if (town.nation !== null && town.nation === viewerTown.nation) {
            return TabConfig.nationColor
        } else if (town.allies.contains(viewerTown)) {
            return TabConfig.allyColor
        } else if (town.enemies.contains(viewerTown)) {
            return TabConfig.enemyColor
        }
    }

    return TabConfig.neutralColor
}

/**
 * Get sorting prefix for tab list ordering
 * Lower = appears higher in tab list
 */
private fun getSortingPrefix(section: String, index: Int): String = when (section) {
    "staff" -> String.format("000_%03d", index)
    "nation" -> String.format("001_%03d", index)
    "town" -> String.format("002_%03d", index)
    "world" -> String.format("003_%03d", index)
    else -> String.format("999_%03d", index)
}

public object Tab {

    private var task: BukkitTask? = null
    private var updateLock = false

    private val sectionHeaders = mutableMapOf<java.util.UUID, MutableList<String>>()

    private var fakeIndex = 0

    private fun nextFakeEntry(): String {
        fakeIndex++
        return "§0§0§${fakeIndex % 10}§r"
    }

    public fun start(plugin: Plugin, period: Long = Config.tabUpdatePeriod) {
        if (task != null) return
        if (!Config.tabEnabled) return

        task = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                pipelinedUpdateAll()
            },
            period,
            period,
        )
    }

    public fun stop() {
        task?.cancel()
        task = null
    }

    public fun updateTabForPlayer(player: Player) {
        fakeIndex = 0

        val viewerTown = Nodes.getTownFromPlayer(player)
        val viewerNation = viewerTown?.nation

        val allPlayers = Bukkit.getOnlinePlayers().mapNotNull { p ->
            val town = Nodes.getTownFromPlayer(p)
            val weight = getPlayerWeight(p)
            val prefix = getPlayerPrefix(p)
            val staff = isPlayerStaff(p)

            TabPlayer(p, town, weight, prefix, staff)
        }

        val staffPlayers = allPlayers.filter { it.isStaff }.sortedByDescending { it.luckPermsWeight }

        val nationPlayers = if (viewerNation != null) {
            allPlayers.filter { !it.isStaff && it.town?.nation === viewerNation && it.town !== viewerTown }
        } else {
            emptyList()
        }

        val townPlayers = if (viewerTown != null) {
            allPlayers.filter { !it.isStaff && it.town === viewerTown }
        } else {
            emptyList()
        }

        val worldPlayers = allPlayers.filter {
            !it.isStaff &&
                (viewerNation == null || it.town?.nation !== viewerNation) &&
                (viewerTown == null || it.town !== viewerTown)
        }.sortedBy { it.player.name }

        sectionHeaders[player.uniqueId]?.forEach {
            player.sendTeamRemove(it)
        }
        sectionHeaders[player.uniqueId]?.clear()

        fun addHeader(team: String, text: String) {
            val fake = nextFakeEntry()

            player.sendTeamCreate(
                teamName = team,
                prefix = text,
                suffix = "",
                color = ChatFormatting.RESET,
                members = listOf(fake),
            )

            sectionHeaders.getOrPut(player.uniqueId) { mutableListOf() }.add(team)
        }

        fun addSeparator(team: String) {
            val fake = nextFakeEntry()

            player.sendTeamCreate(
                teamName = team,
                prefix = TabConfig.sectionSeparator,
                suffix = "",
                color = ChatFormatting.RESET,
                members = listOf(fake),
            )

            sectionHeaders[player.uniqueId]!!.add(team)
        }

        if (staffPlayers.isNotEmpty()) {
            val count = if (TabConfig.showPlayerCounts) " §7(${staffPlayers.size})" else ""
            addHeader("tab_header_staff", TabConfig.staffHeader + count)

            staffPlayers.forEach {
                player.sendTeamCreate(
                    teamName = "tab_staff_${it.player.name}",
                    prefix = "${it.luckPermsPrefix} §r",
                    suffix = "",
                    color = TabConfig.staffColor,
                    members = listOf(it.player.name),
                )
            }

            addSeparator("tab_sep_staff")
        }

        if (nationPlayers.isNotEmpty() && viewerNation != null) {
            val count = if (TabConfig.showPlayerCounts) " §7(${nationPlayers.size})" else ""
            addHeader("tab_header_nation", TabConfig.nationHeader + count)

            nationPlayers.forEach {
                val tag = it.town?.let { t -> "[${t.name}] " } ?: ""

                player.sendTeamCreate(
                    teamName = "tab_nation_${it.player.name}",
                    prefix = tag,
                    suffix = "",
                    color = TabConfig.nationColor,
                    members = listOf(it.player.name),
                )
            }

            addSeparator("tab_sep_nation")
        }

        if (townPlayers.isNotEmpty() && viewerTown != null) {
            val count = if (TabConfig.showPlayerCounts) " §7(${townPlayers.size})" else ""
            addHeader("tab_header_town", TabConfig.townHeader + count)

            townPlayers.forEach {
                player.sendTeamCreate(
                    teamName = "tab_town_${it.player.name}",
                    prefix = "",
                    suffix = "",
                    color = TabConfig.townColor,
                    members = listOf(it.player.name),
                )
            }

            addSeparator("tab_sep_town")
        }

        if (worldPlayers.isNotEmpty()) {
            val count = if (TabConfig.showPlayerCounts) " §7(${worldPlayers.size})" else ""
            addHeader("tab_header_world", TabConfig.worldHeader + count)

            worldPlayers.forEach {
                val color = if (it.town != null) {
                    getTabColorForTown(it.town, player, false)
                } else {
                    TabConfig.neutralColor
                }

                val prefix = if (it.town != null) {
                    when {
                        viewerTown?.allies?.contains(it.town) == true -> "§9[A] "
                        viewerTown?.enemies?.contains(it.town) == true -> "§c[E] "
                        else -> ""
                    }
                } else {
                    ""
                }

                player.sendTeamCreate(
                    teamName = "tab_world_${it.player.name}",
                    prefix = prefix,
                    suffix = "",
                    color = color,
                    members = listOf(it.player.name),
                )
            }
        }
    }

    public fun updateTabHeaderFooter(player: Player) {
        val header =
            "§6§lRNC\n" +
                "§7play.riseandconquer.online\n\n" +
                "§fOnline: §a${Bukkit.getOnlinePlayers().size}"

        val footer =
            "\n§7Patreon: §b https://www.patreon.com/cw/RiseandConquerAges\n" +
                "§8Discord: §9discord.gg/https://discord.gg/pGKpvX5WMU"

        player.setPlayerListHeaderFooter(header, footer)
    }

    public fun pipelinedUpdateAll() {
        if (updateLock) return

        val players = Bukkit.getOnlinePlayers().toList()
        if (players.isEmpty()) return

        updateLock = true

        val perTick = Math.max(
            1,
            Math.ceil(players.size.toDouble() / Config.nametagPipelineTicks).toInt(),
        )

        var index = 0
        var delay = 0L

        while (index < players.size) {
            val start = index
            val end = Math.min(index + perTick, players.size)

            Bukkit.getScheduler().runTask(
                Nodes.plugin!!,
                Runnable {
                    for (i in start until end) {
                        val p = players[i]
                        if (p.isOnline) updateTabForPlayer(p)
                    }
                },
            )

            index += perTick
            delay++
        }

        Bukkit.getScheduler().runTaskLater(
            Nodes.plugin!!,
            Runnable {
                updateLock = false
            },
            delay,
        )
    }

    public fun onPlayerJoin(player: Player) {
        Bukkit.getScheduler().runTaskLater(
            Nodes.plugin!!,
            Runnable {
                pipelinedUpdateAll()
            },
            10L,
        )
    }

    public fun onPlayerQuit(player: Player) {
        sectionHeaders.remove(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(
            Nodes.plugin!!,
            Runnable {
                pipelinedUpdateAll()
            },
            5L,
        )
    }
}
