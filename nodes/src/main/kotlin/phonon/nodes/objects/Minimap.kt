/**
 * Minimap object for player
 *
 * Displays a fixed 11x11 chunk area around player
 * [-x, x] chunks in each direction, x in [3, 4, 5]
 *
 * Internally uses the Bukkit scoreboard api for rendering.
 * Limitations of API:
 * Team names: max 16 chars
 * Score names: max 40 chars
 *
 * Each glyph in minimap is 3 chars (color code "&x" = 2 chars)
 * For [-5, 5] extent -> 33 chars
 * For [-6, 6] extent -> 39 chars
 * Initial line symbol required to distinguish lines,
 * as a result [-5, 5] is max minimap size such that each line < 40 chars
 */

package phonon.nodes.objects

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import phonon.nodes.WorldMap
import phonon.nodes.nms.sendObjectiveCreate
import phonon.nodes.nms.sendObjectiveDisplay
import phonon.nodes.nms.sendObjectiveRemove
import phonon.nodes.nms.sendScore

// max min allowed sizes
private val MAP_RADIUS_MIN = 5
private val MAP_RADIUS_MAX = 9

// used to start each line in scoreboard
// ensures each line name is unique
private val LINE_ID = arrayOf(
    "${ChatColor.AQUA}${ChatColor.RESET}",
    "${ChatColor.BLACK}${ChatColor.RESET}",
    "${ChatColor.BLUE}${ChatColor.RESET}",
    "${ChatColor.DARK_AQUA}${ChatColor.RESET}",
    "${ChatColor.DARK_BLUE}${ChatColor.RESET}",
    "${ChatColor.DARK_GRAY}${ChatColor.RESET}",
    "${ChatColor.DARK_GREEN}${ChatColor.RESET}",
    "${ChatColor.DARK_PURPLE}${ChatColor.RESET}",
    "${ChatColor.DARK_RED}${ChatColor.RESET}",
    "${ChatColor.GOLD}${ChatColor.RESET}",
    "${ChatColor.GRAY}${ChatColor.RESET}",
    "${ChatColor.GREEN}${ChatColor.RESET}",
    "${ChatColor.LIGHT_PURPLE}${ChatColor.RESET}",
    "${ChatColor.RED}${ChatColor.RESET}",
    "${ChatColor.WHITE}${ChatColor.RESET}",
    "${ChatColor.YELLOW}${ChatColor.RESET}",
)

// line header displaying X edges
private val HEADER = arrayOf(
    "${ChatColor.RED}-3    0      3",
    "${ChatColor.RED}-4      0        4",
    "${ChatColor.RED}-5         0          5",
)

public class Minimap(
    val resident: Resident,
    val player: Player,
    var size: Int, // display square half extend, renders [-size, size]
) {
    // objective name for this minimap
    private val objectiveName = "nodes_minimap"

    init {
        // ensure size within limits [3..5]
        this.size = Math.min(5, Math.max(3, this.size))

        // create scoreboard
        player.sendObjectiveCreate(objectiveName, "Minimap")
        player.sendObjectiveDisplay(objectiveName, DisplaySlot.SIDEBAR)

        // initial render, get player current location
        val world = Bukkit.getWorlds()[0]
        val loc = this.player.getLocation()
        val coordX = kotlin.math.floor(loc.x).toInt()
        val coordZ = kotlin.math.floor(loc.z).toInt()
        val coord = Coord.fromBlockCoords(coordX, coordZ)

        this.render(coord)
    }

    // render minimap centered at coord (current player location)
    public fun render(coord: Coord) {
        // remove and recreate the objective
        player.sendObjectiveRemove(objectiveName)
        player.sendObjectiveCreate(objectiveName, "Minimap")
        player.sendObjectiveDisplay(objectiveName, DisplaySlot.SIDEBAR)

        // add header
        val headerEntry = HEADER[size - 3]
        player.sendScore(objectiveName, headerEntry, size + 1)

        // add map lines
        val size = this.size
        for ((i, y) in (size downTo -size).withIndex()) {
            val lineIdString = LINE_ID[i]
            val renderedLine = WorldMap.renderLine(resident, coord, coord.z - y, coord.x - size, coord.x + size)
            val entryName = "${lineIdString}$renderedLine"
            player.sendScore(objectiveName, entryName, y)
        }
    }

    public fun destroy() {
        // clear the display slot (hide from sidebar)
        player.sendObjectiveDisplay("", DisplaySlot.SIDEBAR)

        // remove the objective (this automatically clears all scores)
        player.sendObjectiveRemove(objectiveName)
    }
}
