package com.clientfn.mixin.hud;

import com.clientfn.hud.HudController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiIngame")
public abstract class MixinGuiIngameHud {

    @Inject(method = {"func_96136_a", "renderScoreboard"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipScoreboardDraw(CallbackInfo ci) {
        if (HudController.isScoreboardHidden()) {
            ci.cancel();
        }
    }
}
