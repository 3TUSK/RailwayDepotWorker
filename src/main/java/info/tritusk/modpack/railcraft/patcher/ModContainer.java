package info.tritusk.modpack.railcraft.patcher;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import mods.railcraft.api.crafting.Crafters;
import mods.railcraft.common.items.RailcraftItems;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_CARBON;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_IRON;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_NICKEL;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_SILVER;
import static mods.railcraft.common.items.ItemCharge.EnumCharge.ELECTRODE_ZINC;

public class ModContainer extends DummyModContainer {
    public ModContainer() {
        super(new ModMetadata());
        final ModMetadata meta = this.getMetadata();
        meta.modId = "railway_depot_worker";
        meta.name = "Railway Depot Worker";
        meta.description = "Coremod to fix issues found in Railcraft. Railcraft dev for 1.12.2 is in hiatus, so...";
        meta.version = "1.4.0";
        meta.authorList = Collections.singletonList("3TUSK");
        meta.requiredMods = Collections.singleton(VersionParser.parseVersionReference("railcraft@(,12.1.0-beta-9)"));
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    // Caveat: due to how DefaultArtifactVersion::equals was implemented, the set returned here must
    // call == first before Object#equals when checking if an element is present or not.
    // Otherwise, game will not launch and throw a NullPointerException!
    @Override
    public Set<ArtifactVersion> getRequirements() {
        return new HashSet<>(this.getMetadata().requiredMods);
    }

    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
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
