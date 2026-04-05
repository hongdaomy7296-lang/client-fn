package com.clientfn.mixin.core;

import com.clientfn.optifine.RenderPathSelector;
import com.clientfn.perf.render.TextureFilterController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.TextureMap")
public abstract class MixinTextureMap {

    @Inject(method = {"func_110551_a", "loadTexture", "func_110571_b", "loadTextureAtlas"}, at = @At("HEAD"), require = 0)
    private void clientfn$onTextureReloadStart(IResourceManager resourceManager, CallbackInfo ci) {
        if (RenderPathSelector.USE_COMPAT_PATH) {
            return;
        }
        TextureFilterController.invalidate();
    }

    @Inject(method = {"func_110551_a", "loadTexture", "func_110571_b", "loadTextureAtlas"}, at = @At("TAIL"), require = 0)
    private void clientfn$onTextureReloadEnd(IResourceManager resourceManager, CallbackInfo ci) {
        if (RenderPathSelector.USE_COMPAT_PATH) {
            return;
        }
        TextureFilterController.enforceNearestNoAf();
    }
}
