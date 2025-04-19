package info.tritusk.modpack.railcraft.patcher;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.function.BiFunction;

public class Xformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName == null) {
            return basicClass;
        }
        switch (transformedName) {
            case "mods.railcraft.common.plugins.jei.rolling.RollingMachineRecipeCategory": return tryFixRollingRecipeDisplayInJEI(basicClass);
            case "mods.railcraft.common.blocks.TileRailcraft": return tryPatchingTileRailcraft(basicClass);
            case "mods.railcraft.common.blocks.machine.worldspike.TileWorldspike": return tryExpandStackSizeLimitInWorldSpike(basicClass);
            case "mods.railcraft.common.blocks.structures.StructurePattern": return tryFixStructurePatternCheck(basicClass);
            case "mods.railcraft.common.blocks.logic.IC2EmitterLogic": return tryFixIC2EmitterLogic(basicClass);
            case "mods.railcraft.common.blocks.machine.manipulator.TileRFLoader":
            case "mods.railcraft.common.blocks.machine.manipulator.TileRFUnloader": return tryReenableRFManipulatorGUI(basicClass);
            case "mods.railcraft.common.carts.EntityCartHopper": return tryFixHopperCartDupe(basicClass);
            case "mods.railcraft.common.carts.MinecartHooks": return tryFixCartInvDuplication(basicClass);
            case "mods.railcraft.common.carts.RailcraftCarts": return tryFixCargoCartDismantleRecipe(basicClass);
            case "mods.railcraft.common.gui.containers.RailcraftContainer": return tryPatchRailcraftContainer(basicClass);
            case "mods.railcraft.client.core.ClientProxy": return tryFixFluidTextureWithThirdPartyMods(basicClass);
            case "mods.railcraft.common.gui.containers.ContainerWorldspike": return tryExpandStackSizeLimitInWorldSpikeGUI(basicClass);
            case "mods.railcraft.client.gui.GuiAnvil": return tryFixAnvilScreen(basicClass);
            case "mods.railcraft.client.gui.GuiTrackDelayedLocking":
            case "mods.railcraft.client.gui.GuiTrackEmbarking":
            case "mods.railcraft.client.gui.GuiTrackLauncher":
            case "mods.railcraft.client.gui.GuiTrackPriming": return tryUseI18nForTrackGui(basicClass);
            case "mods.railcraft.client.gui.GuiTrackActivator": return tryMakingActivatorTrackGUIBetter(basicClass);
            case "mods.railcraft.client.gui.GuiTrackRouting": return tryFixGuiRouting(basicClass);
            case "mods.railcraft.client.gui.GuiManipulatorCartRF": return tryDisableInvTitle(basicClass);
            case "mods.railcraft.common.modules.ModuleMagic$1": return tryReplaceFirestoneTicker(basicClass);
            default: return basicClass;
        }
    }

    private byte[] tryExpandStackSizeLimitInWorldSpikeGUI(byte[] basicClass) {
        return patch(basicClass, "<init>", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL && "setStackLimit".equals(name)) {
                    // Redirect the setInventoryStackLimit(16) call to our impl, which in turn voids the effect.
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/hooks/WorldSpikeHook";
                    name = "setInvStackLimit0";
                    desc = "(Lmods/railcraft/common/gui/slots/SlotIngredientMap;I)Lmods/railcraft/common/gui/slots/SlotIngredientMap;";
                    itf = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryFixCargoCartDismantleRecipe(byte[] basicClass) {
        return patch(basicClass, "<clinit>", (api, mv) -> new MethodVisitor(api, mv) {
            private final String TRAPPED_CHEST = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName("net/minecraft/init/Blocks", "field_150447_bR", "Lnet/minecraft/block/Block;");
            private boolean foundTrappedChest = false;

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (TRAPPED_CHEST.equals(name)) {
                    this.foundTrappedChest = true;
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if ("from".equals(name) && this.foundTrappedChest) {
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/Recipes";
                    name = "cargoCartDismantleRemainder";
                    desc = "(Lnet/minecraft/block/Block;)Ljava/util/function/Supplier;";
                    this.foundTrappedChest = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryFixFluidTextureWithThirdPartyMods(byte[] basicClass) {
        return patch(basicClass, "initializeClient", (api, mv) -> new MethodVisitor(api, mv) {
            private int registerRefCount = 0;
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if ("net/minecraftforge/fml/common/eventhandler/EventBus".equals(owner) && "register".equals(name) && ++this.registerRefCount == 3) {
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/hooks/FluidModelRendererHook";
                    name = "intercept";
                    desc = "(Lnet/minecraftforge/fml/common/eventhandler/EventBus;Ljava/lang/Object;)V";
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryFixHopperCartDupe(byte[] basicClass) {
        return patch(basicClass, "transferAndNeedsCooldown", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.POP) {
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/hooks/HopperCartHooks", "handleItemRemainder",
                            "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/item/EntityItem;)V", false);
                    return;
                }
                super.visitInsn(opcode);
            }
        });
    }

    private byte[] tryReplaceFirestoneTicker(byte[] basicClass) {
        return patch(basicClass, "preInit", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if ("net/minecraftforge/fml/common/eventhandler/EventBus".equals(owner) && "register".equals(name)) {
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/AlternativeFirestoneTicker";
                    name = "intercept";
                    desc = "(Lnet/minecraftforge/fml/common/eventhandler/EventBus;Ljava/lang/Object;)V";
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryFixCartInvDuplication(byte[] basicClass) {
        return patch(basicClass, "onMinecartSpawn", (api, mv) -> new MethodVisitor(api, mv) {
            final String entitySetDead = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName("net/minecraft/entity/Entity", "func_70106_y", "()V");
            final String entitySetDropLoot = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName("net/minecraft.entity/Entity", "func_184174_b", "(Z)V");
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL && entitySetDead.equals(name)) {
                    super.visitInsn(Opcodes.DUP);
                    super.visitInsn(Opcodes.ICONST_0);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/entity/Entity", entitySetDropLoot, "(Z)V", false);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private static byte[] tryReenableRFManipulatorGUI(byte[] basicClass) {
        return patch(basicClass, "openGui", (api, mv) -> new MethodVisitor(api, mv) {

            final String teWorldHolder = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName("net/minecraft/tileentity/TileEntity", "field_145850_b", "Lnet/minecraft/world/world;");

            private boolean openGuiCall = false;

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if ("mods/railcraft/common/gui/GuiHandler".equals(owner) && "openGui".equals(name)) {
                    this.openGuiCall = true;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.IRETURN && !this.openGuiCall) {
                    super.visitFieldInsn(Opcodes.GETSTATIC, "mods/railcraft/common/gui/EnumGui", "MANIPULATOR_RF", "Lmods/railcraft/common/gui/EnumGui;");
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/machine/manipulator/TileRFManipulator", this.teWorldHolder, "Lnet/minecraft/world/World;");
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "mods/railcraft/common/blocks/machine/manipulator/TileRFManipulator", "getX", "()I", false);
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "mods/railcraft/common/blocks/machine/manipulator/TileRFManipulator", "getY", "()I", false);
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "mods/railcraft/common/blocks/machine/manipulator/TileRFManipulator", "getZ", "()I", false);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "mods/railcraft/common/gui/GuiHandler", "openGui", "(Lmods/railcraft/common/gui/EnumGui;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;III)V", false);
                }
                super.visitInsn(opcode);
            }
        });
    }

    private static byte[] tryFixIC2EmitterLogic(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new IC2EmitterLogicPatcher(Opcodes.ASM5, writer), 0);
        return writer.toByteArray();
    }

    private static byte[] tryFixStructurePatternCheck(byte[] basicClass) {
        return patch(basicClass, "getPatternMarker", "(Lnet/minecraft/util/math/BlockPos;)C", (api, mv) -> new MethodVisitor(api, mv) {

            private boolean foundFix = false;

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL && "getPatternMarker".equals(name) && !foundFix) {
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/StructurePatternHook";
                    name = "getPatternMarker0";
                    desc = "(Lmods/railcraft/common/blocks/structures/StructurePattern;III)C";
                    itf = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                // Try detecting the presence of ACGaming's fix. If found we will just skip patching.
                if (opcode == Opcodes.BIPUSH && operand == (int)'O') {
                    this.foundFix = true;
                }
                super.visitIntInsn(opcode, operand);
            }
        });
    }

    private static byte[] tryFixAnvilScreen(byte[] basicClass) {
        final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                "net/minecraft/client/gui/inventory/GuiContainer", "func_73863_a", "(IIF)V");
        return patch(basicClass, targetMethodName, (api, mv) -> new MethodVisitor(api, mv) {
            final String tooltipMethod = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    "net/minecraft/client/gui/inventory/GuiContainer", "func_191948_b", "(II)V"
            );

            private boolean foundFix = false;

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                // Try detecting the presence of ACGaming's fix. If found we will just skip patching.
                if (name.equals(tooltipMethod)) {
                    this.foundFix = true;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                // 2896 is GL11.GL_LIGHTING. We need extra call there.
                if (opcode == Opcodes.SIPUSH && operand == 2896 && !this.foundFix) {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitIntInsn(Opcodes.ILOAD, 1);
                    super.visitIntInsn(Opcodes.ILOAD, 2);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/gui/inventory/GuiContainer", tooltipMethod, "(II)V", false);
                }
                super.visitIntInsn(opcode, operand);
            }
        });
    }

    private byte[] tryMakingActivatorTrackGUIBetter(byte[] basicClass) {
        final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                "net/minecraft/client/gui/inventory/GuiContainer", "func_146979_b", "(II)V");
        return patch(basicClass, targetMethodName, (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                // Shift the cart filter label up, so that it is not outside the GUI
                if (opcode == Opcodes.INVOKESTATIC && "drawStringCenteredAtPos".equals(name)) {
                    super.visitInsn(Opcodes.POP);
                    super.visitInsn(Opcodes.POP);
                    super.visitIntInsn(Opcodes.BIPUSH, 44);
                    super.visitIntInsn(Opcodes.BIPUSH, 28);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryDisableInvTitle(byte[] basicClass) {
        return patch(basicClass, "<init>", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.RETURN) {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitInsn(Opcodes.ICONST_0);
                    super.visitFieldInsn(Opcodes.PUTFIELD, "mods/railcraft/client/gui/GuiManipulatorCartRF", "drawInvTitle", "Z");
                }
                super.visitInsn(opcode);
            }
        });
    }

    private byte[] tryPatchRailcraftContainer(byte[] basicClass) {
        return patch(basicClass, "addPlayerSlots", "(Lnet/minecraft/entity/player/InventoryPlayer;I)V", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitCode() {
                Label nullCheckPass = new Label();
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitJumpInsn(Opcodes.IFNONNULL, nullCheckPass);
                super.visitInsn(Opcodes.RETURN);
                super.visitFrame(Opcodes.F_APPEND, 0, new Object[0], 1, new Object[]{ "Lnet/minecraft/entity/player/InventoryPlayer;" });
                super.visitLabel(nullCheckPass);
                super.visitCode();
            }
        });
    }

    private byte[] tryFixGuiRouting(byte[] basicClass) {
        return patch(basicClass, "<init>", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKESPECIAL && "mods/railcraft/client/gui/GuiTitled".equals(owner)) {
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/gui/containers/ContainerTrackRouting",
                            "kit", "Lmods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRailcraft;");
                    super.visitTypeInsn(Opcodes.CHECKCAST, "mods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRouting");
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                            "mods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRouting",
                            "getTrackKit",
                            "()Lmods/railcraft/api/tracks/TrackKit;",
                            false);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "mods/railcraft/common/plugins/forge/LocalizationPlugin",
                            "localize",
                            "(Lmods/railcraft/api/tracks/TrackKit;)Lnet/minecraft/util/text/ITextComponent;",
                            false);
                    desc = "(Lnet/minecraft/world/IWorldNameable;Lmods/railcraft/common/gui/containers/RailcraftContainer;Ljava/lang/String;Lnet/minecraft/util/text/ITextComponent;)V";
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryUseI18nForTrackGui(byte[] basicClass) {
        return patch(basicClass, "<init>", (api, mv) -> new MethodVisitor(api, mv) {
            final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    "net/minecraft/world/IWorldNameable", "func_70005_c_", "()Ljava/lang/String;");
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL && targetMethodName.equals(name)) {
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/I18nHook";
                    name = "translateOutfittedTrackName";
                    desc = "(Lmods/railcraft/common/blocks/tracks/outfitted/TileTrackOutfitted;)Ljava/lang/String;";
                    itf = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private byte[] tryExpandStackSizeLimitInWorldSpike(byte[] basicClass) {
        return patch(basicClass, "<init>", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL && "setInventoryStackLimit".equals(name)) {
                    // Redirect the setInventoryStackLimit(16) call to our impl, which in turn voids the effect.
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/hooks/WorldSpikeHook";
                    name = "setInvStackLimit0";
                    desc = "(Lmods/railcraft/common/util/inventory/InventoryAdvanced;I)Lmods/railcraft/common/util/inventory/InventoryAdvanced;";
                    itf = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private static byte[] tryPatchingTileRailcraft(byte[] basicClass) {
        return patch(basicClass, "markBlockForUpdate", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitCode() {
                // Skip the entire method body by an early return
                // The original method calls World::notifyBlockUpdate with flag 8, which forces Minecraft
                // to rebuild render chunk (a 16 * 16 * 16 box) on main client thread.
                // By not calling notifyBlockUpdate, it can avoid unnecessary render chunk re-building.
                // Changing flag 8 to 0 will make the render chunk rebuilding happen on a separate thread,
                // but the problem is that the re-rendering still happens.
                // In fact, this method is called without a block state change. So rebuilding the render
                // chunk does not lead to a visual difference. Thus, it is better to just avoid the update
                // altogether.
                // TODO this method may be called on server; we need to access its impact and restore correct behavior if there is desync.
                super.visitInsn(Opcodes.RETURN);
                super.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            }
        });
    }

    private static byte[] tryFixRollingRecipeDisplayInJEI(byte[] basicClass) {
        return patch(basicClass, "setRecipe", (api, mv) -> new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEINTERFACE && "setInputs".equals(name)) {
                    super.visitVarInsn(Opcodes.ALOAD, 2);
                    opcode = Opcodes.INVOKESTATIC;
                    owner = "info/tritusk/modpack/railcraft/patcher/JEIHook";
                    name = "setInputs0";
                    desc = "(Lmezz/jei/api/gui/ICraftingGridHelper;Lmezz/jei/api/gui/IGuiItemStackGroup;Ljava/util/List;Lmezz/jei/api/recipe/IRecipeWrapper;)V";
                    itf = false;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }

    private static byte[] patch(byte[] basicClass, String methodToPatch, BiFunction<Integer, MethodVisitor, MethodVisitor> patcher) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals(methodToPatch)) {
                    mv = patcher.apply(this.api, mv);
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] patch(byte[] basicClass, String methodToPatch, String targetMethodDesc, BiFunction<Integer, MethodVisitor, MethodVisitor> patcher) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals(methodToPatch) && desc.equals(targetMethodDesc)) {
                    mv = patcher.apply(this.api, mv);
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

}
