package info.tritusk.modpack.railcraft.patcher;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ListIterator;

public class Xformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if ("mods.railcraft.common.plugins.jei.rolling.RollingMachineRecipeCategory".equals(transformedName)) {
            return tryFixRollingRecipeDisplayInJEI(basicClass);
        }
        return basicClass;
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
