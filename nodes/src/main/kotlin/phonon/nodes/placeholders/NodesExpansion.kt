import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import phonon.nodes.Nodes

class NodesExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String = "nodes"

    override fun getAuthor(): String = "YourName"

    override fun getVersion(): String = "1.0.0"

    override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
        if (player == null) return null

        val resident = Nodes.getResident(player) ?: return null
        val town = resident.town

        return when (identifier) {
            "town_name" -> town?.name ?: "No Town"
            "town_leader" -> town?.leader?.name ?: "No Leader"
            "town_population" -> town?.residents?.size?.toString() ?: "0"
            "town_claims" -> town?.territories?.size?.toString() ?: "0"
            else -> null
        }
    }
}
