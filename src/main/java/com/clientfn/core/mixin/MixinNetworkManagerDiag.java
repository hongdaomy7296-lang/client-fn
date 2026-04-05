package com.clientfn.mixin.core;

import com.clientfn.core.LoginDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.network.NetworkManager")
public abstract class MixinNetworkManagerDiag {
    @Inject(method = {"channelInactive"}, at = @At("HEAD"), require = 0)
    private void clientfn$onChannelInactive(Object ctx, CallbackInfo ci) {
        LoginDiagnostics.log(
            "Network channel inactive (remote=%s, closeReason=%s).",
            LoginDiagnostics.safeAddress(clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress")),
            LoginDiagnostics.safeChat(clientfn$invokeNoArg(this, "func_150730_f", "getExitMessage"))
        );
    }

    @Inject(method = {"exceptionCaught"}, at = @At("HEAD"), require = 0)
    private void clientfn$onNetworkException(Object ctx, Throwable throwable, CallbackInfo ci) {
        LoginDiagnostics.log(
            "Network exception (remote=%s): %s",
            LoginDiagnostics.safeAddress(clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress")),
            LoginDiagnostics.safeThrowable(throwable)
        );
    }

    @Inject(method = {"func_150718_a", "closeChannel"}, at = @At("HEAD"), require = 0)
    private void clientfn$onCloseChannel(Object reason, CallbackInfo ci) {
        LoginDiagnostics.log(
            "Network closeChannel called (remote=%s, reason=%s).",
            LoginDiagnostics.safeAddress(clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress")),
            LoginDiagnostics.safeChat(reason)
        );
    }

    private static Object clientfn$invokeNoArg(Object target, String... names) {
        if (target == null) {
            return null;
        }
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
