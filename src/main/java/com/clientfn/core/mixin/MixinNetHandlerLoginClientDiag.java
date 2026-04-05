package com.clientfn.mixin.core;

import com.clientfn.core.LoginDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.client.network.NetHandlerLoginClient")
public abstract class MixinNetHandlerLoginClientDiag {

    @Inject(method = {"func_147389_a", "handleEncryptionRequest"}, at = @At("HEAD"), require = 0)
    private void clientfn$onEncryptionRequest(Object packet, CallbackInfo ci) {
        String serverId = clientfn$extractEncryptionServerId(packet);
        LoginDiagnostics.log("Received S01 encryption request (serverId=%s).", serverId);
    }

    @Inject(method = {"func_147390_a", "handleLoginSuccess"}, at = @At("HEAD"), require = 0)
    private void clientfn$onLoginSuccess(Object packet, CallbackInfo ci) {
        LoginDiagnostics.log("Received S02 login success.");
    }

    @Inject(method = {"func_147388_a", "handleDisconnect"}, at = @At("HEAD"), require = 0)
    private void clientfn$onDisconnectPacket(Object packet, CallbackInfo ci) {
        LoginDiagnostics.log("Received S00 disconnect packet: %s", LoginDiagnostics.safeChat(clientfn$extractDisconnectReason(packet)));
    }

    @Inject(method = {"func_147231_a", "onDisconnect"}, at = @At("HEAD"), require = 0)
    private void clientfn$onHandlerDisconnect(Object reason, CallbackInfo ci) {
        LoginDiagnostics.log("Login handler disconnected: %s", LoginDiagnostics.safeChat(reason));
    }

    @Inject(method = {"func_147232_a", "onConnectionStateTransition"}, at = @At("HEAD"), require = 0)
    private void clientfn$onStateTransition(Object from, Object to, CallbackInfo ci) {
        LoginDiagnostics.log("Login state transition: %s -> %s", String.valueOf(from), String.valueOf(to));
    }

    @Unique
    private static Object clientfn$extractDisconnectReason(Object packet) {
        if (packet == null) {
            return null;
        }
        try {
            Method method = clientfn$resolveMethod(packet.getClass(), "func_149603_c", "getReason");
            if (method == null) {
                return null;
            }
            Object value = method.invoke(packet);
            return value;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static String clientfn$extractEncryptionServerId(Object packet) {
        if (packet == null) {
            return "<null>";
        }
        try {
            Method method = clientfn$resolveMethod(packet.getClass(), "func_149609_c", "getServerId");
            if (method == null) {
                return "<unknown>";
            }
            Object value = method.invoke(packet);
            return value != null ? value.toString() : "<null>";
        } catch (Throwable ignored) {
            return "<error>";
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
