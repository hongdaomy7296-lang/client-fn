package com.clientfn.perf.render;

import org.lwjgl.opengl.GL11;

/**
 * Lightweight state cache that suppresses redundant glEnable/glDisable calls.
 */
public final class GLStateCache {
    private static final int UNKNOWN = -1;
    private static final int DISABLED = 0;
    private static final int ENABLED = 1;

    private static int blendState = UNKNOWN;
    private static int alphaTestState = UNKNOWN;
    private static int lightingState = UNKNOWN;

    private GLStateCache() {
    }

    public static void invalidate() {
        blendState = UNKNOWN;
        alphaTestState = UNKNOWN;
        lightingState = UNKNOWN;
    }

    public static void setBlend(boolean enable) {
        if (!clientfn$shouldApply(enable, blendState)) {
            return;
        }
        if (enable) {
            GL11.glEnable(GL11.GL_BLEND);
            blendState = ENABLED;
        } else {
            GL11.glDisable(GL11.GL_BLEND);
            blendState = DISABLED;
        }
    }

    public static void setAlphaTest(boolean enable) {
        if (!clientfn$shouldApply(enable, alphaTestState)) {
            return;
        }
        if (enable) {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            alphaTestState = ENABLED;
        } else {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            alphaTestState = DISABLED;
        }
    }

    public static void setLighting(boolean enable) {
        if (!clientfn$shouldApply(enable, lightingState)) {
            return;
        }
        if (enable) {
            GL11.glEnable(GL11.GL_LIGHTING);
            lightingState = ENABLED;
        } else {
            GL11.glDisable(GL11.GL_LIGHTING);
            lightingState = DISABLED;
        }
    }

    private static boolean clientfn$shouldApply(boolean enable, int state) {
        if (state == UNKNOWN) {
            return true;
        }
        return enable ? state != ENABLED : state != DISABLED;
    }
}
