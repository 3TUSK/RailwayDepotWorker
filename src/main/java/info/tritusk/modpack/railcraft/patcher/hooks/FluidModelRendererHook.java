package info.tritusk.modpack.railcraft.patcher.hooks;

import info.tritusk.modpack.railcraft.patcher.ModContainer;
import mods.railcraft.client.render.models.resource.FluidModelRenderer;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FluidModelRendererHook {

    private static final Logger LOGGER = LogManager.getLogger(FluidModelRendererHook.class);

    /*
     * So, In FluidModelRenderer#loadTextures, Railcraft was trying to register texture for *all* fluids
     * into the main texture atlas. The exact intention behind this is not clear till these days, but
     * it could be a "fix" to some of the missing texture issues.
     *
     * However, reports shows that this presumed "fix" does not play well with ExtraUtils2, which will make
     * their Molten Demon Metal missing the texture instead.
     *
     * https://github.com/3TUSK/RailwayDepotWorker/issues/10
     *
     * This hook enables modpack creators to toggle the presumed "fix" as they need.
     *
     * TODO Figure out what exactly was happening behind the scene, and give a more robust fix.
     *  The presumed "fix" on its own seems to be correct; however, disabling this "fix" indeed solves
     *  the ExU texture issue.
     */

    public static void intercept(EventBus bus, Object originalListener) {
        if (ModContainer.disableFluidTextureFix) {
            if (originalListener == FluidModelRenderer.INSTANCE) {
                LOGGER.debug("Disable the presumed fluid texture fix.");
                return;
            } else {
                LOGGER.error("Wrong listener captured! Expected FluidModelRenderer.INSTANCE, got {}. The toggle will likely not working as expected. Please report this error.", originalListener);
            }
        }
        bus.register(originalListener);
    }
}
