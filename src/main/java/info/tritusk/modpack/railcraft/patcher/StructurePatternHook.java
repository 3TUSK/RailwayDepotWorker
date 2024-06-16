package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.common.blocks.structures.StructurePattern;

public class StructurePatternHook {

    public static char getPatternMarker0(StructurePattern pattern, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) {
            return StructurePattern.EMPTY_MARKER;
        }
        if (x >= pattern.getPatternWidthX() || y >= pattern.getPatternHeight() || z >= pattern.getPatternWidthZ()) {
            return StructurePattern.EMPTY_MARKER;
        }
        return pattern.getPatternMarker(x, y, z);
    }
}
