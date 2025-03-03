package info.tritusk.modpack.railcraft.patcher.hooks;

import info.tritusk.modpack.railcraft.patcher.ModContainer;
import mods.railcraft.client.render.models.resource.FluidModelRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FluidModelRendererHook {

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
     * The root issue has two parts:
     * 1. Railcraft did not check if a TextureAtlasSpirit has already been registered under the given
     *    flowing/still texture location.
     *    By default, TextureMap#registerSprite works in a "first one wins all" manner - all subsequent
     *    calls to register a texture under the same name are effectively ignored.
     *    This silently voids ExtraUtils2's registration of custom texture.
     * 2. Railcraft's listener has a NORMAL priority, the same as ExtraUtil2's listener.
     *    There is no listener sorting mechanism other than the 5-level priority system.
     *    The execution order of listeners in the same priority is undefined behavior; it can be of any order.
     *    In execution, Railcraft's one typically comes before ExtraUtils2's, making Railcraft's texture
     *    registration win.
     *    If, by any reason, ExtraUtil2's listener is sorted before Railcraft's, the ExtraUtil2 will successfully
     *    register custom texture for their fluids.
     *
     * To fully address the issue, Railway Depot Worker offers these toggles:
     * - A toggle to turn on/off the presumed "fix" as needed.
     * - An alternative implementation of FluidModelRenderer#loadTextures, with proper priority and checks against
     *   existing spirits.
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
        bus.register(ModContainer.useAlternativeFluidTextureFix ? AltLoadTextureHook.class : originalListener);
    }

    /**
     * Alternative implementation of {@link FluidModelRenderer#loadTextures(TextureStitchEvent.Pre)}, in case this
     * logic is needed by anyone.
     */
    public static final class AltLoadTextureHook {
        // Use the lowest priority to make sure we run after everyone else.
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void on(TextureStitchEvent.Pre event) {
            TextureMap textureMap = event.getMap();
            // Check all registered fluids.
            for (Fluid f : FluidRegistry.getRegisteredFluids().values()) {
                // Check their flowing texture
                ResourceLocation flowingTex = f.getFlowing();
                // If not registered yet, register one as default.
                if (flowingTex != null && textureMap.getTextureExtry(flowingTex.toString()) == null) {
                    textureMap.registerSprite(flowingTex);
                }
                // Check their still texture.
                ResourceLocation stillTex = f.getStill();
                // If not registered yet, register one as default.
                if (stillTex != null && textureMap.getTextureExtry(stillTex.toString()) == null) {
                    textureMap.registerSprite(stillTex);
                }
            }
        }
    }
}
