package com.clientfn.mixin.perf.render;

import com.clientfn.perf.particle.ParticleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.Render")
public abstract class MixinRenderShadow {

    @Inject(method = {"func_76975_c", "renderShadow"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipEntityShadow(CallbackInfo ci) {
        if (ParticleController.isEntityShadowsDisabled()) {
            ci.cancel();
        }
    }
}
