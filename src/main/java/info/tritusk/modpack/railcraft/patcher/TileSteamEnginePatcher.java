package info.tritusk.modpack.railcraft.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TileSteamEnginePatcher extends ClassVisitor {

    public TileSteamEnginePatcher(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("burn".equals(name)) {
            mv = new MethodVisitor(this.api, mv) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    if ("tankSteam".equals(name)) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "getSteamTank", "()Lmods/railcraft/common/fluids/tanks/FilteredTank;", false);
                    } else if ("tankManager".equals(name)) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "getTankManager", "()Lmods/railcraft/common/fluids/TankManager;", false);
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                }
            };
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        MethodVisitor tankSteamGetter = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getSteamTank", "()Lmods/railcraft/common/fluids/tanks/FilteredTank;", null, null);
        tankSteamGetter.visitCode();
        tankSteamGetter.visitVarInsn(Opcodes.ALOAD, 0);
        tankSteamGetter.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/single/TileEngineSteam", "tankSteam", "Lmods/railcraft/common/fluids/tanks/FilteredTank;");
        tankSteamGetter.visitInsn(Opcodes.ARETURN);
        tankSteamGetter.visitMaxs(1, 1);
        tankSteamGetter.visitEnd();

        super.visitEnd();
    }
}
