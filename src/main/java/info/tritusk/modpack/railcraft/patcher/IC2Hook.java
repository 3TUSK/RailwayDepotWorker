package info.tritusk.modpack.railcraft.patcher;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import mods.railcraft.common.blocks.logic.IC2EmitterLogic;
import mods.railcraft.common.plugins.ic2.IC2Plugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class IC2Hook {

    public static void eNetCallback(IC2EmitterLogic logic, boolean isCtrl, boolean completed, World world) {
        if (Game.isHost(world)) {
            if (isCtrl) {
                if (completed) {
                    logic.addToNet();
                } else {
                    logic.dropFromNet();
                }
            }
        }
    }

    public static void addToENet0(IC2EmitterLogic logic) {
        try {
            logic.rebuildSubTiles();
            IC2Plugin.addTileToNet(logic);
        } catch (Throwable t) {
            Game.log().api("IndustrialCraft", t);
        }
    }

    public static void removeFromENet0(IC2EmitterLogic logic) {
        try {
            // GH-14: always check the presence of EnergyNet.instance before dispatching event.
            // This ensures we won't get NPE because of uninitialized API.
            // Unfortunately, the IC2 API is always present because Railcraft repacks IC2 API.
            if (EnergyNet.instance != null) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(logic));
            }
        } catch (Throwable error) {
            Game.log().api("IC2", error, EnergyTileUnloadEvent.class);
        }
    }
}
