package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.common.items.firestone.FirestoneTools;
import mods.railcraft.common.util.inventory.InventoryComposite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AlternativeFirestoneTicker {

    private static final Logger LOGGER = LogManager.getLogger(AlternativeFirestoneTicker.class);

    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.side != Side.SERVER) {
            return;
        }
        EntityPlayer player = event.player;
        if ((player.world.getTotalWorldTime() + player.getEntityId()) % 4 != 0) {
            return;
        }
        if (player.openContainer != player.inventoryContainer) {
            return;
        }
        InventoryComposite.of(player).streamStacks().forEach(stack -> FirestoneTools.trySpawnFire(player.world, player.getPosition(), stack, player));
    }

    public static void intercept(EventBus bus, Object originalEventListener) {
        if (ModContainer.useAlternativeFirestoneTicker) {
            LOGGER.info("Registering alternative firestone ticker. Old listener: {}", originalEventListener);
            bus.register(AlternativeFirestoneTicker.class);
        } else {
            LOGGER.info("Use original firestone ticker.");
            bus.register(originalEventListener);
        }
    }
}
