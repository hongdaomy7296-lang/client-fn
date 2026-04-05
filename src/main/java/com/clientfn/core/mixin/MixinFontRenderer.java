package com.clientfn.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.FontRenderer")
public abstract class MixinFontRenderer {

    @Shadow
    public abstract int func_78276_b(String text, int x, int y, int color);

    @Inject(method = {"func_78261_a", "drawStringWithShadow"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipShadowOnExtremePath(
        String text,
        int x,
        int y,
        int color,
        CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(this.func_78276_b(text, x, y, color));
    }
}
