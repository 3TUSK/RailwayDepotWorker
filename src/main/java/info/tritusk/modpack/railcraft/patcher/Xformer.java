package info.tritusk.modpack.railcraft.patcher;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;

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
            case "mods.railcraft.common.gui.containers.RailcraftContainer": return tryPatchRailcraftContainer(basicClass);
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

    private byte[] tryFixHopperCartDupe(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("transferAndNeedsCooldown".equals(name)) {
                    mv = new MethodVisitor(this.api, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryReplaceFirestoneTicker(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("preInit".equals(name)) {
                    mv = new MethodVisitor(this.api, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryFixCartInvDuplication(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("onMinecartSpawn".equals(name)) {
                    mv = new MethodVisitor(this.api, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] tryReenableRFManipulatorGUI(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("openGui".equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {

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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] tryFixIC2EmitterLogic(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new IC2EmitterLogicPatcher(Opcodes.ASM5, writer), 0);
        return writer.toByteArray();
    }

    private static byte[] tryFixStructurePatternCheck(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("getPatternMarker".equals(name) && "(Lnet/minecraft/util/math/BlockPos;)C".equals(desc)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {

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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static byte[] tryFixAnvilScreen(byte[] basicClass) {

        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    "net/minecraft/client/gui/inventory/GuiContainer", "func_73863_a", "(IIF)V");
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (targetMethodName.equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryMakingActivatorTrackGUIBetter(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    "net/minecraft/client/gui/inventory/GuiContainer", "func_146979_b", "(II)V");
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (targetMethodName.equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryDisableInvTitle(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode targetMethod = null;
        for (MethodNode m : node.methods) {
            if ("<init>".equals(m.name)) {
                targetMethod = m;
                break;
            }
        }

        if (targetMethod == null) { // Not sure how is that even possible, but yeah, fool-proof.
            return basicClass;
        }

        InsnList instructions = targetMethod.instructions;
        AbstractInsnNode endOfMethod = instructions.getLast();
        while (endOfMethod != null && endOfMethod.getOpcode() != Opcodes.RETURN) {
            endOfMethod = endOfMethod.getPrevious();
        }
        if (endOfMethod == null) {
            return basicClass;
        }

        InsnList injectAtTail = new InsnList();
        injectAtTail.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injectAtTail.add(new InsnNode(Opcodes.ICONST_0));
        injectAtTail.add(new FieldInsnNode(Opcodes.PUTFIELD, "mods/railcraft/client/gui/GuiManipulatorCartRF", "drawInvTitle", "Z"));
        targetMethod.instructions.insertBefore(endOfMethod, injectAtTail);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] tryPatchRailcraftContainer(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode targetMethod = null;
        for (MethodNode m : node.methods) {
            if ("addPlayerSlots".equals(m.name) && "(Lnet/minecraft/entity/player/InventoryPlayer;I)V".equals(m.desc)) {
                targetMethod = m;
                break;
            }
        }

        if (targetMethod == null) {
            return basicClass;
        }

        InsnList injectAtHead = new InsnList();
        LabelNode nullCheckPass = new LabelNode(new Label());
        injectAtHead.add(new VarInsnNode(Opcodes.ALOAD, 1));
        injectAtHead.add(new JumpInsnNode(Opcodes.IFNONNULL, nullCheckPass));
        injectAtHead.add(new InsnNode(Opcodes.RETURN));
        injectAtHead.add(new FrameNode(Opcodes.F_APPEND, 0, new Object[0], 1, new Object[] { "Lnet/minecraft/entity/player/InventoryPlayer;" }));
        injectAtHead.add(nullCheckPass);
        targetMethod.instructions.insert(injectAtHead);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private byte[] tryFixGuiRouting(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("<init>".equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryUseI18nForTrackGui(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            final String targetMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    "net/minecraft/world/IWorldNameable", "func_70005_c_", "()Ljava/lang/String;");
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("<init>".equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private byte[] tryExpandStackSizeLimitInWorldSpike(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode constructor = null;
        for (MethodNode m : node.methods) {
            if ("<init>".equals(m.name)) {
                constructor = m;
                break;
            }
        }

        if (constructor == null) { // Not sure how is that even possible, but yeah, fool-proof.
            return basicClass;
        }

        InsnList instructions = constructor.instructions;
        AbstractInsnNode target = instructions.getLast();
        do {
            if (target.getOpcode() == Opcodes.ALOAD) {
                break;
            }
        } while ((target = target.getPrevious()) != null);

        if (target == null) {
            return basicClass;
        }

        // Return early before the getInventory().setInventoryStackLimit(16) call chain,
        // effectively nullify the call.
        instructions.insertBefore(target, new InsnNode(Opcodes.RETURN));

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] tryPatchingTileRailcraft(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode markBlockForUpdateMethod = null;
        for (MethodNode m : node.methods) {
            if ("markBlockForUpdate".equals(m.name)) {
                markBlockForUpdateMethod = m;
                break;
            }
        }

        if (markBlockForUpdateMethod == null) {
            // System.out.println("Did not find method, skipping");
            return basicClass;
        }

        // Remove the entire method body, replacing with a stub.
        // The original method calls World::notifyBlockUpdate with flag 8, which forces Minecraft
        // to rebuild render chunk (a 16 * 16 * 16 box) on main client thread.
        // By not calling notifyBlockUpdate, it can avoid unnecessary render chunk re-building.
        // Changing flag 8 to 0 will make the render chunk rebuilding happen on a separate thread,
        // but the problem is that the re-rendering still happens.
        // In fact, this method is called without a block state change. So rebuilding the render
        // chunk does not lead to a visual difference. Thus, it is better to just avoid the update
        // altogether.
        // TODO this method may be called on server; we need to access its impact and restore correct behavior if there is desync.
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.RETURN));
        markBlockForUpdateMethod.instructions = instructions;
        markBlockForUpdateMethod.localVariables = Collections.emptyList();
        markBlockForUpdateMethod.visitMaxs(0, 1);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] tryFixRollingRecipeDisplayInJEI(byte[] basicClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(basicClass).accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("setRecipe".equals(name)) {
                    mv = new MethodVisitor(Opcodes.ASM5, mv) {
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
                    };
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

}
