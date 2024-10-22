package info.tritusk.modpack.railcraft.patcher;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
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
}
