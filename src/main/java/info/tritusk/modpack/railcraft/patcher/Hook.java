package info.tritusk.modpack.railcraft.patcher;

import mezz.jei.api.gui.ICraftingGridHelper;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;

import java.lang.reflect.Field;
import java.util.List;

public class Hook {

    private static Class<?> railcraftRecipeWrapper;
    private static Field realRecipeRef;
    private static Class<?> railcraftRollingRecipeType;
    private static Field wrappedRecipeRef;
    private static boolean tried = false;

    public static void setInputs0(ICraftingGridHelper craftingGridHelper,
                                  IGuiItemStackGroup guiItemStackGroup,
                                  List<List<ItemStack>> input,
                                  IRecipeWrapper wrapper) {
        if (!tried && railcraftRecipeWrapper == null) {
            tried = true;
            try {
                railcraftRecipeWrapper = Class.forName("mods.railcraft.common.plugins.jei.RailcraftJEIPlugin$DefaultRecipeWrapper");
                realRecipeRef = railcraftRecipeWrapper.getDeclaredField("recipe");
                realRecipeRef.setAccessible(true);

                railcraftRollingRecipeType = Class.forName("mods.railcraft.common.util.crafting.RollingMachineCrafter$RollingRecipe");
                wrappedRecipeRef = railcraftRollingRecipeType.getDeclaredField("recipe");
                wrappedRecipeRef.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        boolean success = false;
        if (railcraftRecipeWrapper != null && railcraftRecipeWrapper.isInstance(wrapper)) {
            try {
                IRecipe underlyingRecipe = (IRecipe) realRecipeRef.get(wrapper);
                if (railcraftRollingRecipeType != null && railcraftRollingRecipeType.isInstance(underlyingRecipe)) {
                    underlyingRecipe = (IRecipe) wrappedRecipeRef.get(underlyingRecipe);
                }
                if (underlyingRecipe instanceof ShapedRecipes) {
                    int width = ((ShapedRecipes) underlyingRecipe).getWidth();
                    int height = ((ShapedRecipes) underlyingRecipe).getHeight();
                    // Railcraft does not give any info about the recipe width and height,
                    // so JEI has to guess on its own.
                    // JEI will assume any recipes that uses 1~4 ingredients can be fit in
                    // a 2*2 grid, which isn't true for cases like vanilla shovels.
                    // To make JEI stop guessing, we need to give it the width and height.
                    // So here it is.
                    craftingGridHelper.setInputs(guiItemStackGroup, input, width, height);
                    success = true;
                }
            } catch (IllegalAccessException e) {
                // ignore i guess
                e.printStackTrace(System.err);
            }
        }
        if (!success) {
            // This is the old logic, and we know it doesn't work quite well.
            // Leave it here as if nothing happened.
            craftingGridHelper.setInputs(guiItemStackGroup, input);
        }
    }
}
