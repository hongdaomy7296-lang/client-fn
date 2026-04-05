package com.clientfn.ui;

import com.clientfn.core.GuiDrawCompat;
import com.clientfn.fog.FogEditor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Minimal Anthropic-style settings screen.
 */
public class ClientFnSettingsScreen extends GuiScreen {
    private static final int COLOR_BG = 0xFF1A1A1A;
    private static final int COLOR_TEXT = 0xFFE8E8E0;
    private static final int COLOR_DIVIDER = 0xFF3A3A3A;
    private static final int COLOR_ACCENT = 0xFFD4A853;
    private static final int COLOR_HINT = 0xFF888880;

    private static final int ID_PARTICLES = 100;
    private static final int ID_CLOUDS = 101;
    private static final int ID_ENTITY_SHADOWS = 102;
    private static final int ID_ANIMATIONS = 103;
    private static final int ID_SMOOTH_LIGHTING = 104;
    private static final int ID_CHAT = 105;
    private static final int ID_SCOREBOARD = 106;
    private static final int ID_FOG_ENABLE = 107;
    private static final int ID_FREELOOK_KEY = 108;
    private static final int ID_DONE = 109;
    private static final int ID_FOG_START = 110;
    private static final int ID_FOG_END = 111;
    private static final int ID_DYNAMIC_FOV = 112;
    private static final int ID_FORCE_SPRINT = 113;
    private static final int ID_PERMANENT_NIGHT_VISION = 114;
    private static final int ID_MOTION_BLUR = 115;

    private final GuiScreen parent;
    private final ClientConfig config = ClientConfig.INSTANCE;

    private CfnToggleButton particlesBtn;
    private CfnToggleButton cloudsBtn;
    private CfnToggleButton entityShadowsBtn;
    private CfnToggleButton animationsBtn;
    private CfnToggleButton smoothLightingBtn;
    private CfnToggleButton dynamicFovBtn;
    private CfnToggleButton motionBlurBtn;
    private CfnToggleButton forceSprintBtn;
    private CfnToggleButton permanentNightVisionBtn;
    private CfnToggleButton chatBtn;
    private CfnToggleButton scoreboardBtn;
    private CfnToggleButton fogEnableBtn;
    private CfnActionButton freelookBtn;
    private CfnActionButton doneBtn;
    private ClientFnSlider fogStartSlider;
    private ClientFnSlider fogEndSlider;

    private boolean waitingFreelookKey;
    private boolean saved;

    public ClientFnSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int left = this.width / 2 - 170;
        int y = this.height / 2 - 118;

        y += 32;
        this.particlesBtn = addToggle(ID_PARTICLES, left, y, "Hide Particles", this.config.isParticlesDisabled());
        y += 22;
        this.cloudsBtn = addToggle(ID_CLOUDS, left, y, "Hide Clouds", this.config.isCloudsDisabled());
        y += 22;
        this.entityShadowsBtn = addToggle(ID_ENTITY_SHADOWS, left, y, "Hide Entity Shadows", this.config.isEntityShadowsDisabled());
        y += 22;
        this.animationsBtn = addToggle(ID_ANIMATIONS, left, y, "Disable Animations", this.config.isAnimationsDisabled());
        y += 22;
        this.smoothLightingBtn = addToggle(ID_SMOOTH_LIGHTING, left, y, "Disable Smooth Lighting", this.config.isSmoothLightingDisabled());
        y += 22;
        this.dynamicFovBtn = addToggle(ID_DYNAMIC_FOV, left, y, "Disable Dynamic FOV", this.config.isDynamicFovDisabled());
        y += 22;
        this.motionBlurBtn = addToggle(ID_MOTION_BLUR, left, y, "Dynamic Motion Blur", this.config.isMotionBlurEnabled());
        y += 22;
        this.forceSprintBtn = addToggle(ID_FORCE_SPRINT, left, y, "Force Sprint", this.config.isForceSprintEnabled());
        y += 22;
        this.permanentNightVisionBtn = addToggle(ID_PERMANENT_NIGHT_VISION, left, y, "Permanent Night Vision", this.config.isPermanentNightVisionEnabled());
        y += 34;

        this.chatBtn = addToggle(ID_CHAT, left, y, "Hide Chat", this.config.isHudChatHidden());
        y += 22;
        this.scoreboardBtn = addToggle(ID_SCOREBOARD, left, y, "Hide Scoreboard", this.config.isHudScoreboardHidden());
        y += 34;

        this.fogEnableBtn = addToggle(ID_FOG_ENABLE, left, y, "Enable Fog Override", this.config.isFogEnabled());
        y += 22;

        this.fogStartSlider = new ClientFnSlider(
            ID_FOG_START, left, y, 260, "Fog Start", 0.0F, 512.0F, this.config.getFogStart(),
            new ClientFnSlider.OnValueChanged() {
                @Override
                public void onChanged(float value) {
                    config.setFogStart(value);
                    if (config.getFogEnd() < config.getFogStart()) {
                        config.setFogEnd(config.getFogStart());
                        if (fogEndSlider != null) {
                            fogEndSlider.setValue(config.getFogEnd(), false);
                        }
                    }
                    FogEditor.INSTANCE.setFogStart(config.getFogStart());
                }
            });
        this.buttonList.add(this.fogStartSlider);
        y += 24;

        this.fogEndSlider = new ClientFnSlider(
            ID_FOG_END, left, y, 260, "Fog End", 0.5F, 512.0F, this.config.getFogEnd(),
            new ClientFnSlider.OnValueChanged() {
                @Override
                public void onChanged(float value) {
                    config.setFogEnd(value);
                    if (config.getFogStart() > config.getFogEnd()) {
                        config.setFogStart(config.getFogEnd());
                        if (fogStartSlider != null) {
                            fogStartSlider.setValue(config.getFogStart(), false);
                        }
                    }
                    FogEditor.INSTANCE.setFogEnd(config.getFogEnd());
                }
            });
        this.buttonList.add(this.fogEndSlider);
        y += 30;

        this.freelookBtn = new CfnActionButton(ID_FREELOOK_KEY, left, y, 180, 20, freelookLabel());
        this.buttonList.add(this.freelookBtn);
        y += 40;

        this.doneBtn = new CfnActionButton(ID_DONE, this.width / 2 - 45, y, 90, 20, "Done");
        this.buttonList.add(this.doneBtn);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
            case ID_PARTICLES:
                this.config.setParticlesDisabled(toggle(this.particlesBtn));
                break;
            case ID_CLOUDS:
                this.config.setCloudsDisabled(toggle(this.cloudsBtn));
                break;
            case ID_ENTITY_SHADOWS:
                this.config.setEntityShadowsDisabled(toggle(this.entityShadowsBtn));
                break;
            case ID_ANIMATIONS:
                this.config.setAnimationsDisabled(toggle(this.animationsBtn));
                break;
            case ID_SMOOTH_LIGHTING:
                this.config.setSmoothLightingDisabled(toggle(this.smoothLightingBtn));
                break;
            case ID_DYNAMIC_FOV:
                this.config.setDynamicFovDisabled(toggle(this.dynamicFovBtn));
                break;
            case ID_MOTION_BLUR:
                this.config.setMotionBlurEnabled(toggle(this.motionBlurBtn));
                break;
            case ID_FORCE_SPRINT:
                this.config.setForceSprintEnabled(toggle(this.forceSprintBtn));
                break;
            case ID_PERMANENT_NIGHT_VISION:
                this.config.setPermanentNightVisionEnabled(toggle(this.permanentNightVisionBtn));
                break;
            case ID_CHAT:
                this.config.setHudChatHidden(toggle(this.chatBtn));
                break;
            case ID_SCOREBOARD:
                this.config.setHudScoreboardHidden(toggle(this.scoreboardBtn));
                break;
            case ID_FOG_ENABLE:
                this.config.setFogEnabled(toggle(this.fogEnableBtn));
                FogEditor.INSTANCE.setEnabled(this.config.isFogEnabled());
                break;
            case ID_FREELOOK_KEY:
                this.waitingFreelookKey = true;
                refreshFreelookLabel();
                break;
            case ID_DONE:
                closeAndSave();
                this.mc.displayGuiScreen(this.parent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.waitingFreelookKey) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                this.config.setFreelookKey(keyCode);
            }
            this.waitingFreelookKey = false;
            refreshFreelookLabel();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeAndSave();
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        closeAndSave();
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiDrawCompat.drawRect(0, 0, this.width, this.height, COLOR_BG);

        int left = this.width / 2 - 170;
        int right = this.width / 2 + 170;
        int top = this.height / 2 - 132;

        drawText("ClientFN Settings", left, top, COLOR_TEXT);
        GuiDrawCompat.drawRect(left, top + 14, right, top + 15, COLOR_DIVIDER);

        drawText("Performance", left, top + 24, COLOR_HINT);
        drawText("HUD", left, top + 146, COLOR_HINT);
        drawText("Fog Editor", left, top + 202, COLOR_HINT);

        drawText("Press K to reset all to defaults", left, top + 320, COLOR_HINT);
        GuiDrawCompat.drawRect(left, top + 334, right, top + 335, COLOR_DIVIDER);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private CfnToggleButton addToggle(int id, int x, int y, String label, boolean checked) {
        CfnToggleButton button = new CfnToggleButton(id, x, y, 220, 20, label, checked);
        this.buttonList.add(button);
        return button;
    }

    private boolean toggle(CfnToggleButton button) {
        button.setChecked(!button.isChecked());
        return button.isChecked();
    }

    private String freelookLabel() {
        if (this.waitingFreelookKey) {
            return "Freelook Key  [Press key]";
        }
        return "Freelook Key  [" + Keyboard.getKeyName(this.config.getFreelookKey()) + "]";
    }

    private void refreshFreelookLabel() {
        if (this.freelookBtn != null) {
            this.freelookBtn.displayString = freelookLabel();
        }
    }

    private void closeAndSave() {
        if (!this.saved) {
            this.saved = true;
            this.config.save();
        }
    }

    private void drawText(String text, int x, int y, int color) {
        if (useOptimizedFontPath()) {
            this.fontRendererObj.drawString(text, x, y, color);
        } else {
            this.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    private static boolean useOptimizedFontPath() {
        try {
            Class<?> clazz = Class.forName("com.clientfn.optifine.RenderPathSelector");
            Field field = clazz.getField("USE_EXTREME_PATH");
            return field.getBoolean(null);
        } catch (Exception ex) {
            return false;
        }
    }

    private static final class CfnToggleButton extends GuiButton {
        private final String label;
        private boolean checked;

        private CfnToggleButton(int id, int x, int y, int width, int height, String label, boolean checked) {
            super(id, x, y, width, height, "");
            this.label = label;
            this.checked = checked;
            refresh();
        }

        private boolean isChecked() {
            return this.checked;
        }

        private void setChecked(boolean checked) {
            this.checked = checked;
            refresh();
        }

        private void refresh() {
            this.displayString = (this.checked ? "[x] " : "[ ] ") + this.label;
        }

        @Override
        protected int getHoverState(boolean mouseOver) {
            return 0;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }
            boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int bg = hovered ? 0xFF2A2A2A : 0xFF222222;
            GuiDrawCompat.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, bg);
            int textColor = this.checked ? COLOR_ACCENT : COLOR_TEXT;
            mc.fontRenderer.drawString(this.displayString, this.xPosition + 6, this.yPosition + 6, textColor);
        }
    }

    private static final class CfnActionButton extends GuiButton {
        private CfnActionButton(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override
        protected int getHoverState(boolean mouseOver) {
            return 0;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }
            boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int bg = hovered ? 0xFF2F2A20 : 0xFF242424;
            GuiDrawCompat.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, bg);
            GuiDrawCompat.drawRect(this.xPosition, this.yPosition + this.height - 1, this.xPosition + this.width, this.yPosition + this.height, COLOR_DIVIDER);
            int textColor = hovered ? COLOR_ACCENT : COLOR_TEXT;
            int x = this.xPosition + (this.width - mc.fontRenderer.getStringWidth(this.displayString)) / 2;
            mc.fontRenderer.drawString(this.displayString, x, this.yPosition + 6, textColor);
        }
    }
}
