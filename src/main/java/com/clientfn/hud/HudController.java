package com.clientfn.hud;

import com.clientfn.ui.ClientConfig;

/**
 * HUD runtime switches backed by client config.
 */
public final class HudController {

    private HudController() {
    }

    public static void syncFromConfig() {
        // HUD toggles are read live from ClientConfig.
    }

    public static boolean isChatHidden() {
        return ClientConfig.INSTANCE.isEnabled("hud.chat.hidden");
    }

    public static boolean isScoreboardHidden() {
        return ClientConfig.INSTANCE.isEnabled("hud.scoreboard.hidden");
    }
}
