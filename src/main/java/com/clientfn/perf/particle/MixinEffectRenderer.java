package com.clientfn.mixin.perf.particle;

import com.clientfn.perf.particle.ParticleController;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.particle.EffectRenderer")
public abstract class MixinEffectRenderer {

    @Inject(method = {"func_78874_a", "renderParticles"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipRenderParticles(Entity entity, float partialTicks, CallbackInfo ci) {
        if (ParticleController.isParticlesDisabled()) {
            ci.cancel();
        }
    }

    @Inject(method = {"func_78872_b", "renderLitParticles"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipRenderLitParticles(Entity entity, float partialTicks, CallbackInfo ci) {
        if (ParticleController.isParticlesDisabled()) {
            ci.cancel();
        }
    }

    @Inject(method = {"func_78873_a", "addEffect"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipAddEffect(EntityFX effect, CallbackInfo ci) {
        if (ParticleController.isParticlesDisabled()) {
            ci.cancel();
        }
    }
}
