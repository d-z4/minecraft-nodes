package phonon.nodes.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import phonon.nodes.Config
import phonon.nodes.Message
import phonon.nodes.Nodes.getRelationshipOfPlayerToPlayer
import phonon.nodes.constants.DiplomaticRelationship

public class NodesPlayerDamageListener : Listener {

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        val attacker = event.damager

        if (victim !is Player || attacker !is Player) return

        // if relationship is ally, town or nation, and config specifies it, cancel event and notify attacker
        val relationship = getRelationshipOfPlayerToPlayer(victim, attacker)
        val (cancel, message) = when (relationship) {
            DiplomaticRelationship.TOWN, DiplomaticRelationship.NATION -> {
                val cancelled = !Config.allowNationFriendlyFire
                cancelled to if (cancelled) "You cannot attack members of your nation" else ""
            }
            DiplomaticRelationship.ALLY -> {
                val cancelled = !Config.allowAllyFriendlyFire
                cancelled to if (cancelled) "You cannot attack your allies" else ""
            }
            else -> false to ""
        }

        if (cancel) {
            event.setCancelled(true)
            Message.error(attacker, message)
        }
    }
}
