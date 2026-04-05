package net.minecraft.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

import java.io.File;

public class Minecraft {
    public File mcDataDir;
    public FontRenderer fontRenderer;
    public WorldClient theWorld;
    public EntityPlayerSP thePlayer;
    public GuiScreen currentScreen;

    public static Minecraft getMinecraft() {
        return null;
    }

    public void displayGuiScreen(GuiScreen screen) {
    }
}
