package com.clientfn.mixin.core;

import com.clientfn.fog.FogEditor;
import com.clientfn.freelook.FreelookHandler;
import com.clientfn.perf.render.MotionBlurController;
import com.clientfn.ui.ClientConfig;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Fog override injection. Base-game fog logic remains intact until RETURN.
 */
@Mixin(targets = "net.minecraft.client.renderer.EntityRenderer")
public abstract class MixinEntityRenderer {

    @Unique
    private static volatile Field clientfn$entityRendererMinecraftField;
    @Unique
    private static volatile Class<?> clientfn$entityRendererMinecraftFieldOwner;
    @Unique
    private static volatile Field clientfn$gameSettingsField;
    @Unique
    private static volatile Class<?> clientfn$gameSettingsFieldOwner;
    @Unique
    private static volatile Field clientfn$fovSettingField;
    @Unique
    private static volatile Class<?> clientfn$fovSettingFieldOwner;
    @Unique
    private static volatile Field clientfn$displayWidthField;
    @Unique
    private static volatile Class<?> clientfn$displayWidthFieldOwner;
    @Unique
    private static volatile Field clientfn$displayHeightField;
    @Unique
    private static volatile Class<?> clientfn$displayHeightFieldOwner;
    @Unique
    private static volatile Field clientfn$currentScreenField;
    @Unique
    private static volatile Class<?> clientfn$currentScreenFieldOwner;
    @Unique
    private static volatile Field clientfn$worldField;
    @Unique
    private static volatile Class<?> clientfn$worldFieldOwner;

    @Inject(method = {"func_78468_a(IF)V", "setupFog(IF)V"}, at = @At("RETURN"), require = 0)
    private void clientfn$overrideFog(int startCoords, float partialTicks, CallbackInfo ci) {
        FogEditor editor = FogEditor.INSTANCE;
        if (!editor.isEnabled()) {
            return;
        }

        // Force linear fog so override also works for underwater/cloud/lava paths.
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, editor.getFogStart());
        GL11.glFogf(GL11.GL_FOG_END, editor.getFogEnd());
    }

    /**
     * 1.7.10 updateLightmap always takes (float partialTicks).
     * SRG: func_78472_g(F)V  |  MCP: updateLightmap(F)V
     * The no-arg variant does NOT exist in 1.7.10 — removed.
     */
    @Inject(method = {"func_78472_g(F)V", "updateLightmap(F)V"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$skipLightmapUpdate(float partialTicks, CallbackInfo ci) {
        // Disabled for stability in SRG runtime: skipping vanilla lightmap updates can black out world rendering.
    }

    @Inject(method = {"func_78467_g(F)V", "orientCamera(F)V"}, at = @At("TAIL"), require = 0)
    private void clientfn$applyFreelookOffset(float partialTicks, CallbackInfo ci) {
        if (!FreelookHandler.isActive()) {
            return;
        }
        GL11.glRotatef(FreelookHandler.getPitchOffset(), 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(FreelookHandler.getYawOffset(), 0.0F, 1.0F, 0.0F);
    }

    @Inject(method = {"func_78481_a(FZ)F", "getFOVModifier(FZ)F"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$disableDynamicFov(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!ClientConfig.INSTANCE.isDynamicFovDisabled()) {
            return;
        }

        float baseFov = 70.0F;
        if (useFovSetting) {
            float fovSetting = clientfn$readFovSetting(this);
            baseFov = fovSetting * 40.0F + 70.0F;
        }
        cir.setReturnValue(baseFov);
    }

    @Inject(method = {"func_78480_b(F)V", "updateCameraAndRender(F)V"}, at = @At("TAIL"), require = 0)
    private void clientfn$applyAdaptiveMotionBlurAtFrameTail(float partialTicks, CallbackInfo ci) {
        MotionBlurController.onRenderWorldTail(this);
    }

    @Unique
    private static float clientfn$readFovSetting(Object entityRenderer) {
        try {
            if (entityRenderer == null) {
                return 0.0F;
            }

            Field minecraftField = clientfn$entityRendererMinecraftField;
            Class<?> rendererClass = entityRenderer.getClass();
            if (minecraftField == null || clientfn$entityRendererMinecraftFieldOwner != rendererClass) {
                minecraftField = resolveField(rendererClass, "field_78531_r", "mc");
                clientfn$entityRendererMinecraftField = minecraftField;
                clientfn$entityRendererMinecraftFieldOwner = rendererClass;
            }
            if (minecraftField == null) {
                return 0.0F;
            }

            Object minecraft = minecraftField.get(entityRenderer);
            if (minecraft == null) {
                return 0.0F;
            }

            Field gameSettingsField = clientfn$gameSettingsField;
            Class<?> minecraftClass = minecraft.getClass();
            if (gameSettingsField == null || clientfn$gameSettingsFieldOwner != minecraftClass) {
                gameSettingsField = resolveField(minecraftClass, "field_71474_y", "gameSettings");
                clientfn$gameSettingsField = gameSettingsField;
                clientfn$gameSettingsFieldOwner = minecraftClass;
            }
            if (gameSettingsField == null) {
                return 0.0F;
            }

            Object gameSettings = gameSettingsField.get(minecraft);
            if (gameSettings == null) {
                return 0.0F;
            }

            Field fovSettingField = clientfn$fovSettingField;
            Class<?> gameSettingsClass = gameSettings.getClass();
            if (fovSettingField == null || clientfn$fovSettingFieldOwner != gameSettingsClass) {
                fovSettingField = resolveField(gameSettingsClass, "field_74334_X", "fovSetting");
                clientfn$fovSettingField = fovSettingField;
                clientfn$fovSettingFieldOwner = gameSettingsClass;
            }
            if (fovSettingField == null) {
                return 0.0F;
            }

            Object value = fovSettingField.get(gameSettings);

            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (Exception ignored) {
            return 0.0F;
        }
    }

    @Unique
    private static void clientfn$drawFrameEndCrosshair(Object entityRenderer) {
        try {
            Object minecraft = clientfn$getMinecraft(entityRenderer);
            if (minecraft == null || !clientfn$shouldDrawFrameEndCrosshair(minecraft)) {
                return;
            }

            int width = clientfn$readMinecraftIntField(
                minecraft,
                "field_71443_c",
                "displayWidth",
                clientfn$displayWidthField,
                clientfn$displayWidthFieldOwner
            );
            if (width <= 0) {
                return;
            }
            if (clientfn$displayWidthField == null || clientfn$displayWidthFieldOwner != minecraft.getClass()) {
                clientfn$displayWidthField = resolveField(minecraft.getClass(), "field_71443_c", "displayWidth");
                clientfn$displayWidthFieldOwner = minecraft.getClass();
            }

            int height = clientfn$readMinecraftIntField(
                minecraft,
                "field_71440_d",
                "displayHeight",
                clientfn$displayHeightField,
                clientfn$displayHeightFieldOwner
            );
            if (height <= 0) {
                return;
            }
            if (clientfn$displayHeightField == null || clientfn$displayHeightFieldOwner != minecraft.getClass()) {
                clientfn$displayHeightField = resolveField(minecraft.getClass(), "field_71440_d", "displayHeight");
                clientfn$displayHeightFieldOwner = minecraft.getClass();
            }

            clientfn$drawCrosshairPixels(width, height);
        } catch (Throwable ignored) {
            // Rendering fallback is best-effort only.
        }
    }

    @Unique
    private static Object clientfn$getMinecraft(Object entityRenderer) throws IllegalAccessException {
        Field minecraftField = clientfn$entityRendererMinecraftField;
        Class<?> rendererClass = entityRenderer.getClass();
        if (minecraftField == null || clientfn$entityRendererMinecraftFieldOwner != rendererClass) {
            minecraftField = resolveField(rendererClass, "field_78531_r", "mc");
            clientfn$entityRendererMinecraftField = minecraftField;
            clientfn$entityRendererMinecraftFieldOwner = rendererClass;
        }
        return minecraftField != null ? minecraftField.get(entityRenderer) : null;
    }

    @Unique
    private static boolean clientfn$shouldDrawFrameEndCrosshair(Object minecraft) throws IllegalAccessException {
        Class<?> minecraftClass = minecraft.getClass();

        Field currentScreenField = clientfn$currentScreenField;
        if (currentScreenField == null || clientfn$currentScreenFieldOwner != minecraftClass) {
            currentScreenField = resolveField(minecraftClass, "field_71462_r", "currentScreen");
            clientfn$currentScreenField = currentScreenField;
            clientfn$currentScreenFieldOwner = minecraftClass;
        }
        if (currentScreenField != null && currentScreenField.get(minecraft) != null) {
            return false;
        }

        Field worldField = clientfn$worldField;
        if (worldField == null || clientfn$worldFieldOwner != minecraftClass) {
            worldField = resolveField(minecraftClass, "field_71441_e", "theWorld");
            clientfn$worldField = worldField;
            clientfn$worldFieldOwner = minecraftClass;
        }
        return worldField == null || worldField.get(minecraft) != null;
    }

    @Unique
    private static int clientfn$readMinecraftIntField(
        Object minecraft,
        String srgName,
        String mcpName,
        Field cachedField,
        Class<?> cachedOwner
    ) throws IllegalAccessException {
        Class<?> minecraftClass = minecraft.getClass();
        Field field = cachedField;
        if (field == null || cachedOwner != minecraftClass) {
            field = resolveField(minecraftClass, srgName, mcpName);
        }
        if (field == null) {
            return -1;
        }
        Object value = field.get(minecraft);
        return value instanceof Number ? ((Number) value).intValue() : -1;
    }

    @Unique
    private static void clientfn$drawCrosshairPixels(int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_TRANSFORM_BIT
                | GL11.GL_CURRENT_BIT
        );

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, (double) screenWidth, (double) screenHeight, 0.0D, -1.0D, 1.0D);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Outline
        clientfn$fillRect(cx - 5, cy - 1, cx + 6, cy + 2, 0xAA000000);
        clientfn$fillRect(cx - 1, cy - 5, cx + 2, cy + 6, 0xAA000000);

        // Inner white lines
        clientfn$fillRect(cx - 4, cy, cx + 5, cy + 1, 0xFFFFFFFF);
        clientfn$fillRect(cx, cy - 4, cx + 1, cy + 5, 0xFFFFFFFF);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    @Unique
    private static void clientfn$fillRect(int left, int top, int right, int bottom, int color) {
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f((float) left, (float) bottom);
        GL11.glVertex2f((float) right, (float) bottom);
        GL11.glVertex2f((float) right, (float) top);
        GL11.glVertex2f((float) left, (float) top);
        GL11.glEnd();
    }

    @Unique
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
}
