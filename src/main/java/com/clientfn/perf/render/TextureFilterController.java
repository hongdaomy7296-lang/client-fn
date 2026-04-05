package com.clientfn.perf.render;

import com.clientfn.perf.lighting.LightingController;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * Forces nearest filtering and disables anisotropic filtering for the active 2D texture.
 */
public final class TextureFilterController {
    private static int lastTextureId = Integer.MIN_VALUE;

    private TextureFilterController() {
    }

    public static void invalidate() {
        lastTextureId = Integer.MIN_VALUE;
    }

    public static void enforceNearestNoAf() {
        if (!LightingController.isMipmapDisabled()) {
            return;
        }

        int textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (textureId <= 0 || textureId == lastTextureId) {
            return;
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            GL11.glTexParameterf(
                GL11.GL_TEXTURE_2D,
                EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                1.0F
            );
        }
        lastTextureId = textureId;
    }
}
