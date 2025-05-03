package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.common.util.misc.Game;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class LiteBetaWarning {
    @SubscribeEvent
    public static void on(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Game.DEVELOPMENT_VERSION) {
            return;
        }
        boolean warned = event.player.getEntityData().getBoolean("RailwayDepotWorkerBetaWarning");
        if (!warned) {
            event.player.getEntityData().setBoolean("RailwayDepotWorkerBetaWarning", true);
            if (ModContainer.oneLineBetaWarning) {
                event.player.sendMessage(new TextComponentTranslation("railway_depot_worker.beta_warning.one_line").setStyle(new Style().setColor(TextFormatting.GRAY)));
            } else {
                for (int i = 1; i <= 8; i++) {
                    event.player.sendMessage(new TextComponentTranslation("railway_depot_worker.beta_warning." + i).setStyle(new Style().setColor(TextFormatting.RED)));
                }
            }
        }
    }
}
