package info.tritusk.modpack.railcraft.patcher.hooks;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

public class HopperCartHooks {

    // Called from EntityCartHopper to properly update the EntityItem on the ground.
    // Otherwise, the dropped item never decreases its count, causing dupe glitches.
    public static void handleItemRemainder(ItemStack remainder, EntityItem dropOnGround) {
        dropOnGround.setItem(remainder);
    }
}
