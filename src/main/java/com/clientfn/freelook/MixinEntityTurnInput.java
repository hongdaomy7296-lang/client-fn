package com.clientfn.mixin.freelook;

import com.clientfn.freelook.FreelookHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.Entity")
public abstract class MixinEntityTurnInput {

    @Inject(method = {"func_70082_c", "setAngles"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$captureFreelookInput(float yawInput, float pitchInput, CallbackInfo ci) {
        if (FreelookHandler.consumeMouseTurn(this, yawInput, pitchInput)) {
            ci.cancel();
        }
    }
}
