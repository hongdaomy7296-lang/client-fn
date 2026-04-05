package com.clientfn.mixin.core;

import com.clientfn.benchmark.BenchmarkHarness;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftBenchmark {
    @Inject(method = {"func_71407_l", "runTick"}, at = @At("HEAD"), require = 0)
    private void clientfn$benchmarkTick(CallbackInfo ci) {
        BenchmarkHarness.INSTANCE.onClientTick((Minecraft) (Object) this);
    }

    @Inject(method = {"func_71411_J", "runGameLoop"}, at = @At("HEAD"), require = 0)
    private void clientfn$benchmarkFrame(CallbackInfo ci) {
        BenchmarkHarness.INSTANCE.onFrame((Minecraft) (Object) this, System.nanoTime());
    }

    @Inject(method = {"func_71353_a", "loadWorld"}, at = @At("TAIL"), require = 0)
    private void clientfn$benchmarkOnWorldLoad(CallbackInfo ci) {
        BenchmarkHarness.INSTANCE.onWorldChanged((Minecraft) (Object) this);
    }
}
