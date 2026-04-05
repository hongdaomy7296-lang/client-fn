package com.clientfn.mixin.perf.animation;

import com.clientfn.perf.particle.ParticleController;
import com.clientfn.optifine.RenderPathSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.TextureMap")
public abstract class MixinTextureMap {

    @Inject(method = {"func_94248_c", "updateAnimations"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipUpdateAnimations(CallbackInfo ci) {
        if (RenderPathSelector.USE_EXTREME_PATH && ParticleController.isAnimationsDisabled()) {
            ci.cancel();
        }
    }
}
