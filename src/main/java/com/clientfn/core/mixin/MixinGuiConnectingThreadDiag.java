package com.clientfn.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Catches uncaught Errors (NoClassDefFoundError, LinkageError, etc.) in the
 * connection thread that vanilla only catches as Exception, leaving Errors to
 * silently kill the thread.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public abstract class MixinGuiConnectingThreadDiag {

    @Inject(method = "run", at = @At("HEAD"), require = 0)
    private void clientfn$wrapRunWithErrorCatch(CallbackInfo ci) {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            String msg = "[ClientFN][LoginDiag] CONNECTION THREAD UNCAUGHT ERROR: "
                + e.getClass().getName() + ": " + e.getMessage();
            try {
                net.minecraft.launchwrapper.LogWrapper.info(msg);
            } catch (Throwable _ig) {
                System.out.println(msg);
            }
            e.printStackTrace();
        });
    }
}
