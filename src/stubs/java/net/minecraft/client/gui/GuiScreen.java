package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class GuiScreen extends Gui {
    public final List<GuiButton> buttonList = new ArrayList<GuiButton>();
    public int width;
    public int height;
    public Minecraft mc;
    public FontRenderer fontRendererObj;

    public void initGui() {
    }

    protected void actionPerformed(GuiButton button) {
    }

    protected void keyTyped(char typedChar, int keyCode) {
    }

    public void onGuiClosed() {
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }
}
