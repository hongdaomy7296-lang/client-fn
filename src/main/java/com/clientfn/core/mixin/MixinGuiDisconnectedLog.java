package com.clientfn.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Logs disconnect screen payload so timeout reasons are visible in latest.log.
 */
@Mixin(targets = "net.minecraft.client.gui.GuiDisconnected")
public abstract class MixinGuiDisconnectedLog {

    @Unique
    private boolean clientfn$logged;

    @Inject(method = {"func_73866_w_", "initGui"}, at = @At("HEAD"), require = 0)
    private void clientfn$logDisconnectScreen(CallbackInfo ci) {
        if (this.clientfn$logged) {
            return;
        }
        this.clientfn$logged = true;
        try {
            Object reason = clientfn$getFieldValue(this, "field_146306_a", "reason");
            Object message = clientfn$getFieldValue(this, "field_146304_f", "message");
            System.out.println(
                "[ClientFN][LoginDiag] GuiDisconnected"
                    + " reason=" + (reason != null ? String.valueOf(reason) : "<null>")
                    + " message=" + (message != null ? String.valueOf(message) : "<null>")
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
}
