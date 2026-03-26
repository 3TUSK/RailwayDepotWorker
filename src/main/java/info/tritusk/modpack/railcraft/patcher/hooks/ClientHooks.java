package info.tritusk.modpack.railcraft.patcher.hooks;

import mods.railcraft.client.gui.GuiBoiler;
import mods.railcraft.common.gui.containers.ContainerBoilerFluid;

/** Hooks that refer physical client-only classes are placed here. */
public final class ClientHooks {

    // GH-31: The fire indicator in the GUI of Liquid Firebox is 16 pixels below,
    // comparing to the Solid Firebox counterpart.
    // So, we shift it down if we know it is a liquid one.
    public static int fixBoilerGUIFireOffset(int originalOffset, GuiBoiler gui) {
        return gui.inventorySlots instanceof ContainerBoilerFluid ? originalOffset + 16 : originalOffset;
    }
}
