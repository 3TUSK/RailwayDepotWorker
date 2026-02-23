package info.tritusk.modpack.railcraft.patcher;

import mods.railcraft.api.core.ILocalizedObject;
import mods.railcraft.api.core.IVariantEnum;
import mods.railcraft.common.blocks.ore.ItemOreMagic;
import mods.railcraft.common.blocks.tracks.outfitted.TileTrackOutfitted;
import mods.railcraft.common.core.RailcraftConstants;
import mods.railcraft.common.plugins.forge.LocalizationPlugin;
import net.minecraft.item.ItemStack;

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

    public static String translateMagicOreName(ItemOreMagic oreItem, ItemStack theItem) {
        IVariantEnum variant = oreItem.getVariant(theItem);
        // GH-29: Firestone missing localized name
        // Railcraft has a check against number of variants;
        // if just one (1), Railcraft will not append variant name after translation key.
        // For Firestone, the block (the "magic ore") has exactly 1 variant, but in language
        // files, the translation key for firestone block has variant in it.
        // We thus fix the missing localized name issue by removing the check, forcing the
        // variant name to be in the key.
        if (variant == null) {
            return oreItem.getTranslationKey();
        }
        String tag = oreItem.getTranslationKey() + RailcraftConstants.SEPERATOR + variant.getResourcePathSuffix();
        return LocalizationPlugin.convertTag(tag);
    }
}
