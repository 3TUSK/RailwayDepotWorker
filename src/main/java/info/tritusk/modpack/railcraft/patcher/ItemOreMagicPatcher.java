package info.tritusk.modpack.railcraft.patcher;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ItemOreMagicPatcher extends ClassVisitor {

    public ItemOreMagicPatcher(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visitEnd() {
        final String methodDesc = "(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;";
        String translationKeyGetterName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName("net/minecraft/item/Item", "func_77667_c", methodDesc);

        MethodVisitor translationKeyGetter = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, translationKeyGetterName, methodDesc, null, null);
        translationKeyGetter.visitCode();
        translationKeyGetter.visitVarInsn(Opcodes.ALOAD, 0);
        translationKeyGetter.visitVarInsn(Opcodes.ALOAD, 1);
        translationKeyGetter.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/I18nHook", "translateMagicOreName", "(Lmods/railcraft/common/blocks/ore/ItemOreMagic;Lnet/minecraft/item/ItemStack;)Ljava/lang/String;", false);
        translationKeyGetter.visitInsn(Opcodes.ARETURN);
        translationKeyGetter.visitMaxs(2, 2);
        translationKeyGetter.visitEnd();

        super.visitEnd();
    }
}
