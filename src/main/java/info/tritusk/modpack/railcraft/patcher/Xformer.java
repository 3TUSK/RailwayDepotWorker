package info.tritusk.modpack.railcraft.patcher;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
            default: return basicClass;
        }
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
                "info/tritusk/modpack/railcraft/patcher/Hook",
                "setInputs0",
                "(Lmezz/jei/api/gui/ICraftingGridHelper;Lmezz/jei/api/gui/IGuiItemStackGroup;Ljava/util/List;Lmezz/jei/api/recipe/IRecipeWrapper;)V",
                false));


        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

}
