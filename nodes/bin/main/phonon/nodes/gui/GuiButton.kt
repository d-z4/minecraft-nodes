/**
 * GUI Button element, run specified callback on click
 */

package phonon.nodes.gui

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

public class GuiButton(
    val x: Int,
    val y: Int,
    val icon: ItemStack,
    val title: String?,
    val tooltip: List<String>?,
    val callback: (InventoryClickEvent) -> Unit,
) : GuiElement {

    // set icon meta
    init {
        if (title != null || tooltip != null) {
            val itemMeta = icon.getItemMeta()

            if (title != null) {
                itemMeta.setDisplayName(title)
            }
            if (tooltip != null) {
                itemMeta.setLore(tooltip)
            }

            icon.setItemMeta(itemMeta)
        }
    }

    public override fun onClick(event: InventoryClickEvent) {
        callback(event)
    }

    public override fun render(screen: GuiWindow) {
        screen.draw(this, x, y, icon)
    }
}
