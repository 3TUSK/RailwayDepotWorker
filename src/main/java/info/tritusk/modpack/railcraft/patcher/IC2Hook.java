package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.common.blocks.logic.IC2EmitterLogic;
import mods.railcraft.common.plugins.ic2.IC2Plugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.world.World;

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
            IC2Plugin.removeTileFromNet(logic);
        } catch (Throwable error) {
            Game.log().api("IndustrialCraft", error);
        }
    }
}
