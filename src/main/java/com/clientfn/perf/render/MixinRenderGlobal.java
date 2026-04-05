package com.clientfn.mixin.perf.render;

import com.clientfn.perf.particle.ParticleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.RenderGlobal")
public abstract class MixinRenderGlobal {

    @Inject(method = {"func_72726_b", "doSpawnParticle"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipSpawnParticle(CallbackInfoReturnable<Object> cir) {
        if (ParticleController.isParticlesDisabled()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = {"func_72718_b", "renderClouds"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipRenderClouds(CallbackInfo ci) {
        if (ParticleController.isCloudsDisabled()) {
            ci.cancel();
        }
    }

    @Inject(method = {"func_72736_c", "renderCloudsFancy"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipRenderCloudsFancy(CallbackInfo ci) {
        if (ParticleController.isCloudsDisabled()) {
            ci.cancel();
        }
    }
}
