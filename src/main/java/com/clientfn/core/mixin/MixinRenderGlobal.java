package com.clientfn.mixin.core;

import com.clientfn.optifine.RenderPathSelector;
import com.clientfn.perf.render.GLStateCache;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.RenderGlobal")
public abstract class MixinRenderGlobal {

    @Inject(method = {"func_147589_a", "renderEntities", "renderTileEntities"}, at = @At("HEAD"), require = 0)
    private void clientfn$invalidateStateCache(CallbackInfo ci) {
        if (RenderPathSelector.USE_COMPAT_PATH) {
            return;
        }
        GLStateCache.invalidate();
    }

    @Redirect(
        method = {"func_147589_a", "renderEntities", "renderTileEntities"},
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"),
        require = 0
    )
    private void clientfn$redirectGlEnable(int cap) {
        if (RenderPathSelector.USE_COMPAT_PATH) {
            GL11.glEnable(cap);
            return;
        }
        if (cap == GL11.GL_BLEND) {
            GLStateCache.setBlend(true);
            return;
        }
        if (cap == GL11.GL_ALPHA_TEST) {
            GLStateCache.setAlphaTest(true);
            return;
        }
        if (cap == GL11.GL_LIGHTING) {
            GLStateCache.setLighting(true);
            return;
        }
        GL11.glEnable(cap);
    }

    @Redirect(
        method = {"func_147589_a", "renderEntities", "renderTileEntities"},
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"),
        require = 0
    )
    private void clientfn$redirectGlDisable(int cap) {
        if (RenderPathSelector.USE_COMPAT_PATH) {
            GL11.glDisable(cap);
            return;
        }
        if (cap == GL11.GL_BLEND) {
            GLStateCache.setBlend(false);
            return;
        }
        if (cap == GL11.GL_ALPHA_TEST) {
            GLStateCache.setAlphaTest(false);
            return;
        }
        if (cap == GL11.GL_LIGHTING) {
            GLStateCache.setLighting(false);
            return;
        }
        GL11.glDisable(cap);
    }
}
