package info.tritusk.modpack.railcraft.patcher;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BlockTradeStationPatcher extends ClassVisitor {

    private static final String ITEM_DROP_META_HANDLER = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName("net/minecraft/block/Block", "func_180651_a", "(Lnet/minecraft/block/state/IBlockState;)I");

    public BlockTradeStationPatcher(int asm, ClassVisitor visitor) {
        super(asm, visitor);
    }

    @Override
    public void visitEnd() {
        // int damageDropped(IBlockState)
        // We want to always return 0 for Trade Station block, so that the model lookup won't get lost.
        MethodVisitor damageDropped = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, ITEM_DROP_META_HANDLER, "(Lnet/minecraft/block/state/IBlockState;)I", null, null);
        damageDropped.visitInsn(Opcodes.ICONST_0);
        damageDropped.visitInsn(Opcodes.IRETURN);
        damageDropped.visitMaxs(1, 2);
        damageDropped.visitEnd();
        super.visitEnd();
    }
}
