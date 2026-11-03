package org.atcgroup.netcompat.coremod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

public final class MixinClassTransformer {

    private static final String CALLBACK_OWNER =
            "org/atcgroup/netcompat/NCClient";
    private static final String CALLBACK_NAME =
            "allowModNetworkMixins";
    private static final String CALLBACK_DESC = "()Z";

    private static final String INJECT_DESC =
            "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String MODIFY_CONSTANT_DESC =
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";

    private static final String[] TARGET_PACKAGES = {
            "cn/ussshenzhou/notenoughbandwidth/mixin/",
            "cn/ussshenzhou/neb/mixin/"
    };

    private MixinClassTransformer() {}

    public static ClassNode transform(ClassNode node, String className) {
        if (node == null || !isTarget(node.name)) {
            return node;
        }

        int injectCount = 0;
        int constantCount = 0;
        int skipped = 0;

        for (MethodNode method : node.methods) {
            if (alreadyPatched(method)) {
                continue;
            }

            if (hasAnnotation(method, INJECT_DESC)) {
                if (Type.getReturnType(method.desc).getSort() != Type.VOID) {
                    skipped++;
                    continue;
                }

                injectGuard(method);
                injectCount++;
            } else if (hasAnnotation(method, MODIFY_CONSTANT_DESC)) {
                if (injectConstantGuard(method)) {
                    constantCount++;
                } else {
                    skipped++;
                }
            }
        }

        if (injectCount + constantCount > 0 || skipped > 0) {
            System.out.println(
                    "[MixinHandlerGate] " + node.name
                            + " | Inject=" + injectCount
                            + " | ModifyConstant=" + constantCount
                            + " | Skipped=" + skipped
            );
        }

        return node;
    }

    private static boolean isTarget(String name) {
        if (name == null) return false;

        for (String prefix : TARGET_PACKAGES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotation(
            MethodNode method,
            String descriptor
    ) {
        return containsAnnotation(method.visibleAnnotations, descriptor)
                || containsAnnotation(method.invisibleAnnotations, descriptor);
    }

    private static boolean containsAnnotation(
            List<AnnotationNode> annotations,
            String descriptor
    ) {
        if (annotations == null) return false;

        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @Inject: Gate=false -> return immediately.
     */
    private static void injectGuard(MethodNode method) {
        InsnList hook = new InsnList();
        LabelNode continueLabel = new LabelNode();

        addGateCall(hook);

        hook.add(new JumpInsnNode(Opcodes.IFNE, continueLabel));
        hook.add(new InsnNode(Opcodes.RETURN));
        hook.add(continueLabel);

        method.instructions.insert(hook);
        method.maxStack = Math.max(method.maxStack, 1);
    }

    /**
     * @ModifyConstant: Gate=false -> return original constant.
     *
     * Supported when:
     * - handler has at least one argument;
     * - first argument and return type are identical;
     * - both are primitive or reference types supported by JVM load/return opcodes.
     */
    private static boolean injectConstantGuard(MethodNode method) {
        Type[] arguments = Type.getArgumentTypes(method.desc);
        Type returnType = Type.getReturnType(method.desc);

        if (arguments.length == 0) {
            return false;
        }

        Type originalType = arguments[0];

        if (!sameType(originalType, returnType)) {
            return false;
        }

        int localIndex =
                (method.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;

        for (int i = 0; i < arguments.length; i++) {
            if (i == 0) break;
            localIndex += arguments[i].getSize();
        }

        InsnList hook = new InsnList();
        LabelNode continueLabel = new LabelNode();

        addGateCall(hook);

        hook.add(new JumpInsnNode(Opcodes.IFNE, continueLabel));

        hook.add(new VarInsnNode(
                originalType.getOpcode(Opcodes.ILOAD),
                localIndex
        ));

        hook.add(new InsnNode(
                returnType.getOpcode(Opcodes.IRETURN)
        ));

        hook.add(continueLabel);

        method.instructions.insert(hook);
        method.maxStack = Math.max(
                method.maxStack,
                Math.max(1, originalType.getSize())
        );

        return true;
    }

    private static boolean sameType(Type a, Type b) {
        return a.equals(b);
    }

    private static void addGateCall(InsnList instructions) {
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                CALLBACK_OWNER,
                CALLBACK_NAME,
                CALLBACK_DESC,
                false
        ));
    }

    private static boolean alreadyPatched(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {

            if (insn instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESTATIC
                    && CALLBACK_OWNER.equals(call.owner)
                    && CALLBACK_NAME.equals(call.name)
                    && CALLBACK_DESC.equals(call.desc)) {
                return true;
            }
        }

        return false;
    }
}
