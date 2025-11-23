package info.tritusk.modpack.railcraft.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TileHobbyistEnginePatcher extends ClassVisitor {
    public TileHobbyistEnginePatcher(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("burn".equals(name)) {
            mv = new MethodVisitor(this.api, mv) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    if ("tankManager".equals(name)) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "getTankManager", "()Lmods/railcraft/common/fluids/TankManager;", false);
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                }
            };
        } else if ("<init>".equals(name)) {
            mv = new MethodVisitor(this.api, mv) {
                @Override
                public void visitInsn(int opcode) {
                    if (opcode == Opcodes.RETURN) {
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/hooks/Hooks", "hobbyistEngineInitCallback", "(Lmods/railcraft/common/blocks/single/TileEngineSteamHobby;)V", false);
                    }
                    super.visitInsn(opcode);
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
        tankSteamGetter.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/single/TileEngineSteamHobby", "boiler", "Lmods/railcraft/common/blocks/logic/BoilerLogic;");
        tankSteamGetter.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/logic/BoilerLogic", "tankSteam", "Lmods/railcraft/common/fluids/tanks/StandardTank;");
        // We know that BoilerLogic creates FilteredTank instances despite that storing them in StandardTank field.
        // Remember to check back again if Railcraft ever receives updates.
        tankSteamGetter.visitTypeInsn(Opcodes.CHECKCAST, "mods/railcraft/common/fluids/tanks/FilteredTank");
        tankSteamGetter.visitInsn(Opcodes.ARETURN);
        tankSteamGetter.visitMaxs(1, 1);
        tankSteamGetter.visitEnd();

        MethodVisitor tankManagerGetter = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getTankManager", "()Lmods/railcraft/common/fluids/TankManager;", null, null);
        tankManagerGetter.visitCode();
        tankManagerGetter.visitVarInsn(Opcodes.ALOAD, 0);
        tankManagerGetter.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/single/TileEngineSteamHobby", "boiler", "Lmods/railcraft/common/blocks/logic/BoilerLogic;");
        tankManagerGetter.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/hooks/Hooks", "getTankManagerFromBoilerLogic", "(Lmods/railcraft/common/blocks/logic/BoilerLogic;)Lmods/railcraft/common/fluids/TankManager;", false);
        tankManagerGetter.visitInsn(Opcodes.ARETURN);
        tankManagerGetter.visitMaxs(1, 1);
        tankManagerGetter.visitEnd();

        super.visitEnd();
    }
}
