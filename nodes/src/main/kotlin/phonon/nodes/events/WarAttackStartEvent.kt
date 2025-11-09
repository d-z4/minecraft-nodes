/**
 * Event that occurs when a flag attack starts
 */

package phonon.nodes.event

import org.bukkit.block.Block
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import phonon.nodes.objects.Territory
import phonon.nodes.objects.Town
import java.util.UUID

public class WarAttackStartEvent(
    public val attacker: UUID,
    public val attackingTown: Town,
    public val territory: Territory,
    public val block: Block,
) : Event(),
    Cancellable {

    private var isCancelled: Boolean = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(cancel: Boolean) {
        this.isCancelled = cancel
    }

    override fun getHandlers(): HandlerList = WarAttackStartEvent.handlers

    companion object {
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = WarAttackStartEvent.handlers
    }
}
