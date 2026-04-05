package com.clientfn.mixin.core;

import com.clientfn.fog.FogEditor;
import com.clientfn.freelook.FreelookHandler;
import com.clientfn.hud.HudController;
import com.clientfn.core.PlayerFeatureController;
import com.clientfn.perf.lighting.LightingController;
import com.clientfn.perf.particle.ParticleController;
import com.clientfn.ui.ClientConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Lifecycle hooks + global reset key.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Unique
    private long clientfn$lastResetHotkeyMs = 0L;
    @Unique
    private static volatile boolean clientfn$threadCrashLoggerInstalled = false;

    @Inject(method = {"func_71407_l", "runTick"}, at = @At("HEAD"), require = 0)
    private void clientfn$onRunTick(CallbackInfo ci) {
        if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
            long now = System.currentTimeMillis();
            if (now - this.clientfn$lastResetHotkeyMs >= 200L) {
                this.clientfn$lastResetHotkeyMs = now;
                clientfn$resetDefaultsFromHotkey();
            }
        }
        FreelookHandler.onClientTick((Minecraft) (Object) this);
        PlayerFeatureController.onClientTick((Minecraft) (Object) this);
    }

    @Unique
    private void clientfn$resetDefaultsFromHotkey() {
        ClientConfig.INSTANCE.resetToDefaults();
        FogEditor.INSTANCE.syncFromConfig();
        ParticleController.syncFromConfig();
        LightingController.syncFromConfig();
        HudController.syncFromConfig();
        FreelookHandler.syncFromConfig();
        PlayerFeatureController.syncFromConfig();
        clientfn$appendHotkeyLog();
        System.out.println("[ClientFN] Reset configuration to defaults via key K.");
    }

    @Unique
    private void clientfn$appendHotkeyLog() {
        try {
            File baseDir = ClientConfig.INSTANCE.resolveConfigDirectory();
            Path path = (baseDir != null ? baseDir.toPath() : new File(".").toPath()).resolve("clientfn_hotkey.log");
            String line = "K_RESET " + System.currentTimeMillis() + System.lineSeparator();
            Files.write(path, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
            // Keep runtime path resilient.
        }
    }

    @Inject(method = {"func_71384_a", "startGame"}, at = @At("TAIL"), require = 0)
    private void clientfn$loadConfigOnStartup(CallbackInfo ci) {
        clientfn$installThreadCrashLogger();
        ClientConfig.INSTANCE.load();
        FogEditor.INSTANCE.syncFromConfig();
        ParticleController.syncFromConfig();
        LightingController.syncFromConfig();
        HudController.syncFromConfig();
        FreelookHandler.syncFromConfig();
        PlayerFeatureController.syncFromConfig();
    }

    @Inject(method = {"func_71400_g", "shutdownMinecraftApplet"}, at = @At("HEAD"), require = 0)
    private void clientfn$saveConfigOnShutdownApplet(CallbackInfo ci) {
        ClientConfig.INSTANCE.save();
    }

    @Inject(method = {"func_71379_u", "isAmbientOcclusionEnabled"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void clientfn$forceAmbientOcclusionOff(CallbackInfoReturnable<Boolean> cir) {
        if (LightingController.isSmoothLightingDisabled()) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static void clientfn$installThreadCrashLogger() {
        if (clientfn$threadCrashLoggerInstalled) {
            return;
        }
        synchronized (MixinMinecraft.class) {
            if (clientfn$threadCrashLoggerInstalled) {
                return;
            }

            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    try {
                        System.err.println(
                            "[ClientFN][ThreadCrash] name="
                                + (t != null ? t.getName() : "<null>")
                                + " error="
                                + (e != null ? e.getClass().getName() : "<null>")
                                + " msg="
                                + (e != null ? e.getMessage() : "<null>")
                        );
                        if (e != null) {
                            e.printStackTrace();
                        }
                    } catch (Throwable ignored) {
                        // Avoid recursive error handling.
                    }

                    if (previous != null) {
                        previous.uncaughtException(t, e);
                    }
                }
            });
            clientfn$threadCrashLoggerInstalled = true;
        }
    }
}
