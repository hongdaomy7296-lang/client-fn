package com.clientfn.fog;

import com.clientfn.ui.ClientConfig;
import com.clientfn.ui.IClientConfig;

/**
 * Runtime fog override state, synchronized from ClientConfig.
 */
public final class FogEditor {
    public static final FogEditor INSTANCE = new FogEditor();

    public static final float MIN_START = 0.0F;
    public static final float MAX_START = 512.0F;
    public static final float MIN_END = 0.5F;
    public static final float MAX_END = 512.0F;

    private volatile boolean enabled;
    private volatile float fogStart;
    private volatile float fogEnd;

    private FogEditor() {
        syncFromConfig();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getFogStart() {
        return this.fogStart;
    }

    public float getFogEnd() {
        return this.fogEnd;
    }

    public synchronized void setFogStart(float value) {
        this.fogStart = clamp(value, MIN_START, MAX_START);
        if (this.fogStart > this.fogEnd) {
            this.fogEnd = this.fogStart;
        }
    }

    public synchronized void setFogEnd(float value) {
        this.fogEnd = clamp(value, MIN_END, MAX_END);
        if (this.fogEnd < this.fogStart) {
            this.fogStart = this.fogEnd;
        }
    }

    public synchronized void syncFromConfig() {
        IClientConfig cfg = ClientConfig.INSTANCE;
        this.enabled = cfg.isEnabled("fog.enabled");
        setFogStart(cfg.getFloat("fog.start", 0.0F));
        setFogEnd(cfg.getFloat("fog.end", 16.0F));
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
