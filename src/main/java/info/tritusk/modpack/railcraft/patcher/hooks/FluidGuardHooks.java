package info.tritusk.modpack.railcraft.patcher.hooks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class FluidGuardHooks {

    private FluidGuardHooks() {}

    public static void rebuildFluidCandidates(Object self) {
        try {
            Class<?> clazz = Class.forName("mods.railcraft.common.fluids.FluidContainerHandler");
            Field initializedField = clazz.getDeclaredField("initialized");
            initializedField.setAccessible(true);
            initializedField.setBoolean(self, true);

            Field candidatesField = clazz.getDeclaredField("candidates");
            candidatesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> candidates = (List<ItemStack>) candidatesField.get(self);
            candidates.clear();

            for (Item item : ForgeRegistries.ITEMS) {
                try {
                    NonNullList<ItemStack> list = NonNullList.create();
                    item.getSubItems(CreativeTabs.SEARCH, list);
                    for (ItemStack stack : list) {
                        try {
                            if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                                candidates.add(stack);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static List<ItemStack> findCanDrain(Object self, FluidStack fluidStack) {
        try {
            Class<?> clazz = Class.forName("mods.railcraft.common.fluids.FluidContainerHandler");
            Field candidatesField = clazz.getDeclaredField("candidates");
            candidatesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> candidates = (List<ItemStack>) candidatesField.get(self);

            List<ItemStack> ret = new ArrayList<>();
            for (ItemStack itemStack : candidates) {
                try {
                    IFluidHandlerItem handler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                    if (handler != null && fluidStack.isFluidStackIdentical(handler.drain(fluidStack, false))) {
                        ret.add(itemStack);
                    }
                } catch (Throwable ignored) {
                }
            }
            return ret;
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }

    public static boolean apply(Object ingredient, ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            Class<?> clazz = Class.forName("mods.railcraft.common.util.crafting.FluidIngredient");
            Field fluidField = clazz.getDeclaredField("fluidStack");
            fluidField.setAccessible(true);
            FluidStack fluidStack = ((FluidStack) fluidField.get(ingredient)).copy();

            ItemStack checking = stack.copy();
            checking.setCount(1);
            IFluidHandlerItem handler = checking.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (handler == null) {
                return false;
            }
            return fluidStack.isFluidStackIdentical(handler.drain(fluidStack, false));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ItemStack getRemaining(Object ingredient, ItemStack original) {
        try {
            Class<?> clazz = Class.forName("mods.railcraft.common.util.crafting.FluidIngredient");
            Field fluidField = clazz.getDeclaredField("fluidStack");
            fluidField.setAccessible(true);
            FluidStack fluidStack = ((FluidStack) fluidField.get(ingredient)).copy();

            ItemStack ret = original.copy();
            ret.setCount(1);
            IFluidHandlerItem handler = ret.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (handler == null) {
                return original;
            }
            handler.drain(fluidStack, true);
            ItemStack container = handler.getContainer();
            return container == null || container.isEmpty() ? ItemStack.EMPTY : container;
        } catch (Throwable ignored) {
            return original;
        }
    }
}


