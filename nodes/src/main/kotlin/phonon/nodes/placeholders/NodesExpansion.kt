package phonon.nodes.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import phonon.nodes.Nodes
import phonon.nodes.objects.Nation
import phonon.nodes.objects.Town
import phonon.nodes.utils.Color
import kotlin.toString

/**
 * PlaceholderAPI expansion for Nodes plugin
 * Provides placeholders for town/nation info with RGB colors
 */
class NodesExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String = "nodes"
    override fun getAuthor(): String = "YourName"
    override fun getVersion(): String = "1.0.5"
    override fun persist(): Boolean = true
    override fun canRegister(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if (player == null) return null

        val resident = Nodes.getResident(player) ?: return null
        val town = resident.town

        val idLower = identifier.lowercase()

        return when {
            // Basic town info
            idLower == "town_name" -> town?.name ?: "Wilderness"
            idLower == "town_leader" -> town?.leader?.name ?: "None"

            idLower == "town_population" -> town?.residents?.size?.toString() ?: "0"
            idLower == "town_claims" -> town?.territories?.size?.toString() ?: "0"

            // Nation info
            idLower == "nation_name" -> town?.nation?.name ?: "None"
            idLower == "nation_capital" -> town?.nation?.capital?.name ?: "None"

            // RGB hex color (matches tab list color)

            idLower == "town_color" -> {
                if (town == null) {
                    val grayColor = ChatColor.of("#808080")
                    "$grayColor"
                } else {
                    val nation = town.nation
                    if (nation != null) {
                        // Use nation capital's color
                        val capital = nation.capital
                        val hexColor = rgbToHexColor(capital.color)
                        "$hexColor"
                    } else {
                        // Use town's own color
                        val hexColor = rgbToHexColor(town.color)
                        "$hexColor"
                    }
                }
            }

            // Display name (what shows in tab/nametag)
            idLower == "display_name" -> {
                if (town == null) {
                    "Wilderness"
                } else {
                    town.nation?.name ?: town.name
                }
            }

            idLower == "town_name_formatted" -> {
                val town = resident.town ?: return "${ChatColor.GRAY}Wilderness"
                val relationColor = getRelationColor(player, town)
                "$relationColor${town.name}"
            }

            // Nation name formatted with relation color
            idLower == "nation_name_formatted" -> {
                val nation = resident.town?.nation ?: return "${ChatColor.GRAY}Wilderness"
                val relationColor = getNationRelationColor(player, nation)
                "$relationColor${nation.name}"
            }

            idLower == "town_relation_color" -> {
                val town = resident.town ?: return "${ChatColor.GRAY}"
                getRelationColor(player, town).toString()
            }

            idLower == "nation" -> {
                resident.nation?.name ?: ""
            }

            else -> null
        }
    }

    /**
     * Get the relation color between the viewing player and a target nation
     */
    private fun getNationRelationColor(viewer: Player, targetNation: Nation): org.bukkit.ChatColor {
        val viewerResident = Nodes.getResident(viewer) ?: return org.bukkit.ChatColor.GRAY
        val viewerTown = viewerResident.town ?: return org.bukkit.ChatColor.GRAY
        val viewerNation = viewerTown.nation

        // Same nation = dark green
        if (viewerNation != null && viewerNation == targetNation) {
            return org.bukkit.ChatColor.DARK_GREEN
        }

        // Check if any town in target nation is ally/enemy
        for (nationTown in targetNation.towns) {
            if (viewerTown.allies.contains(nationTown)) {
                return org.bukkit.ChatColor.DARK_AQUA
            }
            if (viewerTown.enemies.contains(nationTown)) {
                return org.bukkit.ChatColor.DARK_RED
            }
        }

        // Neutral = gold
        return org.bukkit.ChatColor.GOLD
    }

    /**
     * Get the relation color between the viewing player and a target town
     *
     * Colors:
     * - §a (GREEN) - Member of SAME town
     * - §2 (DARK_GREEN) - Member of same NATION
     * - §3 (DARK_AQUA) - ALLY
     * - §4 (DARK_RED) - ENEMY
     * - §6 (GOLD) - NEUTRAL
     * - §7 (GRAY) - No town
     */
    private fun getRelationColor(viewer: Player, targetTown: Town): org.bukkit.ChatColor {
        val viewerResident = Nodes.getResident(viewer) ?: return org.bukkit.ChatColor.GRAY
        val viewerTown = viewerResident.town ?: return org.bukkit.ChatColor.GRAY

        // Same town = bright green
        if (viewerTown == targetTown) {
            return org.bukkit.ChatColor.GREEN
        }

        // Same nation = dark green
        val viewerNation = viewerTown.nation
        val targetNation = targetTown.nation
        if (viewerNation != null && viewerNation == targetNation) {
            return org.bukkit.ChatColor.DARK_GREEN
        }

        // Ally = dark aqua
        if (viewerTown.allies.contains(targetTown)) {
            return org.bukkit.ChatColor.DARK_AQUA
        }

        // Enemy = dark red
        if (viewerTown.enemies.contains(targetTown)) {
            return org.bukkit.ChatColor.DARK_RED
        }

        // Neutral = gold
        return org.bukkit.ChatColor.GOLD
    }

    /**
     * Convert RGB color to hex ChatColor (matches TabIntegration logic)
     */
    private fun rgbToHexColor(color: Color): ChatColor {
        val r = color.r.coerceIn(0, 255)
        val g = color.g.coerceIn(0, 255)
        val b = color.b.coerceIn(0, 255)

        val hex = String.format("#%02x%02x%02x", r, g, b)
        return ChatColor.of(hex)
    }
}
