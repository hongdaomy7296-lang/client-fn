package net.minecraft.util;

public final class MathHelper {
    private MathHelper() {
    }

    public static float clamp_float(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
