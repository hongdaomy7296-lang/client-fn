package com.clientfn.core;

import com.clientfn.ui.ClientConfig;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime-applied player features that must work across MCP/SRG name variants.
 */
public final class PlayerFeatureController {
    private static final int NIGHT_VISION_ID = 16;
    private static final int NIGHT_VISION_DURATION_TICKS = 220;
    private static final int NIGHT_VISION_AMPLIFIER = 0;
    private static final long NIGHT_VISION_REFRESH_MS = 900L;

    private static volatile Field movementInputField;
    private static volatile Class<?> movementInputFieldOwner;
    private static volatile Field moveForwardField;
    private static volatile Class<?> moveForwardFieldOwner;
    private static volatile Field sneakField;
    private static volatile Class<?> sneakFieldOwner;
    private static volatile Method setSprintingMethod;
    private static volatile Class<?> setSprintingMethodOwner;
    private static volatile Method addPotionEffectMethod;
    private static volatile Class<?> addPotionEffectMethodOwner;
    private static volatile Method removePotionEffectMethod;
    private static volatile Class<?> removePotionEffectMethodOwner;
    private static volatile Constructor<?> potionEffectCtor4;
    private static volatile Constructor<?> potionEffectCtor3;

    private static volatile long lastNightVisionApplyMs;
    private static volatile boolean nightVisionAppliedByClientFn;

    private PlayerFeatureController() {
    }

    public static void onClientTick(Minecraft minecraft) {
        Object localPlayer = MinecraftCompat.getLocalPlayer(minecraft);
        if (localPlayer == null) {
            nightVisionAppliedByClientFn = false;
            return;
        }

        if (ClientConfig.INSTANCE.isForceSprintEnabled()) {
            applyForceSprint(localPlayer);
        }

        if (ClientConfig.INSTANCE.isPermanentNightVisionEnabled()) {
            applyNightVision(localPlayer);
        } else {
            clearNightVision(localPlayer);
        }
    }

    public static void syncFromConfig() {
        lastNightVisionApplyMs = 0L;
    }

    private static void applyForceSprint(Object localPlayer) {
        if (!isTryingToMoveForward(localPlayer)) {
            return;
        }
        invokeSetSprinting(localPlayer, true);
    }

    private static boolean isTryingToMoveForward(Object localPlayer) {
        try {
            Object movementInput = readMovementInput(localPlayer);
            if (movementInput == null) {
                return true;
            }

            float forward = readMoveForward(movementInput);
            boolean sneaking = readSneaking(movementInput);
            return forward > 0.01F && !sneaking;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object readMovementInput(Object localPlayer) throws IllegalAccessException {
        Class<?> owner = localPlayer.getClass();
        Field field = movementInputField;
        if (field == null || movementInputFieldOwner != owner) {
            field = resolveField(owner, "field_71158_b", "movementInput");
            movementInputField = field;
            movementInputFieldOwner = owner;
        }
        return field == null ? null : field.get(localPlayer);
    }

    private static float readMoveForward(Object movementInput) throws IllegalAccessException {
        Class<?> owner = movementInput.getClass();
        Field field = moveForwardField;
        if (field == null || moveForwardFieldOwner != owner) {
            field = resolveField(owner, "field_78900_b", "moveForward");
            moveForwardField = field;
            moveForwardFieldOwner = owner;
        }
        if (field == null) {
            return 0.0F;
        }
        Object value = field.get(movementInput);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static boolean readSneaking(Object movementInput) throws IllegalAccessException {
        Class<?> owner = movementInput.getClass();
        Field field = sneakField;
        if (field == null || sneakFieldOwner != owner) {
            field = resolveField(owner, "field_78899_d", "sneak");
            sneakField = field;
            sneakFieldOwner = owner;
        }
        if (field == null) {
            return false;
        }
        Object value = field.get(movementInput);
        return value instanceof Boolean && ((Boolean) value);
    }

    private static void invokeSetSprinting(Object localPlayer, boolean sprinting) {
        try {
            Class<?> owner = localPlayer.getClass();
            Method method = setSprintingMethod;
            if (method == null || setSprintingMethodOwner != owner) {
                method = resolveMethod(owner, new String[]{"func_70031_b", "setSprinting"}, boolean.class);
                setSprintingMethod = method;
                setSprintingMethodOwner = owner;
            }
            if (method != null) {
                method.invoke(localPlayer, sprinting);
            }
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        }
    }

    private static void applyNightVision(Object localPlayer) {
        long now = System.currentTimeMillis();
        if (now - lastNightVisionApplyMs < NIGHT_VISION_REFRESH_MS) {
            return;
        }

        try {
            Object effect = createNightVisionEffect(localPlayer.getClass().getClassLoader());
            if (effect == null) {
                return;
            }

            Class<?> owner = localPlayer.getClass();
            Method method = addPotionEffectMethod;
            if (method == null || addPotionEffectMethodOwner != owner) {
                method = resolveAddPotionEffectMethod(owner, effect.getClass());
                addPotionEffectMethod = method;
                addPotionEffectMethodOwner = owner;
            }
            if (method == null) {
                return;
            }

            method.invoke(localPlayer, effect);
            lastNightVisionApplyMs = now;
            nightVisionAppliedByClientFn = true;
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        }
    }

    private static void clearNightVision(Object localPlayer) {
        if (!nightVisionAppliedByClientFn) {
            return;
        }
        try {
            Class<?> owner = localPlayer.getClass();
            Method method = removePotionEffectMethod;
            if (method == null || removePotionEffectMethodOwner != owner) {
                method = resolveMethod(owner, new String[]{"func_82170_o", "removePotionEffect"}, int.class);
                removePotionEffectMethod = method;
                removePotionEffectMethodOwner = owner;
            }
            if (method != null) {
                method.invoke(localPlayer, NIGHT_VISION_ID);
            }
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        } finally {
            nightVisionAppliedByClientFn = false;
        }
    }

    private static Object createNightVisionEffect(ClassLoader loader) {
        try {
            Class<?> potionEffectClass = Class.forName("net.minecraft.potion.PotionEffect", false, loader);
            Constructor<?> ctor4 = potionEffectCtor4;
            if (ctor4 == null || ctor4.getDeclaringClass() != potionEffectClass) {
                try {
                    ctor4 = potionEffectClass.getDeclaredConstructor(int.class, int.class, int.class, boolean.class);
                    ctor4.setAccessible(true);
                    potionEffectCtor4 = ctor4;
                } catch (NoSuchMethodException ignored) {
                    ctor4 = null;
                }
            }
            if (ctor4 != null) {
                return ctor4.newInstance(NIGHT_VISION_ID, NIGHT_VISION_DURATION_TICKS, NIGHT_VISION_AMPLIFIER, true);
            }

            Constructor<?> ctor3 = potionEffectCtor3;
            if (ctor3 == null || ctor3.getDeclaringClass() != potionEffectClass) {
                try {
                    ctor3 = potionEffectClass.getDeclaredConstructor(int.class, int.class, int.class);
                    ctor3.setAccessible(true);
                    potionEffectCtor3 = ctor3;
                } catch (NoSuchMethodException ignored) {
                    ctor3 = null;
                }
            }
            if (ctor3 != null) {
                return ctor3.newInstance(NIGHT_VISION_ID, NIGHT_VISION_DURATION_TICKS, NIGHT_VISION_AMPLIFIER);
            }
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        }
        return null;
    }

    private static Method resolveAddPotionEffectMethod(Class<?> owner, Class<?> potionEffectClass) {
        Method method = resolveMethod(owner, new String[]{"func_70690_d", "addPotionEffect"}, potionEffectClass);
        if (method != null) {
            return method;
        }

        Class<?> cursor = owner;
        while (cursor != null) {
            Method[] methods = cursor.getDeclaredMethods();
            for (Method candidate : methods) {
                Class<?>[] params = candidate.getParameterTypes();
                if (params.length != 1) {
                    continue;
                }
                if (!params[0].isAssignableFrom(potionEffectClass) && !potionEffectClass.isAssignableFrom(params[0])) {
                    continue;
                }
                String name = candidate.getName();
                if ("func_70690_d".equals(name) || "addPotionEffect".equals(name)) {
                    candidate.setAccessible(true);
                    return candidate;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static Method resolveMethod(Class<?> owner, String[] names, Class<?>... parameterTypes) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name, parameterTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    // Try next candidate.
                }
            }
            cursor = cursor.getSuperclass();
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
