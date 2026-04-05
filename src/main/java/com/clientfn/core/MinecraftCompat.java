package com.clientfn.core;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bridges MCP/SRG member name differences for runtime environments without reobf.
 */
public final class MinecraftCompat {
    private static volatile Method minecraftGetter;
    private static volatile Field mcDataDirField;
    private static volatile Field localPlayerField;

    private MinecraftCompat() {
    }

    public static Minecraft getMinecraft() {
        try {
            Method method = minecraftGetter;
            if (method == null) {
                method = resolveMethod(Minecraft.class, new String[]{"getMinecraft", "func_71410_x"}, 0);
                if (method != null) {
                    method.setAccessible(true);
                    minecraftGetter = method;
                }
            }
            if (method == null) {
                return null;
            }
            Object value = method.invoke(null);
            return value instanceof Minecraft ? (Minecraft) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static File getMcDataDir(Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }
        try {
            Field field = mcDataDirField;
            if (field == null) {
                field = resolveField(minecraft.getClass(), "mcDataDir", "field_71412_D");
                mcDataDirField = field;
            }
            if (field == null) {
                return null;
            }
            Object value = field.get(minecraft);
            return value instanceof File ? (File) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object getLocalPlayer(Minecraft minecraft) {
        if (minecraft == null) {
            return null;
        }
        try {
            Field field = localPlayerField;
            if (field == null) {
                field = resolveField(minecraft.getClass(), "thePlayer", "field_71439_g");
                localPlayerField = field;
            }
            return field == null ? null : field.get(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method resolveMethod(Class<?> owner, String[] names, int arity) {
        Class<?> current = owner;
        while (current != null) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterTypes().length != arity) {
                    continue;
                }
                String methodName = method.getName();
                for (String candidate : names) {
                    if (candidate.equals(methodName)) {
                        return method;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field resolveField(Class<?> owner, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // Try next candidate.
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }
}
