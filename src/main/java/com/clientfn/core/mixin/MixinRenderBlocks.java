package com.clientfn.mixin.core;

import com.clientfn.perf.lighting.LightingController;
import com.clientfn.optifine.RenderPathSelector;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.RenderBlocks")
public abstract class MixinRenderBlocks {

    /**
     * Shadow the flat-lighting method so we can call it directly instead of reflection.
     * SRG name: func_147736_d  |  MCP name: renderStandardBlockWithColorMultiplier
     */
    @Shadow(aliases = {"renderStandardBlockWithColorMultiplier"})
    public abstract boolean func_147736_d(Block block, int x, int y, int z, float red, float green, float blue);

    @Inject(method = {"func_147751_a", "renderStandardBlockWithAmbientOcclusion"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$bypassAoFull(
        Block block,
        int x,
        int y,
        int z,
        float red,
        float green,
        float blue,
        CallbackInfoReturnable<Boolean> cir
    ) {
        clientfn$tryForceFlatLighting(block, x, y, z, red, green, blue, cir);
    }

    @Inject(method = {"func_147808_b", "renderStandardBlockWithAmbientOcclusionPartial"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$bypassAoPartial(
        Block block,
        int x,
        int y,
        int z,
        float red,
        float green,
        float blue,
        CallbackInfoReturnable<Boolean> cir
    ) {
        clientfn$tryForceFlatLighting(block, x, y, z, red, green, blue, cir);
    }

    @Unique
    private void clientfn$tryForceFlatLighting(
        Block block,
        int x,
        int y,
        int z,
        float red,
        float green,
        float blue,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!LightingController.isSmoothLightingDisabled()) {
            return;
        }
        if (RenderPathSelector.USE_COMPAT_PATH) {
            return;
        }

        // Direct call via @Shadow — no reflection, no per-frame overhead.
        boolean result = this.func_147736_d(block, x, y, z, red, green, blue);
        cir.setReturnValue(result);
    }
}
