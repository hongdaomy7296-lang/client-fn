package com.clientfn.ui;

import com.clientfn.core.GuiDrawCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Lightweight slider for the settings screen.
 */
public class ClientFnSlider extends GuiButton {
    public interface OnValueChanged {
        void onChanged(float value);
    }

    private static final int COLOR_BG = 0xFF262626;
    private static final int COLOR_TRACK = 0xFF3A3A3A;
    private static final int COLOR_KNOB = 0xFFD4A853;
    private static final int COLOR_TEXT = 0xFFE8E8E0;

    private final String label;
    private final float minValue;
    private final float maxValue;
    private final OnValueChanged callback;

    private boolean dragging;
    private float value;

    public ClientFnSlider(int id, int x, int y, int width, String label, float minValue, float maxValue, float value,
                          OnValueChanged callback) {
        super(id, x, y, width, 20, "");
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.callback = callback;
        setValue(value, false);
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(float value, boolean notify) {
        this.value = clamp(value, this.minValue, this.maxValue);
        this.displayString = this.label + "  " + format(this.value);
        if (notify && this.callback != null) {
            this.callback.onChanged(this.value);
        }
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!this.enabled || !this.visible || mouseX < this.xPosition || mouseY < this.yPosition
            || mouseX >= this.xPosition + this.width || mouseY >= this.yPosition + this.height) {
            return false;
        }
        this.dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }

        if (this.dragging) {
            updateFromMouse(mouseX);
        }

        GuiDrawCompat.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, COLOR_BG);

        int trackY = this.yPosition + 14;
        int trackLeft = this.xPosition + 8;
        int trackRight = this.xPosition + this.width - 8;
        GuiDrawCompat.drawRect(trackLeft, trackY, trackRight, trackY + 2, COLOR_TRACK);

        float ratio = (this.value - this.minValue) / (this.maxValue - this.minValue);
        ratio = clamp(ratio, 0.0F, 1.0F);
        int knobX = trackLeft + Math.round((trackRight - trackLeft) * ratio);
        GuiDrawCompat.drawRect(knobX - 2, trackY - 4, knobX + 2, trackY + 6, COLOR_KNOB);

        mc.fontRenderer.drawString(this.displayString, this.xPosition + 6, this.yPosition + 4, COLOR_TEXT);
    }

    private void updateFromMouse(int mouseX) {
        int trackLeft = this.xPosition + 8;
        int trackRight = this.xPosition + this.width - 8;
        float ratio = (float) (mouseX - trackLeft) / (float) (trackRight - trackLeft);
        ratio = clamp(ratio, 0.0F, 1.0F);
        float mapped = this.minValue + ratio * (this.maxValue - this.minValue);
        setValue(mapped, true);
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
