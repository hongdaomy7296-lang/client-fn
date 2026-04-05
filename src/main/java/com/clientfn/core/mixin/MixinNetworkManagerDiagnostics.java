package com.clientfn.mixin.core;

import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Network diagnostics for login timeout troubleshooting.
 * This mixin logs key state transitions and exceptions without mutating logic.
 */
@Mixin(targets = "net.minecraft.network.NetworkManager")
public abstract class MixinNetworkManagerDiagnostics {

    @Inject(method = {"func_150719_a", "setNetHandler"}, at = @At("TAIL"), require = 0)
    private void clientfn$logSetNetHandler(CallbackInfo ci) {
        try {
            Object listener = clientfn$getFieldValue(this, "field_150744_m", "packetListener");
            Object remote = clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress");
            clientfn$logInfo(
                "[ClientFN][LoginDiag] setNetHandler"
                    + " listener=" + (listener != null ? listener.getClass().getName() : "<null>")
                    + " remote=" + (remote != null ? String.valueOf(remote) : "<null>")
            );
        } catch (Throwable ignored) {
            // Diagnostics only.
        }
    }

    @Inject(method = {"func_150718_a", "closeChannel"}, at = @At("TAIL"), require = 0)
    private void clientfn$logCloseChannel(CallbackInfo ci) {
        try {
            Object listener = clientfn$getFieldValue(this, "field_150744_m", "packetListener");
            Object remote = clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress");
            Object storedReason = clientfn$getFieldValue(this, "field_150742_o", "terminationReason");
            clientfn$logInfo(
                "[ClientFN][LoginDiag] closeChannel"
                    + " remote=" + (remote != null ? String.valueOf(remote) : "<null>")
                    + " listener=" + (listener != null ? listener.getClass().getName() : "<null>")
                    + " reasonStored=" + (storedReason != null ? String.valueOf(storedReason) : "<null>")
            );
        } catch (Throwable ignored) {
            // Diagnostics only.
        }
    }

    @Inject(method = "channelInactive", at = @At("HEAD"), require = 0)
    private void clientfn$logChannelInactive(ChannelHandlerContext ctx, CallbackInfo ci) {
        try {
            Object remote = clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress");
            Object listener = clientfn$getFieldValue(this, "field_150744_m", "packetListener");
            clientfn$logInfo(
                "[ClientFN][LoginDiag] channelInactive"
                    + " remote=" + (remote != null ? String.valueOf(remote) : "<null>")
                    + " listener=" + (listener != null ? listener.getClass().getName() : "<null>")
            );
        } catch (Throwable ignored) {
            // Diagnostics only.
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"), require = 0)
    private void clientfn$logExceptionCaught(ChannelHandlerContext ctx, Throwable throwable, CallbackInfo ci) {
        try {
            Object remote = clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress");
            clientfn$logInfo(
                "[ClientFN][LoginDiag] exceptionCaught"
                    + " remote=" + (remote != null ? String.valueOf(remote) : "<null>")
                    + " type=" + (throwable != null ? throwable.getClass().getName() : "<null>")
                    + " message=" + (throwable != null ? String.valueOf(throwable.getMessage()) : "<null>")
            );
            if (throwable != null) {
                throwable.printStackTrace();
            }
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
    private static void clientfn$logInfo(String message) {
        try {
            net.minecraft.launchwrapper.LogWrapper.info(message);
        } catch (Throwable ignored) {
            System.out.println(message);
        }
    }

    @Unique
    private static Object clientfn$invokeNoArg(Object target, String... names) {
        Class<?> cursor = target.getClass();
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (NoSuchMethodException ignored) {
                    // Try next candidate.
                } catch (Throwable ignored) {
                    return null;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }
}
