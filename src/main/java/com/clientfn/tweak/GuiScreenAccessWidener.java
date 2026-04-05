package com.clientfn.tweak;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Widens GuiScreen.field_146297_k (mc) from protected to public so that
 * GuiConnecting$1 can access it via the raw getfield bytecode present in
 * the shipped jar.  The vanilla jar is missing the synthetic accessor that
 * javac would normally emit, which causes IllegalAccessError at runtime.
 */
public class GuiScreenAccessWidener implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.client.gui.GuiScreen";
    private static final String FIELD_NAME = "field_146297_k";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            boolean modified = false;
            for (FieldNode field : node.fields) {
                if (FIELD_NAME.equals(field.name)) {
                    if ((field.access & Opcodes.ACC_PUBLIC) == 0) {
                        field.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
                        field.access |= Opcodes.ACC_PUBLIC;
                        modified = true;
                    }
                    break;
                }
            }

            if (!modified) {
                return basicClass;
            }

            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable ignored) {
            return basicClass;
        }
    }
}
