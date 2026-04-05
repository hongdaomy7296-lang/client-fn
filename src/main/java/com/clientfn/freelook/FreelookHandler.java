package com.clientfn.freelook;

import com.clientfn.core.MinecraftCompat;
import com.clientfn.ui.ClientConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Hold-to-freelook runtime state.
 */
public final class FreelookHandler {
    private static final float TURN_SCALE = 0.15F;

    private static volatile boolean active;
    private static volatile float yawOffset;
    private static volatile float pitchOffset;

    private static volatile Field currentScreenField;
    private static volatile Field playerField;
    private static volatile Class<?> pitchFieldOwner;
    private static volatile Field pitchField;

    private FreelookHandler() {
    }

    public static void onClientTick(Minecraft mc) {
        if (mc == null) {
            deactivate();
            return;
        }

        int keyCode = (int) ClientConfig.INSTANCE.getFloat("freelook.key", Keyboard.KEY_V);
        boolean keyHeld = keyCode > 0 && Keyboard.isKeyDown(keyCode);
        boolean guiOpen = resolveCurrentScreen(mc) != null;
        boolean shouldBeActive = keyHeld && !guiOpen;

        if (shouldBeActive) {
            if (!active) {
                active = true;
                yawOffset = 0.0F;
                pitchOffset = 0.0F;
            }
            return;
        }

        if (active) {
            deactivate();
        }
    }

    public static boolean consumeMouseTurn(Object entity, float yawInput, float pitchInput) {
        if (!active || !isLocalPlayer(entity)) {
            return false;
        }

        yawOffset = wrapDegrees(yawOffset + yawInput * TURN_SCALE);

        float basePitch = readPitch(entity);
        float minOffset = -90.0F - basePitch;
        float maxOffset = 90.0F - basePitch;
        pitchOffset = clamp(pitchOffset - pitchInput * TURN_SCALE, minOffset, maxOffset);
        return true;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYawOffset() {
        return active ? yawOffset : 0.0F;
    }

    public static float getPitchOffset() {
        return active ? pitchOffset : 0.0F;
    }

    public static void syncFromConfig() {
        deactivate();
    }

    private static void deactivate() {
        active = false;
        yawOffset = 0.0F;
        pitchOffset = 0.0F;
    }

    private static boolean isLocalPlayer(Object entity) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc == null || entity == null) {
            return false;
        }

        Object localPlayer = MinecraftCompat.getLocalPlayer(mc);
        if (localPlayer == null) {
            localPlayer = resolveLocalPlayer(mc);
        }
        return localPlayer != null && localPlayer == entity;
    }

    private static Object resolveCurrentScreen(Minecraft mc) {
        try {
            Field field = currentScreenField;
            if (field == null) {
                field = resolveField(mc.getClass(), "currentScreen", "field_71462_r");
                currentScreenField = field;
            }
            return field == null ? null : field.get(mc);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object resolveLocalPlayer(Minecraft mc) {
        try {
            Field field = playerField;
            if (field == null) {
                field = resolveField(mc.getClass(), "thePlayer", "field_71439_g");
                playerField = field;
            }
            return field == null ? null : field.get(mc);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float readPitch(Object entity) {
        try {
            Class<?> owner = entity.getClass();
            Field field = pitchField;
            if (field == null || pitchFieldOwner != owner) {
                field = resolveField(owner, "rotationPitch", "field_70125_A");
                pitchField = field;
                pitchFieldOwner = owner;
            }
            if (field == null) {
                return 0.0F;
            }
            Object value = field.get(entity);
            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (Exception ignored) {
            return 0.0F;
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

    private static float wrapDegrees(float value) {
        while (value <= -180.0F) {
            value += 360.0F;
        }
        while (value > 180.0F) {
            value -= 360.0F;
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
