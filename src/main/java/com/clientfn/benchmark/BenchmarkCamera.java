package com.clientfn.benchmark;

import net.minecraft.client.entity.EntityPlayerSP;

public enum BenchmarkCamera {
    NORMAL(0.0F, 0.0F),
    SKY_GAZE(0.0F, -90.0F);

    private final float yaw;
    private final float pitch;
    private boolean active;
    private float restoreYaw;
    private float restorePitch;
    private EntityPlayerSP player;

    BenchmarkCamera(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void activate(EntityPlayerSP player) {
        if (player == null) {
            return;
        }
        if (!this.active || this.player != player) {
            this.restoreYaw = player.rotationYaw;
            this.restorePitch = player.rotationPitch;
            this.active = true;
            this.player = player;
        }
        player.rotationYaw = this.yaw;
        player.rotationPitch = this.pitch;
    }

    public void release() {
        if (!this.active) {
            return;
        }
        if (this.player != null) {
            this.player.rotationYaw = this.restoreYaw;
            this.player.rotationPitch = this.restorePitch;
        }
        this.active = false;
        this.player = null;
    }

    public static void releaseAll() {
        for (BenchmarkCamera scene : values()) {
            scene.release();
        }
    }
}
