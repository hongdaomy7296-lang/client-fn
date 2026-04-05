package com.clientfn.mixin.hud;

import com.clientfn.hud.HudController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiNewChat")
public abstract class MixinGuiNewChat {

    @Inject(method = {"func_146230_a", "drawChat"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipChatDraw(CallbackInfo ci) {
        if (HudController.isChatHidden()) {
            ci.cancel();
        }
    }
}
