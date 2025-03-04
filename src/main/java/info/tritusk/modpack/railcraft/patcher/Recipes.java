package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.api.crafting.Crafters;
import mods.railcraft.common.items.RailcraftItems;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_CARBON;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_IRON;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_NICKEL;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_SILVER;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_ZINC;

public final class Recipes {

    public static Supplier<ItemStack> cargoCartDismantleRemainder(Block original) {
        return () -> new ItemStack(Items.COMPARATOR);
    }

    public static void addExtraRecipes() {
        Crafters.rollingMachine()
                .newRecipe(RailcraftItems.CHARGE.getStack(ELECTRODE_NICKEL))
                .name("railway_depot_worker", "alt_nickel_electrode")
                .shaped("P",
                        "P",
                        "P",
                        'P', "plateNickel");

        Crafters.rollingMachine()
                .newRecipe(RailcraftItems.CHARGE.getStack(ELECTRODE_IRON))
                .name("railway_depot_worker", "alt_iron_electrode")
                .shaped("P",
                        "P",
                        "P",
                        'P', "plateIron");

        Crafters.rollingMachine()
                .newRecipe(RailcraftItems.CHARGE.getStack(ELECTRODE_ZINC))
                .name("railway_depot_worker", "alt_zinc_electrode")
                .shaped("P",
                        "P",
                        "P",
                        'P', "plateZinc");

        Crafters.rollingMachine()
                .newRecipe(RailcraftItems.CHARGE.getStack(ELECTRODE_CARBON))
                .name("railway_depot_worker", "alt_carbon_electrode")
                .shaped("P",
                        "P",
                        "P",
                        'P', Items.COAL);

        Crafters.rollingMachine()
                .newRecipe(RailcraftItems.CHARGE.getStack(ELECTRODE_SILVER))
                .name("railway_depot_worker", "alt_silver_electrode")
                .shaped("P",
                        "P",
                        "P",
                        'P', "plateSilver");
    }
}
