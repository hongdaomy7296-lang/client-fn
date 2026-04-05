package com.clientfn.core;

import net.minecraft.client.gui.Gui;

import java.lang.reflect.Method;

/**
 * Bridges Gui#drawRect across MCP/SRG runtime names.
 */
public final class GuiDrawCompat {
    private static volatile Method drawRectMethod;

    private GuiDrawCompat() {
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        try {
            Method method = drawRectMethod;
            if (method == null) {
                method = resolveMethod(Gui.class, "func_73734_a", "drawRect");
                drawRectMethod = method;
            }
            if (method != null) {
                method.invoke(null, left, top, right, bottom, color);
            }
        } catch (Throwable ignored) {
            // Best-effort draw bridge.
        }
    }

    private static Method resolveMethod(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Method method = owner.getDeclaredMethod(name, int.class, int.class, int.class, int.class, int.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Try next alias.
            }
        }
        return null;
    }
}
