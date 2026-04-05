package com.clientfn.perf.lighting;

import com.clientfn.optifine.OptiFineDetector;
import com.clientfn.ui.ClientConfig;

/**
 * Runtime lighting toggles backed by {@link ClientConfig}.
 */
public final class LightingController {
    private static volatile boolean smoothLightingDisabled = false;
    private static volatile boolean dynamicLightingDisabled = false;
    private static volatile boolean mipmapDisabled = false;

    private LightingController() {
    }

    public static void syncFromConfig() {
        smoothLightingDisabled = ClientConfig.INSTANCE.isEnabled("smooth_lighting.disabled");
        dynamicLightingDisabled = ClientConfig.INSTANCE.isEnabled("dynamic_lighting.disabled");
        mipmapDisabled = ClientConfig.INSTANCE.isEnabled("mipmap.disabled");
    }

    public static boolean isSmoothLightingDisabled() {
        return smoothLightingDisabled;
    }

    public static boolean isMipmapDisabled() {
        return mipmapDisabled;
    }

    public static boolean shouldDisableDynamicLightingPath() {
        return dynamicLightingDisabled && !OptiFineDetector.INSTANCE.isOptiFinePresent();
    }
}
