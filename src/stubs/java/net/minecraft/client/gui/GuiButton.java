package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

public class GuiButton extends Gui {
    public int id;
    public int xPosition;
    public int yPosition;
    public int width;
    public int height;
    public String displayString;
    public boolean enabled = true;
    public boolean visible = true;

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String text) {
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = text;
    }

    protected int getHoverState(boolean mouseOver) {
        return 0;
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
    }
}
