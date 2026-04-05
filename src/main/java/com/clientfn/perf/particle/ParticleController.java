package com.clientfn.perf.particle;

import com.clientfn.optifine.RenderPathSelector;
import com.clientfn.ui.ClientConfig;

/**
 * Runtime accessors for render-cut toggles.
 */
public final class ParticleController {

    private ParticleController() {
    }

    public static void syncFromConfig() {
        // Runtime reads use ClientConfig directly for immediate UI toggle response.
    }

    public static boolean isParticlesDisabled() {
        return ClientConfig.INSTANCE.isEnabled("particles.disabled");
    }

    public static boolean isCloudsDisabled() {
        return RenderPathSelector.USE_EXTREME_PATH && ClientConfig.INSTANCE.isEnabled("clouds.disabled");
    }

    public static boolean isEntityShadowsDisabled() {
        return ClientConfig.INSTANCE.isEnabled("entity_shadows.disabled");
    }

    public static boolean isAnimationsDisabled() {
        return ClientConfig.INSTANCE.isEnabled("animations.disabled");
    }
}
