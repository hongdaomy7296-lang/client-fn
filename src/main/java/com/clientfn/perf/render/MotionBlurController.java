package com.clientfn.perf.render;

import com.clientfn.ui.ClientConfig;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Adaptive motion blur that raises blend while moving and lowers it while static.
 * Uses frame-history compositing with conservative static alpha to avoid ghosting.
 */
public final class MotionBlurController {
    private static final float STATIC_BLEND = 0.0020F;
    private static final float MIN_MOTION_BLEND = 0.055F;
    private static final float MAX_MOTION_BLEND = 0.36F;
    private static final float MIN_BLEND_LERP = 0.02F;
    private static final float MAX_BLEND_LERP = 0.55F;
    private static final float DEFAULT_BLEND_LERP = 0.32F;

    private static final double YAW_RATE_WEIGHT = 0.0030D;
    private static final double PITCH_RATE_WEIGHT = 0.0024D;
    private static final double MOVE_SPEED_WEIGHT = 0.14D;
    private static final double MOTION_NORMALIZER = 0.75D;
    private static final double STATIC_THRESHOLD = 0.012D;

    private static final double TELEPORT_DISTANCE = 3.0D;
    private static final float TELEPORT_ROTATION = 80.0F;
    private static final long HISTORY_BREAK_NS = 500_000_000L;

    private static int historyTextureId = -1;
    private static int historyTextureWidth = -1;
    private static int historyTextureHeight = -1;
    private static boolean historyValid = false;
    private static float smoothedBlend = STATIC_BLEND;
    private static float frameBlendLerp = DEFAULT_BLEND_LERP;
    private static MotionSample lastSample;
    private static boolean bootstrapLogged = false;

    private static volatile Field rendererMcField;
    private static volatile Class<?> rendererMcFieldOwner;
    private static volatile Field currentScreenField;
    private static volatile Class<?> currentScreenFieldOwner;
    private static volatile Field worldField;
    private static volatile Class<?> worldFieldOwner;
    private static volatile Field playerField;
    private static volatile Class<?> playerFieldOwner;
    private static volatile Field displayWidthField;
    private static volatile Class<?> displayWidthFieldOwner;
    private static volatile Field displayHeightField;
    private static volatile Class<?> displayHeightFieldOwner;
    private static volatile Field playerPosXField;
    private static volatile Class<?> playerPosXFieldOwner;
    private static volatile Field playerPosYField;
    private static volatile Class<?> playerPosYFieldOwner;
    private static volatile Field playerPosZField;
    private static volatile Class<?> playerPosZFieldOwner;
    private static volatile Field playerYawField;
    private static volatile Class<?> playerYawFieldOwner;
    private static volatile Field playerPitchField;
    private static volatile Class<?> playerPitchFieldOwner;

    private MotionBlurController() {
    }

    public static void onRenderWorldTail(Object entityRenderer) {
        if (!ClientConfig.INSTANCE.isMotionBlurEnabled()) {
            resetHistoryState();
            return;
        }

        if (!bootstrapLogged) {
            bootstrapLogged = true;
            System.out.println("[ClientFN] Adaptive Motion Blur pipeline active.");
        }

        Object minecraft = getMinecraft(entityRenderer);
        if (minecraft == null || !shouldRenderInCurrentState(minecraft)) {
            resetHistoryState();
            return;
        }

        int width = readMinecraftIntField(
            minecraft,
            "field_71443_c",
            "displayWidth",
            displayWidthField,
            displayWidthFieldOwner
        );
        int height = readMinecraftIntField(
            minecraft,
            "field_71440_d",
            "displayHeight",
            displayHeightField,
            displayHeightFieldOwner
        );
        if (width <= 0 || height <= 0) {
            resetHistoryState();
            return;
        }

        if (!ensureHistoryTexture(width, height)) {
            resetHistoryState();
            return;
        }

        MotionSample sample = sampleMotion(minecraft);
        if (sample == null) {
            resetHistoryState();
            return;
        }

        float targetBlend = computeTargetBlend(sample);
        if (!historyValid) {
            smoothedBlend = STATIC_BLEND;
            captureFrame(width, height);
            historyValid = true;
            lastSample = sample;
            return;
        }

        smoothedBlend += (targetBlend - smoothedBlend) * frameBlendLerp;
        if (smoothedBlend > 0.001F) {
            drawHistoryOverlay(width, height, smoothedBlend);
        }

        captureFrame(width, height);
        lastSample = sample;
    }

    private static Object getMinecraft(Object entityRenderer) {
        if (entityRenderer == null) {
            return null;
        }
        try {
            Field field = rendererMcField;
            Class<?> owner = entityRenderer.getClass();
            if (field == null || rendererMcFieldOwner != owner) {
                field = resolveField(owner, "field_78531_r", "mc");
                rendererMcField = field;
                rendererMcFieldOwner = owner;
            }
            return field != null ? field.get(entityRenderer) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean shouldRenderInCurrentState(Object minecraft) {
        try {
            Class<?> minecraftClass = minecraft.getClass();

            Field world = worldField;
            if (world == null || worldFieldOwner != minecraftClass) {
                world = resolveField(minecraftClass, "field_71441_e", "theWorld");
                worldField = world;
                worldFieldOwner = minecraftClass;
            }
            if (world == null || world.get(minecraft) == null) {
                return false;
            }

            Field currentScreen = currentScreenField;
            if (currentScreen == null || currentScreenFieldOwner != minecraftClass) {
                currentScreen = resolveField(minecraftClass, "field_71462_r", "currentScreen");
                currentScreenField = currentScreen;
                currentScreenFieldOwner = minecraftClass;
            }
            return currentScreen == null || currentScreen.get(minecraft) == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static MotionSample sampleMotion(Object minecraft) {
        try {
            Class<?> minecraftClass = minecraft.getClass();

            Field player = playerField;
            if (player == null || playerFieldOwner != minecraftClass) {
                player = resolveField(minecraftClass, "field_71439_g", "thePlayer");
                playerField = player;
                playerFieldOwner = minecraftClass;
            }
            if (player == null) {
                return null;
            }

            Object localPlayer = player.get(minecraft);
            if (localPlayer == null) {
                return null;
            }

            double x = readPlayerDouble(localPlayer, "field_70165_t", "posX", true);
            double y = readPlayerDouble(localPlayer, "field_70163_u", "posY", false);
            double z = readPlayerDouble(localPlayer, "field_70161_v", "posZ", false);
            float yaw = readPlayerFloat(localPlayer, "field_70177_z", "rotationYaw", true);
            float pitch = readPlayerFloat(localPlayer, "field_70125_A", "rotationPitch", false);

            return new MotionSample(x, y, z, yaw, pitch, System.nanoTime());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double readPlayerDouble(Object player, String srgName, String mcpName, boolean xAxis)
        throws IllegalAccessException {
        Class<?> playerClass = player.getClass();
        Field field;
        if (xAxis) {
            field = playerPosXField;
            if (field == null || playerPosXFieldOwner != playerClass) {
                field = resolveField(playerClass, srgName, mcpName);
                playerPosXField = field;
                playerPosXFieldOwner = playerClass;
            }
        } else if ("field_70163_u".equals(srgName)) {
            field = playerPosYField;
            if (field == null || playerPosYFieldOwner != playerClass) {
                field = resolveField(playerClass, srgName, mcpName);
                playerPosYField = field;
                playerPosYFieldOwner = playerClass;
            }
        } else {
            field = playerPosZField;
            if (field == null || playerPosZFieldOwner != playerClass) {
                field = resolveField(playerClass, srgName, mcpName);
                playerPosZField = field;
                playerPosZFieldOwner = playerClass;
            }
        }

        if (field == null) {
            return 0.0D;
        }
        Object value = field.get(player);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    private static float readPlayerFloat(Object player, String srgName, String mcpName, boolean yawAxis)
        throws IllegalAccessException {
        Class<?> playerClass = player.getClass();
        Field field;
        if (yawAxis) {
            field = playerYawField;
            if (field == null || playerYawFieldOwner != playerClass) {
                field = resolveField(playerClass, srgName, mcpName);
                playerYawField = field;
                playerYawFieldOwner = playerClass;
            }
        } else {
            field = playerPitchField;
            if (field == null || playerPitchFieldOwner != playerClass) {
                field = resolveField(playerClass, srgName, mcpName);
                playerPitchField = field;
                playerPitchFieldOwner = playerClass;
            }
        }
        if (field == null) {
            return 0.0F;
        }
        Object value = field.get(player);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static float computeTargetBlend(MotionSample sample) {
        MotionSample previous = lastSample;
        if (previous == null) {
            frameBlendLerp = DEFAULT_BLEND_LERP;
            return STATIC_BLEND;
        }

        long elapsedNs = sample.timestampNs - previous.timestampNs;
        if (elapsedNs <= 0L || elapsedNs > HISTORY_BREAK_NS) {
            historyValid = false;
            frameBlendLerp = DEFAULT_BLEND_LERP;
            return STATIC_BLEND;
        }
        double elapsedSec = elapsedNs / 1_000_000_000.0D;
        if (elapsedSec <= 0.0D) {
            frameBlendLerp = DEFAULT_BLEND_LERP;
            return STATIC_BLEND;
        }

        double dx = sample.x - previous.x;
        double dy = sample.y - previous.y;
        double dz = sample.z - previous.z;
        double moveDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float yawDelta = Math.abs(wrapDegrees(sample.yaw - previous.yaw));
        float pitchDelta = Math.abs(sample.pitch - previous.pitch);

        if (moveDistance > TELEPORT_DISTANCE || yawDelta > TELEPORT_ROTATION || pitchDelta > TELEPORT_ROTATION) {
            historyValid = false;
            frameBlendLerp = DEFAULT_BLEND_LERP;
            return STATIC_BLEND;
        }

        frameBlendLerp = clamp((float) (1.0D - Math.exp(-elapsedSec * 18.0D)), MIN_BLEND_LERP, MAX_BLEND_LERP);

        double moveSpeed = clamp(moveDistance / elapsedSec, 0.0D, 20.0D);
        double yawRate = clamp(yawDelta / elapsedSec, 0.0D, 1080.0D);
        double pitchRate = clamp(pitchDelta / elapsedSec, 0.0D, 1080.0D);

        double motionScore = yawRate * YAW_RATE_WEIGHT + pitchRate * PITCH_RATE_WEIGHT + moveSpeed * MOVE_SPEED_WEIGHT;
        double normalized = clamp(motionScore / MOTION_NORMALIZER, 0.0D, 1.0D);
        if (normalized < STATIC_THRESHOLD) {
            return STATIC_BLEND;
        }

        return (float) (MIN_MOTION_BLEND + (MAX_MOTION_BLEND - MIN_MOTION_BLEND) * normalized);
    }

    private static boolean ensureHistoryTexture(int width, int height) {
        if (historyTextureId <= 0) {
            historyTextureId = GL11.glGenTextures();
            if (historyTextureId <= 0) {
                return false;
            }
            historyTextureWidth = -1;
            historyTextureHeight = -1;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextureId);
        if (historyTextureWidth != width || historyTextureHeight != height) {
            historyTextureWidth = width;
            historyTextureHeight = height;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
            );
            historyValid = false;
        }
        return true;
    }

    private static void drawHistoryOverlay(int width, int height, float alpha) {
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_TRANSFORM_BIT
                | GL11.GL_CURRENT_BIT
        );

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, (double) width, (double) height, 0.0D, -1.0D, 1.0D);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, clamp(alpha, 0.0F, 1.0F));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextureId);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(width, 0.0F);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(width, height);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, height);
        GL11.glEnd();

        GL11.glDepthMask(true);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static void captureFrame(int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextureId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        historyValid = true;
    }

    private static int readMinecraftIntField(
        Object minecraft,
        String srgName,
        String mcpName,
        Field cachedField,
        Class<?> cachedOwner
    ) {
        try {
            Class<?> minecraftClass = minecraft.getClass();
            Field field = cachedField;
            if (field == null || cachedOwner != minecraftClass) {
                field = resolveField(minecraftClass, srgName, mcpName);
                if ("field_71443_c".equals(srgName)) {
                    displayWidthField = field;
                    displayWidthFieldOwner = minecraftClass;
                } else if ("field_71440_d".equals(srgName)) {
                    displayHeightField = field;
                    displayHeightFieldOwner = minecraftClass;
                }
            }
            if (field == null) {
                return -1;
            }
            Object value = field.get(minecraft);
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void resetHistoryState() {
        historyValid = false;
        smoothedBlend = STATIC_BLEND;
        frameBlendLerp = DEFAULT_BLEND_LERP;
        lastSample = null;
    }

    private static Field resolveField(Class<?> owner, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // Try next candidate.
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private static float wrapDegrees(float value) {
        while (value <= -180.0F) {
            value += 360.0F;
        }
        while (value > 180.0F) {
            value -= 360.0F;
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }

    private static final class MotionSample {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final long timestampNs;

        private MotionSample(double x, double y, double z, float yaw, float pitch, long timestampNs) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestampNs = timestampNs;
        }
    }
}
