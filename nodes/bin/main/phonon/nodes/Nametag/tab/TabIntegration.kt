package phonon.nodes.nametags

import me.neznamy.tab.api.TabAPI
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import phonon.nodes.Nodes
import phonon.nodes.objects.Town
import phonon.nodes.utils.Color

/**
 * TAB integration using nation colors from town RGB values
 *
 * Uses true RGB hex colors (Minecraft 1.16+) so each nation can have
 * a unique color from their capital town's RGB value.
 *
 * Color hierarchy:
 * 1. If in a nation -> use nation capital's RGB color
 * 2. If no nation -> use town's own RGB color
 */
object TabIntegration {

    private val tabAPI: TabAPI? = try {
        TabAPI.getInstance()
    } catch (e: Exception) {
        null
    }

    /**
     * Update both nametag and tab list for a specific player
     * Shows their nation/town color to everyone using true RGB
     */
    fun updateTabForPlayer(player: Player) {
        val tabAPI = tabAPI ?: return
        val tabListManager = tabAPI.tabListFormatManager ?: return
        val nametagManager = tabAPI.nameTagManager ?: return
        val sortingManager = tabAPI.sortingManager ?: return

        val resident = Nodes.getResident(player) ?: return
        val town = resident.town
        val tabPlayer = tabAPI.getPlayer(player.uniqueId) ?: return

        if (town == null) {
            // Wilderness player - gray
            val grayColor = ChatColor.of("#808080")
            val prefix = "$grayColor[Wilderness] "
            tabListManager.setPrefix(tabPlayer, prefix)
            nametagManager.setPrefix(tabPlayer, prefix)
            sortingManager.forceTeamName(tabPlayer, "zzz_wilderness")
            return
        }

        // Get the RGB color to use (nation capital > town)
        val rgbColor = getColorForTown(town)
        val hexColor = rgbToHexColor(rgbColor)
        val displayName = town.nation?.name ?: town.name

        val prefix = "$hexColor[$displayName] "

        // Update both tab list and nametag
        tabListManager.setPrefix(tabPlayer, prefix)
        nametagManager.setPrefix(tabPlayer, prefix)

        // Sorting: group by nation, then town
        val sortingPrefix = getSortingPrefix(town)
        sortingManager.forceTeamName(tabPlayer, sortingPrefix)
    }

    /**
     * Update tab for all online players
     */
    fun updateAllPlayers() {
        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            updateTabForPlayer(player)
        }
    }

    /**
     * Get the RGB color for a town based on nation
     *
     * Simple logic:
     * - If in nation: use nation capital's RGB color
     * - If no nation: use town's own RGB color
     *
     * Each nation gets one color (from their capital)
     * Independent towns each have their own color
     */
    private fun getColorForTown(town: Town): Color {
        val nation = town.nation

        if (nation != null) {
            // Use nation capital's color
            return nation.capital.color
        }

        // Use town's own color (no nation)
        return town.color
    }

    /**
     * Convert RGB color to hex ChatColor
     * This gives us true RGB color support (16.7 million colors!)
     */
    private fun rgbToHexColor(color: Color): ChatColor {
        val r = color.r.coerceIn(0, 255)
        val g = color.g.coerceIn(0, 255)
        val b = color.b.coerceIn(0, 255)

        val hex = String.format("#%02x%02x%02x", r, g, b)
        return ChatColor.of(hex)
    }

    /**
     * Generate sorting prefix for tab list
     * Groups players by nation, then by town
     */
    private fun getSortingPrefix(town: Town): String {
        val nation = town.nation

        if (nation != null) {
            // Format: nationName_townName
            return "${nation.name.lowercase()}_${town.name.lowercase()}"
        }

        // Towns without nations come after nations but before wilderness
        return "zz_${town.name.lowercase()}"
    }
}
