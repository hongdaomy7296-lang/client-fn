package com.clientfn.mixin.core;

import com.clientfn.core.GuiScreenCompat;
import com.clientfn.ui.ClientFnSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

/**
 * Adds a minimal CFN entry button to main menu.
 */
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {
    @Unique
    private static final int CLIENTFN_BUTTON_ID = 43180;

    @Inject(method = {"func_73866_w_", "initGui"}, at = @At("TAIL"), require = 0)
    private void clientfn$addButton(CallbackInfo ci) {
        GuiScreen screen = (GuiScreen) (Object) this;
        java.util.List buttons = GuiScreenCompat.getButtonList(screen);
        if (buttons == null) {
            return;
        }
        for (Iterator<?> it = buttons.iterator(); it.hasNext(); ) {
            Object next = it.next();
            if (next instanceof GuiButton && ((GuiButton) next).id == CLIENTFN_BUTTON_ID) {
                it.remove();
            }
        }
        int width = GuiScreenCompat.getWidth(screen);
        int height = GuiScreenCompat.getHeight(screen);
        buttons.add(new GuiButton(CLIENTFN_BUTTON_ID, width - 34, height - 18, 30, 14, "CFN"));
    }

    @Inject(method = {"func_146284_a", "actionPerformed"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$openSettings(GuiButton button, CallbackInfo ci) {
        if (button != null && button.id == CLIENTFN_BUTTON_ID) {
            GuiScreen screen = (GuiScreen) (Object) this;
            Minecraft mc = GuiScreenCompat.getMinecraft(screen);
            if (mc != null) {
                mc.displayGuiScreen(new ClientFnSettingsScreen(screen));
            }
            ci.cancel();
        }
    }
}
