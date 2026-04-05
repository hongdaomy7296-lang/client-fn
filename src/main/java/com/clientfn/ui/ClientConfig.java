package com.clientfn.ui;

import com.clientfn.core.MinecraftCompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persistent client configuration backed by JSON.
 */
public final class ClientConfig implements IClientConfig {
    public static final ClientConfig INSTANCE = new ClientConfig();

    private static final int CONFIG_VERSION = 1;
    private static final String CONFIG_FILE_NAME = "clientfn_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final float DEFAULT_FOG_START = 0.0F;
    private static final float DEFAULT_FOG_END = 16.0F;
    private static final int DEFAULT_FREELOOK_KEY = Keyboard.KEY_V;

    private boolean particlesDisabled = false;
    private boolean cloudsDisabled = false;
    private boolean entityShadowsDisabled = false;
    private boolean animationsDisabled = false;
    private boolean smoothLightingDisabled = false;
    private boolean dynamicLightingDisabled = false;
    private boolean dynamicFovDisabled = true;
    private boolean motionBlurEnabled = true;
    private boolean forceSprintEnabled = false;
    private boolean permanentNightVisionEnabled = false;
    private boolean mipmapDisabled = false;
    private boolean hudChatHidden = false;
    private boolean hudScoreboardHidden = false;
    private boolean fogEnabled = false;
    private float fogStart = DEFAULT_FOG_START;
    private float fogEnd = DEFAULT_FOG_END;
    private int freelookKey = DEFAULT_FREELOOK_KEY;

    private ClientConfig() {
    }

    public synchronized void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (!parsed.isJsonObject()) {
                resetToDefaults();
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            int version = getInt(root, "configVersion", 0);
            if (version <= 0 || version > CONFIG_VERSION) {
                resetToDefaults();
                return;
            }

            readVersion1(root);
            normalizeRanges();
        } catch (Exception ex) {
            resetToDefaults();
        }
    }

    public synchronized void save() {
        Path path = getConfigPath();
        JsonObject root = new JsonObject();
        root.addProperty("configVersion", CONFIG_VERSION);

        JsonObject particles = new JsonObject();
        particles.addProperty("disabled", this.particlesDisabled);
        root.add("particles", particles);

        JsonObject clouds = new JsonObject();
        clouds.addProperty("disabled", this.cloudsDisabled);
        root.add("clouds", clouds);

        JsonObject entityShadows = new JsonObject();
        entityShadows.addProperty("disabled", this.entityShadowsDisabled);
        root.add("entityShadows", entityShadows);

        JsonObject animations = new JsonObject();
        animations.addProperty("disabled", this.animationsDisabled);
        root.add("animations", animations);

        JsonObject smoothLighting = new JsonObject();
        smoothLighting.addProperty("disabled", this.smoothLightingDisabled);
        root.add("smoothLighting", smoothLighting);

        JsonObject dynamicLighting = new JsonObject();
        dynamicLighting.addProperty("disabled", this.dynamicLightingDisabled);
        root.add("dynamicLighting", dynamicLighting);

        JsonObject dynamicFov = new JsonObject();
        dynamicFov.addProperty("disabled", this.dynamicFovDisabled);
        root.add("dynamicFov", dynamicFov);

        JsonObject motionBlur = new JsonObject();
        motionBlur.addProperty("enabled", this.motionBlurEnabled);
        root.add("motionBlur", motionBlur);

        JsonObject forceSprint = new JsonObject();
        forceSprint.addProperty("enabled", this.forceSprintEnabled);
        root.add("forceSprint", forceSprint);

        JsonObject permanentNightVision = new JsonObject();
        permanentNightVision.addProperty("enabled", this.permanentNightVisionEnabled);
        root.add("permanentNightVision", permanentNightVision);

        JsonObject mipmap = new JsonObject();
        mipmap.addProperty("disabled", this.mipmapDisabled);
        root.add("mipmap", mipmap);

        JsonObject hud = new JsonObject();
        hud.addProperty("chatHidden", this.hudChatHidden);
        hud.addProperty("scoreboardHidden", this.hudScoreboardHidden);
        root.add("hud", hud);

        JsonObject fog = new JsonObject();
        fog.addProperty("enabled", this.fogEnabled);
        fog.addProperty("start", this.fogStart);
        fog.addProperty("end", this.fogEnd);
        root.add("fog", fog);

        JsonObject freelook = new JsonObject();
        freelook.addProperty("key", this.freelookKey);
        root.add("freelook", freelook);

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
            // Keep runtime config active even if filesystem write fails.
        }
    }

    @Override
    public synchronized void resetToDefaults() {
        this.particlesDisabled = false;
        this.cloudsDisabled = false;
        this.entityShadowsDisabled = false;
        this.animationsDisabled = false;
        this.smoothLightingDisabled = false;
        this.dynamicLightingDisabled = false;
        this.dynamicFovDisabled = true;
        this.motionBlurEnabled = true;
        this.forceSprintEnabled = false;
        this.permanentNightVisionEnabled = false;
        this.mipmapDisabled = false;
        this.hudChatHidden = false;
        this.hudScoreboardHidden = false;
        this.fogEnabled = false;
        this.fogStart = DEFAULT_FOG_START;
        this.fogEnd = DEFAULT_FOG_END;
        this.freelookKey = DEFAULT_FREELOOK_KEY;
        save();
    }

    @Override
    public synchronized boolean isEnabled(String key) {
        if (key == null) {
            return false;
        }

        if ("particles.disabled".equals(key) || "particles".equals(key)) {
            return this.particlesDisabled;
        }
        if ("clouds.disabled".equals(key) || "clouds".equals(key)) {
            return this.cloudsDisabled;
        }
        if ("entityShadows.disabled".equals(key) || "entity_shadows.disabled".equals(key) || "entityShadows".equals(key)) {
            return this.entityShadowsDisabled;
        }
        if ("animations.disabled".equals(key) || "animations".equals(key)) {
            return this.animationsDisabled;
        }
        if ("smoothLighting.disabled".equals(key) || "smooth_lighting.disabled".equals(key) || "smoothLighting".equals(key)) {
            return this.smoothLightingDisabled;
        }
        if ("dynamicLighting.disabled".equals(key) || "dynamic_lighting.disabled".equals(key) || "dynamicLighting".equals(key)) {
            return this.dynamicLightingDisabled;
        }
        if ("dynamicFov.disabled".equals(key) || "dynamic_fov.disabled".equals(key) || "dynamicFov".equals(key)) {
            return this.dynamicFovDisabled;
        }
        if ("motionBlur.enabled".equals(key) || "motion_blur.enabled".equals(key) || "motionBlur".equals(key)) {
            return this.motionBlurEnabled;
        }
        if ("forceSprint.enabled".equals(key) || "force_sprint.enabled".equals(key) || "forceSprint".equals(key)) {
            return this.forceSprintEnabled;
        }
        if ("permanentNightVision.enabled".equals(key) || "permanent_night_vision.enabled".equals(key) || "permanentNightVision".equals(key)) {
            return this.permanentNightVisionEnabled;
        }
        if ("mipmap.disabled".equals(key) || "mipmap".equals(key)) {
            return this.mipmapDisabled;
        }
        if ("hud.chatHidden".equals(key) || "hud.chat.hidden".equals(key)) {
            return this.hudChatHidden;
        }
        if ("hud.scoreboardHidden".equals(key) || "hud.scoreboard.hidden".equals(key)) {
            return this.hudScoreboardHidden;
        }
        if ("fog.enabled".equals(key)) {
            return this.fogEnabled;
        }
        return false;
    }

    @Override
    public synchronized float getFloat(String key, float defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        if ("fog.start".equals(key)) {
            return this.fogStart;
        }
        if ("fog.end".equals(key)) {
            return this.fogEnd;
        }
        if ("freelook.key".equals(key)) {
            return this.freelookKey;
        }
        return defaultValue;
    }

    public synchronized boolean isParticlesDisabled() {
        return this.particlesDisabled;
    }

    public synchronized void setParticlesDisabled(boolean particlesDisabled) {
        this.particlesDisabled = particlesDisabled;
    }

    public synchronized boolean isCloudsDisabled() {
        return this.cloudsDisabled;
    }

    public synchronized void setCloudsDisabled(boolean cloudsDisabled) {
        this.cloudsDisabled = cloudsDisabled;
    }

    public synchronized boolean isEntityShadowsDisabled() {
        return this.entityShadowsDisabled;
    }

    public synchronized void setEntityShadowsDisabled(boolean entityShadowsDisabled) {
        this.entityShadowsDisabled = entityShadowsDisabled;
    }

    public synchronized boolean isAnimationsDisabled() {
        return this.animationsDisabled;
    }

    public synchronized void setAnimationsDisabled(boolean animationsDisabled) {
        this.animationsDisabled = animationsDisabled;
    }

    public synchronized boolean isSmoothLightingDisabled() {
        return this.smoothLightingDisabled;
    }

    public synchronized void setSmoothLightingDisabled(boolean smoothLightingDisabled) {
        this.smoothLightingDisabled = smoothLightingDisabled;
    }

    public synchronized boolean isDynamicLightingDisabled() {
        return this.dynamicLightingDisabled;
    }

    public synchronized void setDynamicLightingDisabled(boolean dynamicLightingDisabled) {
        this.dynamicLightingDisabled = dynamicLightingDisabled;
    }

    public synchronized boolean isMipmapDisabled() {
        return this.mipmapDisabled;
    }

    public synchronized boolean isForceSprintEnabled() {
        return this.forceSprintEnabled;
    }

    public synchronized void setForceSprintEnabled(boolean forceSprintEnabled) {
        this.forceSprintEnabled = forceSprintEnabled;
    }

    public synchronized boolean isPermanentNightVisionEnabled() {
        return this.permanentNightVisionEnabled;
    }

    public synchronized void setPermanentNightVisionEnabled(boolean permanentNightVisionEnabled) {
        this.permanentNightVisionEnabled = permanentNightVisionEnabled;
    }

    public synchronized boolean isDynamicFovDisabled() {
        return this.dynamicFovDisabled;
    }

    public synchronized void setDynamicFovDisabled(boolean dynamicFovDisabled) {
        this.dynamicFovDisabled = dynamicFovDisabled;
    }

    public synchronized boolean isMotionBlurEnabled() {
        return this.motionBlurEnabled;
    }

    public synchronized void setMotionBlurEnabled(boolean motionBlurEnabled) {
        this.motionBlurEnabled = motionBlurEnabled;
    }

    public synchronized void setMipmapDisabled(boolean mipmapDisabled) {
        this.mipmapDisabled = mipmapDisabled;
    }

    public synchronized boolean isHudChatHidden() {
        return this.hudChatHidden;
    }

    public synchronized void setHudChatHidden(boolean hudChatHidden) {
        this.hudChatHidden = hudChatHidden;
    }

    public synchronized boolean isHudScoreboardHidden() {
        return this.hudScoreboardHidden;
    }

    public synchronized void setHudScoreboardHidden(boolean hudScoreboardHidden) {
        this.hudScoreboardHidden = hudScoreboardHidden;
    }

    public synchronized boolean isFogEnabled() {
        return this.fogEnabled;
    }

    public synchronized void setFogEnabled(boolean fogEnabled) {
        this.fogEnabled = fogEnabled;
    }

    public synchronized float getFogStart() {
        return this.fogStart;
    }

    public synchronized void setFogStart(float fogStart) {
        this.fogStart = clamp(fogStart, 0.0F, 512.0F);
        if (this.fogStart > this.fogEnd) {
            this.fogEnd = this.fogStart;
        }
    }

    public synchronized float getFogEnd() {
        return this.fogEnd;
    }

    public synchronized void setFogEnd(float fogEnd) {
        this.fogEnd = clamp(fogEnd, 0.5F, 512.0F);
        if (this.fogEnd < this.fogStart) {
            this.fogStart = this.fogEnd;
        }
    }

    public synchronized int getFreelookKey() {
        return this.freelookKey;
    }

    public synchronized void setFreelookKey(int freelookKey) {
        if (freelookKey > 0) {
            this.freelookKey = freelookKey;
        }
    }

    private void readVersion1(JsonObject root) {
        this.particlesDisabled = getNestedBoolean(root, "particles", "disabled", this.particlesDisabled);
        this.cloudsDisabled = getNestedBoolean(root, "clouds", "disabled", this.cloudsDisabled);
        this.entityShadowsDisabled = getNestedBoolean(root, "entityShadows", "disabled", this.entityShadowsDisabled);
        this.animationsDisabled = getNestedBoolean(root, "animations", "disabled", this.animationsDisabled);
        this.smoothLightingDisabled = getNestedBoolean(root, "smoothLighting", "disabled", this.smoothLightingDisabled);
        this.dynamicLightingDisabled = getNestedBoolean(root, "dynamicLighting", "disabled", this.dynamicLightingDisabled);
        this.dynamicFovDisabled = getNestedBoolean(root, "dynamicFov", "disabled", this.dynamicFovDisabled);
        this.motionBlurEnabled = getNestedBoolean(root, "motionBlur", "enabled", this.motionBlurEnabled);
        this.forceSprintEnabled = getNestedBoolean(root, "forceSprint", "enabled", this.forceSprintEnabled);
        this.permanentNightVisionEnabled = getNestedBoolean(root, "permanentNightVision", "enabled", this.permanentNightVisionEnabled);
        this.mipmapDisabled = getNestedBoolean(root, "mipmap", "disabled", this.mipmapDisabled);
        this.hudChatHidden = getNestedBoolean(root, "hud", "chatHidden", this.hudChatHidden);
        this.hudScoreboardHidden = getNestedBoolean(root, "hud", "scoreboardHidden", this.hudScoreboardHidden);
        this.fogEnabled = getNestedBoolean(root, "fog", "enabled", this.fogEnabled);
        this.fogStart = getNestedFloat(root, "fog", "start", this.fogStart);
        this.fogEnd = getNestedFloat(root, "fog", "end", this.fogEnd);
        this.freelookKey = getNestedInt(root, "freelook", "key", this.freelookKey);
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static boolean getNestedBoolean(JsonObject root, String section, String key, boolean defaultValue) {
        JsonObject obj = getSection(root, section);
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static int getNestedInt(JsonObject root, String section, String key, int defaultValue) {
        JsonObject obj = getSection(root, section);
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static float getNestedFloat(JsonObject root, String section, String key, float defaultValue) {
        JsonObject obj = getSection(root, section);
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static JsonObject getSection(JsonObject root, String section) {
        if (root == null || !root.has(section) || !root.get(section).isJsonObject()) {
            return null;
        }
        return root.getAsJsonObject(section);
    }

    private void normalizeRanges() {
        this.fogStart = clamp(this.fogStart, 0.0F, 512.0F);
        this.fogEnd = clamp(this.fogEnd, 0.5F, 512.0F);
        if (this.fogEnd < this.fogStart) {
            this.fogEnd = this.fogStart;
        }
        if (this.freelookKey <= 0) {
            this.freelookKey = DEFAULT_FREELOOK_KEY;
        }
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }

    private static Path getConfigPath() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        File gameDir = MinecraftCompat.getMcDataDir(mc);
        if (gameDir != null) {
            return Paths.get(gameDir.toString(), CONFIG_FILE_NAME);
        }
        return Paths.get(CONFIG_FILE_NAME);
    }

    public synchronized File resolveConfigDirectory() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        File gameDir = MinecraftCompat.getMcDataDir(mc);
        return gameDir != null ? gameDir : new File(".");
    }
}
