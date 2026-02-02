package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.api.crafting.Crafters;
import mods.railcraft.api.crafting.IBlastFurnaceCrafter;
import mods.railcraft.common.items.Metal;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.util.crafting.BlastFurnaceCrafter;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.List;
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

    public static void fixBlastFurnaceRecipe() {
        List<IBlastFurnaceCrafter.IRecipe> recipes = BlastFurnaceCrafter.INSTANCE.getRecipes();
        if (recipes.removeIf(recipe -> "railcraft".equals(recipe.getName().getNamespace()) && "smelt_trapdoor".equals(recipe.getName().getPath()))) {
            BlastFurnaceCrafter.INSTANCE.newRecipe(Blocks.IRON_TRAPDOOR)
                    .name("railcraft:smelt_trapdoor")
                    .output(RailcraftItems.INGOT.getStack(4, Metal.STEEL))
                    .slagOutput(4)
                    .time(IBlastFurnaceCrafter.SMELT_TIME * 4)
                    .register();
        }

        if (recipes.removeIf(recipe -> "railcraft".equals(recipe.getName().getNamespace()) && "smelt_door".equals(recipe.getName().getPath()))) {
            BlastFurnaceCrafter.INSTANCE.newRecipe(Items.IRON_DOOR)
                    .name("railcraft:smelt_door")
                    .output(RailcraftItems.INGOT.getStack(2, Metal.STEEL))
                    .slagOutput(2)
                    .time(IBlastFurnaceCrafter.SMELT_TIME * 2)
                    .register();
        }
    }
}
