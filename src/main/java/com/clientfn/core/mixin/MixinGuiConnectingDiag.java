package com.clientfn.mixin.core;

import com.clientfn.core.LoginDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting")
public abstract class MixinGuiConnectingDiag {

    @Unique
    private long clientfn$connectScreenOpenedMs;
    @Unique
    private long clientfn$lastStateLogMs;

    @Inject(method = {"func_73876_c", "updateScreen"}, at = @At("RETURN"), require = 0)
    private void clientfn$logConnectingState(CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (this.clientfn$connectScreenOpenedMs == 0L) {
            this.clientfn$connectScreenOpenedMs = now;
            LoginDiagnostics.log("GuiConnecting opened (first updateScreen tick).");
        }
        if (now - this.clientfn$lastStateLogMs < 5000L) {
            return;
        }
        this.clientfn$lastStateLogMs = now;

        Object manager = clientfn$getFieldValue(this, "field_146371_g", "networkManager");
        if (manager == null) {
            LoginDiagnostics.log("GuiConnecting state: manager=null elapsedMs=%d", Long.valueOf(now - this.clientfn$connectScreenOpenedMs));
            return;
        }

        boolean channelOpen = clientfn$invokeBoolean(manager, "func_150724_d", "isChannelOpen");
        Object remote = clientfn$invokeObject(manager, "func_74430_c", "getRemoteAddress");
        Object reason = clientfn$invokeObject(manager, "func_150730_f", "getExitMessage");
        LoginDiagnostics.log(
            "GuiConnecting state: channelOpen=%s remote=%s reason=%s elapsedMs=%d",
            Boolean.valueOf(channelOpen),
            LoginDiagnostics.safeAddress(remote),
            LoginDiagnostics.safeChat(reason),
            Long.valueOf(now - this.clientfn$connectScreenOpenedMs)
        );
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
    private static boolean clientfn$invokeBoolean(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), names);
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
    private static Object clientfn$invokeObject(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), names);
            if (method == null) {
                return null;
            }
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static Method clientfn$resolveMethod(Class<?> owner, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name);
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
}
