package com.clientfn.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Bridges GuiScreen MCP/SRG fields in runtime environments without reobf.
 */
public final class GuiScreenCompat {
    private static volatile Field buttonListField;
    private static volatile Field widthField;
    private static volatile Field heightField;
    private static volatile Field minecraftField;

    private GuiScreenCompat() {
    }

    public static List<?> getButtonList(GuiScreen screen) {
        if (screen == null) {
            return null;
        }
        try {
            Field field = buttonListField;
            if (field == null) {
                field = resolveField(screen.getClass(), "buttonList", "field_146292_n");
                buttonListField = field;
            }
            Object value = field == null ? null : field.get(screen);
            return value instanceof List<?> ? (List<?>) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static int getWidth(GuiScreen screen) {
        return readIntField(screen, true);
    }

    public static int getHeight(GuiScreen screen) {
        return readIntField(screen, false);
    }

    public static Minecraft getMinecraft(GuiScreen screen) {
        if (screen == null) {
            return null;
        }
        try {
            Field field = minecraftField;
            if (field == null) {
                field = resolveField(screen.getClass(), "mc", "field_146297_k");
                minecraftField = field;
            }
            Object value = field == null ? null : field.get(screen);
            return value instanceof Minecraft ? (Minecraft) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int readIntField(GuiScreen screen, boolean width) {
        if (screen == null) {
            return 0;
        }
        try {
            Field field = width ? widthField : heightField;
            if (field == null) {
                field = width
                        ? resolveField(screen.getClass(), "width", "field_146294_l")
                        : resolveField(screen.getClass(), "height", "field_146295_m");
                if (width) {
                    widthField = field;
                } else {
                    heightField = field;
                }
            }
            Object value = field == null ? null : field.get(screen);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
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
