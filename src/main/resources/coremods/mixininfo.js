var Opcodes = Java.type("org.objectweb.asm.Opcodes");
var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");

function initializeCoreMod() {
    return {
        "MixinInfoLoadMixinClassHook": {
            "target": {
                "type": "METHOD",
                "class": "org.spongepowered.asm.mixin.transformer.MixinInfo",
                "methodName": "loadMixinClass",
                "methodDesc": "(Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;"
            },
            "transformer": function(methodNode) {
                var local = methodNode.maxLocals++;
                var patched = 0;

                for (var insn = methodNode.instructions.getFirst();
                     insn !== null;) {
                    var next = insn.getNext();

                    if (insn.getOpcode() === Opcodes.ARETURN) {
                        var hook = new InsnList();

                        hook.add(new VarInsnNode(Opcodes.ASTORE, local));
                        hook.add(new VarInsnNode(Opcodes.ALOAD, local));
                        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));

                        hook.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "org/atcgroup/netcompat/coremod/MixinClassTransformer",
                            "transform",
                            "(Lorg/objectweb/asm/tree/ClassNode;Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;",
                            false
                        ));

                        methodNode.instructions.insertBefore(insn, hook);
                        patched++;
                    }

                    insn = next;
                }

                print("[MixinHandlerGate] Patched MixinInfo.loadMixinClass returns: " + patched);

                return methodNode;
            }
        }
    };
}