package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.api.core.ILocalizedObject;
import mods.railcraft.common.blocks.tracks.outfitted.TileTrackOutfitted;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;

import java.util.HashMap;
import java.util.Map;

public class I18nHook {
    public static String translateOutfittedTrackName(TileTrackOutfitted track) {
        String baseKey = "tile.railcraft.track_outfitted";
        String overrideKey = baseKey + "." + track.getTrackType().getName() + "." +  track.getTrackKitInstance().getTrackKit().getName() + ".name";
        if (LocalizationPlugin.hasTag(overrideKey))
            return LocalizationPlugin.translateFast(overrideKey);
        Map<String, ILocalizedObject> args = new HashMap<>();
        args.put("track_type", track.getTrackType());
        args.put("track_kit", track.getTrackKitInstance().getTrackKit());
        return LocalizationPlugin.translateArgs(baseKey + ".name", args);
    }
}
