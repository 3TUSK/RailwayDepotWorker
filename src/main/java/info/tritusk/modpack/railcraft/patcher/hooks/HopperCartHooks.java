package info.tritusk.modpack.railcraft.patcher.hooks;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

public class HopperCartHooks {

    public static void handleItemRemainder(ItemStack remainder, EntityItem dropOnGround) {
        dropOnGround.setItem(remainder);
    }
}
