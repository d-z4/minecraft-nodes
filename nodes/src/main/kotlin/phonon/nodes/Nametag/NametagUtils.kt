package phonon.nodes.nametags

import phonon.nodes.objects.Nametag
import phonon.nodes.objects.Nation
import phonon.nodes.objects.Resident
import phonon.nodes.objects.Town

/**
 * Utility functions to update nametags when game events occur
 */
object NametagUtils {

    /**
     * Called when a player changes towns (joins, leaves, etc.)
     */
    fun onPlayerTownChange(resident: Resident) {
        val player = resident.player() ?: return
        TabIntegration.updateTabForPlayer(player)
        Nametag.pipelinedUpdateAllText() // Update nametags for everyone
    }

    /**
     * Called when a town's data changes (name, etc.)
     */
    fun onTownDataChange(town: Town) {
        town.playersOnline.forEach { player ->
            TabIntegration.updateTabForPlayer(player)
        }
    }

    /**
     * Called when a town's COLOR changes
     * This needs to update all affected players:
     * - If town is an independent town: just that town's players
     * - If town is a nation capital: all players in that nation
     */
    fun onTownColorChange(town: Town) {
        val nation = town.nation

        // Check if this town is a nation capital
        if (nation != null && nation.capital == town) {
            // This town is a capital! Need to update whole nation
            nation.towns.forEach { nationTown ->
                nationTown.playersOnline.forEach { player ->
                    TabIntegration.updateTabForPlayer(player)
                }
            }
        } else {
            // Not a capital, just update this town's players
            town.playersOnline.forEach { player ->
                TabIntegration.updateTabForPlayer(player)
            }
        }
        town.updateNametags()
        Nametag.pipelinedUpdateAllText()
    }

    /**
     * Called when a nation is created or a town joins/leaves a nation
     */
    fun onNationMembershipChange(nation: Nation) {
        nation.towns.forEach { town ->
            town.playersOnline.forEach { player ->
                TabIntegration.updateTabForPlayer(player)
            }
        }
    }

    /**
     * Called when towns form or break alliances
     * Note: Town alliances don't affect colors, so this just updates for completeness
     */
    fun onTownAllianceChange(town1: Town, town2: Town) {
        // Town-to-town alliances don't change colors
        // Colors are only based on nations
        // But we can update in case other display logic needs it in the future
        town1.playersOnline.forEach { player ->
            TabIntegration.updateTabForPlayer(player)
        }
        town2.playersOnline.forEach { player ->
            TabIntegration.updateTabForPlayer(player)
        }
    }

    /**
     * Called when a nation's capital changes
     * Need to update all players in the nation
     */
    fun onNationCapitalChange(nation: Nation) {
        nation.towns.forEach { town ->
            town.playersOnline.forEach { player ->
                TabIntegration.updateTabForPlayer(player)
            }
        }
    }
}
