package info.tritusk.modpack.railcraft.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SteamLocomotivePatcher extends ClassVisitor {
    public SteamLocomotivePatcher(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("<init>".equals(name)) {
            // There are multiple <init> methods in EntityLocomotiveSteam, and we want to patch all of them.
            // Note that we hardcode the local variable position; they may change if another mod modifies this spot.
            switch (desc) {
                case "(Lnet/minecraft/world/World;)V": return new AddInvLogic(this.api, mv, 2);
                case "(Lnet/minecraft/world/World;DDD)V": return new AddInvLogic(this.api, mv, 8);
            }
        }
        return mv;
    }

    public static final class AddInvLogic extends MethodVisitor {
        private final int localVarPosition;

        public AddInvLogic(int api, MethodVisitor mv, int localVarPosition) {
            super(api, mv);
            this.localVarPosition = localVarPosition;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, this.localVarPosition);
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/carts/EntityLocomotiveSteam", "invWaterContainers", "Lmods/railcraft/common/util/inventory/wrappers/InventoryMapper;");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/hooks/Hooks", "initSteamLocomotive",
                        "(Lmods/railcraft/common/carts/EntityLocomotiveSteam;Lmods/railcraft/common/blocks/logic/Logic$Adapter;Lmods/railcraft/common/util/inventory/wrappers/InventoryMapper;)V", false);
            }
            super.visitInsn(opcode);
        }
    }
}
