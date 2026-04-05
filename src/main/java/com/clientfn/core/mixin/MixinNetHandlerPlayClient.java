package com.clientfn.mixin.core;

import com.clientfn.benchmark.BenchmarkHarness;
import com.clientfn.core.MinecraftCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.network.NetHandlerPlayClient")
public abstract class MixinNetHandlerPlayClient {
    @Inject(method = {"func_147280_a", "handleRespawn"}, at = @At("TAIL"), require = 0)
    private void clientfn$benchmarkOnRespawn(CallbackInfo ci) {
        BenchmarkHarness.INSTANCE.onRespawn(MinecraftCompat.getMinecraft());
    }
}
