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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;
import java.util.ListIterator;

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
            // TODO ACGaming's Railcraft fork disabled RF Loader/Unloader GUI; we might want to restore it.
            case "mods.railcraft.common.gui.containers.RailcraftContainer": return tryPatchRailcraftContainer(basicClass);
            case "mods.railcraft.client.gui.GuiTrackDelayedLocking":
            case "mods.railcraft.client.gui.GuiTrackEmbarking":
            case "mods.railcraft.client.gui.GuiTrackLauncher":
            case "mods.railcraft.client.gui.GuiTrackPriming": return tryUseI18nForTrackGui(basicClass);
            // TODO Activator get another issue to fix
            //case "mods.railcraft.client.gui.GuiTrackActivator":
            case "mods.railcraft.client.gui.GuiTrackRouting": return tryFixGuiRouting(basicClass);
            case "mods.railcraft.client.gui.GuiManipulatorCartRF": return tryDisableInvTitle(basicClass);
            default: return basicClass;
        }
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

        // Attempt to locate the INVOKESPECIAL.
        InsnList instructions = constructor.instructions;
        AbstractInsnNode target = instructions.getFirst();
        do {
            if (target.getOpcode() == Opcodes.INVOKESPECIAL) {
                break;
            }
        } while ((target = target.getNext()) != null);
        if (target == null) {
            return basicClass;
        }

        InsnList staticCallForTranslatedName = new InsnList();
        staticCallForTranslatedName.add(new VarInsnNode(Opcodes.ALOAD, 1));
        staticCallForTranslatedName.add(new FieldInsnNode(Opcodes.GETFIELD,
                "mods/railcraft/common/gui/containers/ContainerTrackRouting",
                "kit", "Lmods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRailcraft;"));
        staticCallForTranslatedName.add(new TypeInsnNode(Opcodes.CHECKCAST,
                "mods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRouting"));
        staticCallForTranslatedName.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "mods/railcraft/common/blocks/tracks/outfitted/kits/TrackKitRouting",
                "getTrackKit",
                "()Lmods/railcraft/api/tracks/TrackKit;",
                false));
        staticCallForTranslatedName.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "mods/railcraft/common/plugins/forge/LocalizationPlugin",
                "localize",
                "(Lmods/railcraft/api/tracks/TrackKit;)Lnet/minecraft/util/text/ITextComponent;",
                false));
        instructions.insertBefore(target, staticCallForTranslatedName);
        MethodInsnNode newConstructorCall = new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "mods/railcraft/client/gui/GuiTitled",
                "<init>",
                "(Lnet/minecraft/world/IWorldNameable;Lmods/railcraft/common/gui/containers/RailcraftContainer;Ljava/lang/String;Lnet/minecraft/util/text/ITextComponent;)V",
                false);
        instructions.set(target, newConstructorCall);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
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
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode setRecipeMethod = null;
        for (MethodNode m : node.methods) {
            if ("setRecipe".equals(m.name)) {
                setRecipeMethod = m;
            }
        }

        if (setRecipeMethod == null) {
            // System.out.println("Did not find method, skipping");
            return basicClass;
        }

        InsnList instructions = setRecipeMethod.instructions;
        AbstractInsnNode target = null;
        for (ListIterator<AbstractInsnNode> itr = instructions.iterator(); itr.hasNext();) {
            AbstractInsnNode instruction = itr.next();
            if (instruction.getOpcode() != Opcodes.INVOKEINTERFACE) {
                continue;
            }
            MethodInsnNode maybeTarget = (MethodInsnNode) instruction;
            if (maybeTarget.name.equals("setInputs")) {
                target = instruction;
                break;
            }
        }

        if (target == null) {
            // System.out.println("Did not find target, skipping");
            return basicClass;
        }

        instructions.insertBefore(target, new VarInsnNode(Opcodes.ALOAD, 2)); // Param IRecipeWrapper recipeWrapper

        instructions.set(target, new MethodInsnNode(Opcodes.INVOKESTATIC,
                "info/tritusk/modpack/railcraft/patcher/JEIHook",
                "setInputs0",
                "(Lmezz/jei/api/gui/ICraftingGridHelper;Lmezz/jei/api/gui/IGuiItemStackGroup;Ljava/util/List;Lmezz/jei/api/recipe/IRecipeWrapper;)V",
                false));


        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}
