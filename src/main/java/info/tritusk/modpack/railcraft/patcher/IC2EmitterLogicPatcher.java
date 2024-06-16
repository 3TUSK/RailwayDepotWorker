package info.tritusk.modpack.railcraft.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class IC2EmitterLogicPatcher extends ClassVisitor {
    public IC2EmitterLogicPatcher(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        switch (name) {
            case "onStructureChanged": return new StructureChangedPatcher(Opcodes.ASM5, mv);
            case "addToNet": return new AddToENetPatcher(Opcodes.ASM5, mv);
            case "dropFromNet": return new RemoveFromENetPatcher(Opcodes.ASM5, mv);
            default: return mv;
        }
    }

    static final class StructureChangedPatcher extends MethodVisitor {

        private int firstArgRefCount = 0;

        public StructureChangedPatcher(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            /*
             * bootstrap method (bsm for short) takes 6 args.
             * 1st arg is MethodHandles.Lookup which will be taken care automatically.
             * 2nd arg is the `name` here. It is the name of sole method in target functional interface.
             * 3rd arg is the `desc` here. It should contain all captured variables.
             * 4th arg is bsmArgs[0]. It is the samMethodType.
             * 5th arg is bsmArgs[1]. It is the method handle of the actual implementation.
             * 6th arg is bsmArgs[2]. It is the concrete type of samMethodType. It may be the same of samMethodType.
             * Refer to LambdaMetaFactory for a more accurate description.
             */
            desc = "(Lmods/railcraft/common/blocks/logic/IC2EmitterLogic;ZZ)Ljava/util/function/Consumer;";
            bsmArgs[1] = new Handle(Opcodes.H_INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/IC2Hook", "eNetCallback", "(Lmods/railcraft/common/blocks/logic/IC2EmitterLogic;ZZLnet/minecraft/world/World;)V", false);
            // In ACGaming's fork, we already have this ILOAD_1 ready; having extra one will cause stack map mismatch.
            if (this.firstArgRefCount == 1) {
                super.visitVarInsn(Opcodes.ILOAD, 1);
            }
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (opcode == Opcodes.ILOAD && var == 1) {
                this.firstArgRefCount++;
            }
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // We pushed one more ILOAD_1, so we also need to expand stack limit
            super.visitMaxs(maxStack + 1, maxLocals + 1);
        }
    }

    static final class AddToENetPatcher extends MethodVisitor {

        private final MethodVisitor realMv;

        public AddToENetPatcher(int api, MethodVisitor mv) {
            super(api, null);
            this.realMv = mv;
        }

        @Override
        public void visitParameter(String name, int access) {
            this.realMv.visitParameter(name, access);
        }

        @Override
        public void visitCode() {
            // Ignore everything, we are going to hand-write instructions! Hell yeah!
            this.realMv.visitCode();
            this.realMv.visitVarInsn(Opcodes.ALOAD, 0);
            this.realMv.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/logic/IC2EmitterLogic", "added", "Z");
            Label jumpTarget = new Label();
            this.realMv.visitJumpInsn(Opcodes.IFEQ, jumpTarget);
            this.realMv.visitInsn(Opcodes.RETURN);
            this.realMv.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            this.realMv.visitLabel(jumpTarget);
            this.realMv.visitVarInsn(Opcodes.ALOAD, 0);
            this.realMv.visitInsn(Opcodes.DUP);
            this.realMv.visitInsn(Opcodes.ICONST_1);
            this.realMv.visitFieldInsn(Opcodes.PUTFIELD, "mods/railcraft/common/blocks/logic/IC2EmitterLogic", "added", "Z");
            this.realMv.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/IC2Hook", "addToENet0", "(Lmods/railcraft/common/blocks/logic/IC2EmitterLogic;)V", false);
            this.realMv.visitInsn(Opcodes.RETURN);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            this.realMv.visitMaxs(3, 3);
        }

        @Override
        public void visitEnd() {
            this.realMv.visitEnd();
        }
    }

    static final class RemoveFromENetPatcher extends MethodVisitor {

        private final MethodVisitor realMv;

        public RemoveFromENetPatcher(int api, MethodVisitor mv) {
            super(api, null);
            this.realMv = mv;
        }

        @Override
        public void visitCode() {
            // Ignore everything, we are going to hand-write instructions! Hell yeah!
            this.realMv.visitCode();
            this.realMv.visitVarInsn(Opcodes.ALOAD, 0);
            this.realMv.visitFieldInsn(Opcodes.GETFIELD, "mods/railcraft/common/blocks/logic/IC2EmitterLogic", "added", "Z");
            Label jumpTarget = new Label();
            this.realMv.visitJumpInsn(Opcodes.IFNE, jumpTarget);
            this.realMv.visitInsn(Opcodes.RETURN);
            this.realMv.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            this.realMv.visitLabel(jumpTarget);
            this.realMv.visitVarInsn(Opcodes.ALOAD, 0);
            this.realMv.visitInsn(Opcodes.DUP);
            this.realMv.visitInsn(Opcodes.ICONST_0);
            this.realMv.visitFieldInsn(Opcodes.PUTFIELD, "mods/railcraft/common/blocks/logic/IC2EmitterLogic", "added", "Z");
            this.realMv.visitMethodInsn(Opcodes.INVOKESTATIC, "info/tritusk/modpack/railcraft/patcher/IC2Hook", "removeFromENet0", "(Lmods/railcraft/common/blocks/logic/IC2EmitterLogic;)V", false);
            this.realMv.visitInsn(Opcodes.RETURN);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            this.realMv.visitMaxs(3, 3);
        }

        @Override
        public void visitEnd() {
            this.realMv.visitEnd();
        }
    }
}
