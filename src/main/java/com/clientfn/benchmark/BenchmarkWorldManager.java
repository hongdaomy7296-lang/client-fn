package com.clientfn.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Best-effort benchmark world bootstrap and fixed-condition enforcer.
 */
public final class BenchmarkWorldManager {
    public static final String WORLD_NAME = "clientfn_benchmark";
    public static final BlockPos FIXED_POS = new BlockPos(0, 64, 0);
    public static final long FIXED_TIME = 6000L;
    public static final boolean NO_RAIN = true;

    private static final long REAPPLY_INTERVAL_MS = 500L;
    private static final long WORLD_LOAD_RETRY_MS = 5000L;
    private static long lastApplyMs = 0L;
    private static long lastLoadAttemptMs = 0L;

    private BenchmarkWorldManager() {
    }

    public static void ensureBenchmarkWorldLoaded(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        if (minecraft.theWorld != null && isBenchmarkWorld(minecraft.theWorld)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLoadAttemptMs < WORLD_LOAD_RETRY_MS) {
            return;
        }
        lastLoadAttemptMs = now;

        Object worldSettings = createSuperflatWorldSettings();
        if (worldSettings == null) {
            return;
        }
        invokeByNameAndArity(
            minecraft,
            new String[]{"launchIntegratedServer", "func_71371_a"},
            3,
            WORLD_NAME,
            WORLD_NAME,
            worldSettings
        );
    }

    public static boolean isBenchmarkWorld(WorldClient world) {
        if (world == null) {
            return false;
        }
        Object worldInfo = invokeByNameAndArity(world, new String[]{"getWorldInfo", "func_72912_H"}, 0);
        if (worldInfo == null) {
            return false;
        }
        Object worldName = invokeByNameAndArity(worldInfo, new String[]{"getWorldName", "func_76065_j"}, 0);
        if (!(worldName instanceof String)) {
            return false;
        }
        String name = ((String) worldName).trim();
        return WORLD_NAME.equalsIgnoreCase(name);
    }

    public static void ensureBenchmarkConditions(WorldClient world, EntityPlayerSP player) {
        if (world == null || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastApplyMs < REAPPLY_INTERVAL_MS) {
            return;
        }
        lastApplyMs = now;

        double targetX = FIXED_POS.getX() + 0.5D;
        double targetY = FIXED_POS.getY();
        double targetZ = FIXED_POS.getZ() + 0.5D;
        double dx = player.posX - targetX;
        double dy = player.posY - targetY;
        double dz = player.posZ - targetZ;
        if ((dx * dx + dy * dy + dz * dz) > 0.01D) {
            player.posX = targetX;
            player.posY = targetY;
            player.posZ = targetZ;
            Object moved = invokeByNameAndArity(
                player,
                new String[]{"setPositionAndUpdate", "func_70634_a"},
                3,
                targetX,
                targetY,
                targetZ
            );
            if (moved == null) {
                invokeByNameAndArity(
                    player,
                    new String[]{"setPosition", "func_70107_b"},
                    3,
                    targetX,
                    targetY,
                    targetZ
                );
            }
        }

        invokeByNameAndArity(world, new String[]{"setWorldTime", "func_72877_b"}, 1, FIXED_TIME);
        Object worldInfo = invokeByNameAndArity(world, new String[]{"getWorldInfo", "func_72912_H"}, 0);
        if (worldInfo != null) {
            invokeByNameAndArity(worldInfo, new String[]{"setWorldTime", "func_76068_b"}, 1, FIXED_TIME);
        }

        if (NO_RAIN) {
            invokeByNameAndArity(world, new String[]{"setRainStrength", "func_72894_k"}, 1, 0.0F);
            invokeByNameAndArity(world, new String[]{"setThunderStrength", "func_147442_i"}, 1, 0.0F);
            if (worldInfo != null) {
                invokeByNameAndArity(worldInfo, new String[]{"setRaining", "func_76084_b"}, 1, false);
                invokeByNameAndArity(worldInfo, new String[]{"setThundering", "func_76069_a"}, 1, false);
                invokeByNameAndArity(worldInfo, new String[]{"setRainTime", "func_76080_g"}, 1, 0);
                invokeByNameAndArity(worldInfo, new String[]{"setThunderTime", "func_76090_f"}, 1, 0);
                invokeByNameAndArity(worldInfo, new String[]{"setCleanWeatherTime", "func_176142_i"}, 1, Integer.MAX_VALUE);
            }
        }
    }

    private static Object createSuperflatWorldSettings() {
        try {
            Class<?> settingsClass = Class.forName("net.minecraft.world.WorldSettings");
            Class<?> gameTypeClass = Class.forName("net.minecraft.world.WorldSettings$GameType");
            Class<?> worldTypeClass = Class.forName("net.minecraft.world.WorldType");

            Object creative = getStaticField(gameTypeClass, "CREATIVE");
            Object flat = getStaticField(worldTypeClass, "FLAT");
            if (creative == null || flat == null) {
                return null;
            }

            Constructor<?> ctor = settingsClass.getConstructor(
                long.class,
                gameTypeClass,
                boolean.class,
                boolean.class,
                worldTypeClass
            );
            Object settings = ctor.newInstance(1L, creative, true, false, flat);
            invokeByNameAndArity(settings, new String[]{"enableCommands", "func_77159_a"}, 0);
            invokeByNameAndArity(settings, new String[]{"setGeneratorOptions", "func_82750_a"}, 1, "2;7,2x3,2;1;");
            return settings;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getStaticField(Class<?> owner, String fieldName) {
        try {
            Field field = owner.getField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeByNameAndArity(Object target, String[] names, int arity, Object... args) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), names, arity);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String[] names, int arity) {
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
}
