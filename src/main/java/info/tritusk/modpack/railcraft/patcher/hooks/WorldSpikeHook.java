package info.tritusk.modpack.railcraft.patcher.hooks;

import mods.railcraft.common.gui.slots.SlotIngredientMap;
import mods.railcraft.common.util.inventory.InventoryAdvanced;

public class WorldSpikeHook {
    public static InventoryAdvanced setInvStackLimit0(InventoryAdvanced self, int limit) {
        return self;
    }

    public static <T> SlotIngredientMap<T> setInvStackLimit0(SlotIngredientMap<T> self, int limit) {
        return self;
    }
}
