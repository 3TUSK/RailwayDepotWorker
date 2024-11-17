package info.tritusk.modpack.railcraft.patcher;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.VersionParser;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModContainer extends DummyModContainer {

    public static boolean useAlternativeFirestoneTicker = true;

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
    public void construct(FMLConstructionEvent event) {
        File cfgFile = new File(Loader.instance().getConfigDir(), "railway_depot_worker.cfg");
        Configuration config = new Configuration(cfgFile);
        config.load();
        Property prop = config.get("general", "useAlternativeFirestoneTicker", true);
        prop.setRequiresMcRestart(true);
        useAlternativeFirestoneTicker = prop.getBoolean();

        if (config.hasChanged()) {
            config.save();
        }
    }

    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
        Recipes.addExtraRecipes();
    }

    /*
     * Special thanks to devs of GregicalityStarbound for the example use of ForgeChunkManager.savedWorldHasForcedChunkTickets
     * The original code is licenced under the MIT license.
     * https://github.com/SymmetricDevs/GregicalityStarbound/blob/master/LICENSE
     * Code used here originally locates at:
     * https://github.com/SymmetricDevs/GregicalityStarbound/blob/45ca94df949480d5e5a6baadbe18e2ac092a061d/src/main/java/com/starl0stgaming/gregicalitystarbound/api/space/planets/Planet.java#L74
     */
    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        // After server started, we attempt to load all dimensions that has forced chunk tickets
        // managed by Forge.
        // Doing so ensures that MystCraft dimensions that has Railcraft Worldspike (aka. World Anchor)
        // will be correctly revived on server restart.
        // As a bonus, any chunk loaders that experience similar issue can also benefit from this fix.
        // One notable exception is ChickenChunks: it has its own dimension revival logic.
        // Step 1: go through all registered dimensions
        for (Integer i : DimensionManager.getStaticDimensionIDs()) {
            World theWorld = DimensionManager.getWorld(i);
            // Step 2: skip loaded worlds.
            // Loaded worlds can be retrieved from DimensionManager.
            if (theWorld != null) {
                continue;
            }
            // Step 3: check if the dimension has forced chunk tickets
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();
            String dimDirName = DimensionManager.createProviderFor(i).getSaveFolder();
            // Only overworld can return null here. Skip if that ever happens.
            if (dimDirName == null) {
                continue;
            }
            File dimDir = new File(saveDir, dimDirName);
            if (ForgeChunkManager.savedWorldHasForcedChunkTickets(dimDir)) {
                // Step 4: if tickets found, load the dimension now.
                // ForgeChunkManager will handle the chunk-loader revival.
                DimensionManager.initDimension(i);
            }
        }
    }
}
