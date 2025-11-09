/**
 * Generic filler color using Stained Glass Pane items
 *
 */

package phonon.nodes.gui

public class GuiFillColor(val x: Int, val y: Int, val color: GuiColor) : GuiElement {
    public override fun render(screen: GuiWindow) {
        screen.draw(this, x, y, GUI_STAINED_GLASS[color.ordinal])
    }
}
