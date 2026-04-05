package com.clientfn.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Periodic connect-screen state logging for multiplayer timeout diagnosis.
 * This mixin is diagnostics-only and does not alter vanilla behavior.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting")
public abstract class MixinGuiConnectingStateLog {

    @Unique
    private long clientfn$lastStateLogMs;

    @Inject(method = {"func_73876_c", "updateScreen"}, at = @At("TAIL"), require = 0)
    private void clientfn$logConnectingState(CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - this.clientfn$lastStateLogMs < 5000L) {
            return;
        }
        this.clientfn$lastStateLogMs = now;

        try {
            Object manager = clientfn$getFieldValue(this, "field_146371_g", "networkManager");
            if (manager == null) {
                System.out.println("[ClientFN][LoginDiag] GuiConnecting manager=<null>");
                return;
            }

            boolean open = clientfn$invokeBooleanNoArg(manager, "func_150724_d", "isChannelOpen");
            Object handler = clientfn$invokeObjectNoArg(manager, "func_150729_e", "getNetHandler");
            Object reason = clientfn$invokeObjectNoArg(manager, "func_150730_f", "getExitMessage");
            Object remote = clientfn$invokeObjectNoArg(manager, "func_74430_c", "getRemoteAddress");

            System.out.println(
                "[ClientFN][LoginDiag] GuiConnecting"
                    + " open=" + open
                    + " handler=" + (handler != null ? handler.getClass().getName() : "<null>")
                    + " remote=" + (remote != null ? String.valueOf(remote) : "<null>")
                    + " reason=" + (reason != null ? String.valueOf(reason) : "<null>")
            );
        } catch (Throwable ignored) {
            // Diagnostics only.
        }
    }

    @Unique
    private static Object clientfn$getFieldValue(Object target, String... names) {
        Field field = clientfn$resolveField(target.getClass(), names);
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @Unique
    private static Field clientfn$resolveField(Class<?> owner, String... names) {
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

    @Unique
    private static Method clientfn$resolveMethod(Class<?> owner, Class<?>[] paramTypes, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name, paramTypes);
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

    @Unique
    private static boolean clientfn$invokeBooleanNoArg(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), new Class<?>[0], names);
            if (method == null) {
                return false;
            }
            Object value = method.invoke(target);
            return value instanceof Boolean && ((Boolean) value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static Object clientfn$invokeObjectNoArg(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), new Class<?>[0], names);
            if (method == null) {
                return null;
            }
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
