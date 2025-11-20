package info.tritusk.modpack.railcraft.patcher.hooks;

import mods.railcraft.common.blocks.logic.BoilerLogic;
import mods.railcraft.common.blocks.logic.FluidLogic;
import mods.railcraft.common.blocks.logic.Logic;
import mods.railcraft.common.blocks.structures.StructurePattern;
import mods.railcraft.common.blocks.structures.TileBoilerFireboxSolid;
import mods.railcraft.common.carts.EntityLocomotiveSteam;
import mods.railcraft.common.fluids.TankManager;
import mods.railcraft.common.gui.containers.RailcraftContainer;
import mods.railcraft.common.gui.slots.SlotIngredientMap;
import mods.railcraft.common.gui.slots.SlotRailcraft;
import mods.railcraft.common.util.inventory.IInventoryImplementor;
import mods.railcraft.common.util.inventory.InventoryAdvanced;
import mods.railcraft.common.util.inventory.wrappers.InventoryMapper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
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

    public static void initSteamLocomotive(EntityLocomotiveSteam locomotive, Logic.Adapter adapter, InventoryMapper waterHandlerInv) {
        locomotive.boiler.addLogic(new WaterHandlerLogicWrapper(adapter, waterHandlerInv));
    }

    public static void fixSolidBoilerSlotIndex(RailcraftContainer container, Slot slot) {
        // 1. Check if we get the 3rd fuel slot
        if (slot.xPos == 89 && slot.yPos == 56) {
            // 2. Then check if it points to the wrong slot
            if (slot.getSlotIndex() == TileBoilerFireboxSolid.SLOT_BUNKER_B) {
                // 3. If so, change it to the correct one
                slot = new SlotRailcraft(slot.inventory, TileBoilerFireboxSolid.SLOT_BUNKER_C, 89, 56);
            }
        }
        // 4. Proceed to add the slot
        container.addSlot(slot);
    }

    public static TankManager getTankManagerFromBoilerLogic(BoilerLogic logic) {
        return logic.getLogic(FluidLogic.class).orElseThrow(RuntimeException::new).getTankManager();
    }

    /**
     * A dummy {@link Logic} implementation that also implements {@link IInventoryImplementor}, used for exposing
     * an inventory as a {@code Logic} instance.
     */
    private static final class WaterHandlerLogicWrapper extends Logic implements IInventoryImplementor {

        /*
         * Implementation summary
         *
         * In EntityLocomotiveSteam, when constructing instance, an instance of BucketProcessorLogic is
         * attached to its boiler logic. This BucketProcessorLogic attempts to find "water input slots"
         * from all sub-logic attached to it, cast to IInventoryImplementor type and then use it as a
         * source to pull water from.
         *
         * In short, to make the water input slots usable, we need to make boiler logic to find this
         * inventory. To make the boiler logic find this inventory, we need to wrap it as an instance
         * of Logic.
         *
         * Thus this implementation.
         */

        private final InventoryMapper waterHandler;

        public WaterHandlerLogicWrapper(Adapter adapter, InventoryMapper waterHandler) {
            super(adapter);
            this.waterHandler = waterHandler;
        }

        @Override
        public IInventory getInventory() {
            return this.waterHandler;
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer entityPlayer) {
            return false;
        }
    }
}
