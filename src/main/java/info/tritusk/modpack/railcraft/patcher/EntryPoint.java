package info.tritusk.modpack.railcraft.patcher;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(9001)
public class EntryPoint implements IFMLLoadingPlugin {

    static File modLocation;

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "info.tritusk.modpack.railcraft.patcher.Xformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return "info.tritusk.modpack.railcraft.patcher.ModContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        modLocation = (File) data.get("coremodLocation");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
