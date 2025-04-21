package info.tritusk.modpack.railcraft.patcher.hooks;

import mods.railcraft.common.blocks.structures.StructurePattern;
import mods.railcraft.common.gui.slots.SlotIngredientMap;
import mods.railcraft.common.util.inventory.InventoryAdvanced;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

/**
 * All call back methods are found in this class.
 */
public final class Hooks {

    public static char getPatternMarker0(StructurePattern pattern, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) {
            return StructurePattern.EMPTY_MARKER;
        }
        if (x >= pattern.getPatternWidthX() || y >= pattern.getPatternHeight() || z >= pattern.getPatternWidthZ()) {
            return StructurePattern.EMPTY_MARKER;
        }
        return pattern.getPatternMarker(x, y, z);
    }

    // Called from EntityCartHopper to properly update the EntityItem on the ground.
    // Otherwise, the dropped item never decreases its count, causing dupe glitches.
    public static void handleItemRemainder(ItemStack remainder, EntityItem dropOnGround) {
        dropOnGround.setItem(remainder);
    }

    public static InventoryAdvanced setInvStackLimit0(InventoryAdvanced self, int limit) {
        return self;
    }

    public static <T> SlotIngredientMap<T> setInvStackLimit0(SlotIngredientMap<T> self, int limit) {
        return self;
    }
}
