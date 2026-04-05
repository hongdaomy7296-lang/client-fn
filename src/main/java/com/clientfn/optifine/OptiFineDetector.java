package com.clientfn.optifine;

import net.minecraft.launchwrapper.Launch;

import java.util.List;

/**
 * Detects OptiFine classes once at startup and exposes a cached result.
 */
public final class OptiFineDetector implements IOptiFineDetector {

    public static final OptiFineDetector INSTANCE = new OptiFineDetector();

    private static final boolean PRESENT;

    static {
        PRESENT = detectByBootstrapFlag() || detectByClassName() || detectByTweakerList();
    }

    private OptiFineDetector() {
    }

    @Override
    public boolean isOptiFinePresent() {
        return PRESENT;
    }

    @Override
    public boolean shouldUseCompatPath() {
        return PRESENT;
    }

    private static boolean detectByBootstrapFlag() {
        try {
            return Boolean.parseBoolean(System.getProperty("clientfn.optifine.loaded", "false"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean detectByClassName() {
        String[] candidates = new String[]{
            "optifine.OptiFineTweaker",
            "optifine.OptiFineForgeTweaker",
            "optifine.OptiFineClassTransformer"
        };
        for (String name : candidates) {
            try {
                Class.forName(name, false, OptiFineDetector.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException ignored) {
                // Try next candidate.
            } catch (Throwable ignored) {
                // Keep probing with the remaining candidates.
            }
        }
        return false;
    }

    private static boolean detectByTweakerList() {
        try {
            Object value = Launch.blackboard.get("TweakClasses");
            if (!(value instanceof List<?>)) {
                return false;
            }
            for (Object item : (List<?>) value) {
                if (item != null && item.toString().toLowerCase().contains("optifine")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Optional probe path, failures should not affect startup.
        }
        return false;
    }
}
