package com.clientfn.mixin.perf.animation;

import com.clientfn.perf.particle.ParticleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.model.ModelBase")
public abstract class MixinModelBase {

    @Inject(method = {"func_78086_a", "setLivingAnimations"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipLivingAnimations(CallbackInfo ci) {
        if (ParticleController.isAnimationsDisabled()) {
            ci.cancel();
        }
    }
}
