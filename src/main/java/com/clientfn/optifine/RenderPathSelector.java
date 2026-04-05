package com.clientfn.optifine;

/**
 * Startup-cached render path toggle:
 * compat path for OptiFine environments, extreme path otherwise.
 */
public final class RenderPathSelector {

    public static final boolean USE_COMPAT_PATH = OptiFineDetector.INSTANCE.shouldUseCompatPath();
    public static final boolean USE_EXTREME_PATH = !USE_COMPAT_PATH;

    private RenderPathSelector() {
    }
}
