package com.clientfn.mixin.core;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * Fixes a login race where packets can arrive before packetListener is assigned.
 */
@Mixin(targets = "net.minecraft.network.NetworkManager")
public abstract class MixinNetworkManagerRaceFix {

    @Unique
    private static volatile boolean clientfn$loggedEarlyPacketQueue;

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void clientfn$queuePacketsUntilListenerReady(ChannelHandlerContext ctx, Packet packet, CallbackInfo ci) {
        try {
            Object listener = clientfn$getFieldValue(this, "field_150744_m", "packetListener");
            if (listener != null || packet == null) {
                return;
            }

            Object queueObj = clientfn$getFieldValue(this, "field_150748_i", "receivedPacketsQueue");
            if (!(queueObj instanceof Queue<?>)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Queue<Object> queue = (Queue<Object>) queueObj;
            queue.add(packet);
            ci.cancel();

            if (!clientfn$loggedEarlyPacketQueue) {
                clientfn$loggedEarlyPacketQueue = true;
                try { net.minecraft.launchwrapper.LogWrapper.info("[ClientFN] NetworkManager race fix queued packet before listener init."); } catch (Throwable _ig) { System.out.println("[ClientFN] NetworkManager race fix queued packet before listener init."); }
            }
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        }
    }

    @Inject(method = {"func_150718_a", "closeChannel"}, at = @At("TAIL"), require = 0)
    private void clientfn$logCloseReason(CallbackInfo ci) {
        try {
            Object reason = clientfn$getFieldValue(this, "field_150742_o", "terminationReason");
            Object remote = clientfn$invokeNoArg(this, "func_74430_c", "getRemoteAddress");
            String reasonText = reason != null ? reason.toString() : "<null>";
            String remoteText = remote != null ? remote.toString() : "<null>";
            try { net.minecraft.launchwrapper.LogWrapper.info("[ClientFN][LoginDiag] closeChannel remote=" + remoteText + " reason=" + reasonText); } catch (Throwable _ig) { System.out.println("[ClientFN][LoginDiag] closeChannel remote=" + remoteText + " reason=" + reasonText); }
        } catch (Throwable ignored) {
            // Best-effort diagnostics only.
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
