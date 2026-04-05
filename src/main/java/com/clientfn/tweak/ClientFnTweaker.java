package com.clientfn.tweak;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;
import org.spongepowered.asm.launch.MixinTweaker;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Production tweaker for ClientFN.
 */
public class ClientFnTweaker implements ITweaker {

    private static final String OPTIFINE_TRANSFORMER_CLASS = "optifine.OptiFineClassTransformer";
    private static final boolean CLIENTFN_ENABLE_OPTIFINE_BOOTSTRAP = false;
    private static volatile boolean clientfn$optiFineBootstrapAttempted;
    private static volatile boolean clientfn$optiFineBootstrapSuccess;

    private final MixinTweaker delegate = new MixinTweaker();
    private final List<String> pendingArgs = new ArrayList<String>();
    private final List<String> ensuredArgs = new ArrayList<String>();

    private String profile;
    private File gameDir;
    private File assetsDir;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        clientfn$ensureNetworkCompatibilityDefaults();
        clientfn$logNetworkPreferenceState();
        this.pendingArgs.clear();
        this.ensuredArgs.clear();
        if (args != null) {
            this.pendingArgs.addAll(args);
        }
        this.gameDir = gameDir;
        this.assetsDir = assetsDir;
        this.profile = profile;

        ensureOptionIfValue(this.pendingArgs, this.ensuredArgs, "gameDir", clientfn$pathOrNull(gameDir));
        ensureOptionIfValue(this.pendingArgs, this.ensuredArgs, "assetsDir", clientfn$pathOrNull(assetsDir));
        ensureOption(this.pendingArgs, this.ensuredArgs, "version", (profile != null && !profile.isEmpty()) ? profile : "1.7.10-ClientFN");
        ensureOption(this.pendingArgs, this.ensuredArgs, "userProperties", "{}");

        this.delegate.acceptOptions(new ArrayList<String>(this.pendingArgs), gameDir, assetsDir, profile);
        Mixins.addConfiguration("mixins.clientfn.json");
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.registerTransformer("com.clientfn.tweak.GuiScreenAccessWidener");
        if (CLIENTFN_ENABLE_OPTIFINE_BOOTSTRAP) {
            clientfn$bootstrapOptiFineIfNeeded(classLoader);
        } else {
            clientfn$logWarn("OptiFine bootstrap disabled for stability (multiplayer login safeguard).");
        }
        this.delegate.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return this.delegate.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        List<String> out = new ArrayList<String>(this.pendingArgs);
        if (out.isEmpty()) {
            String[] mixinArgs = this.delegate.getLaunchArguments();
            if (mixinArgs != null) {
                out.addAll(Arrays.asList(mixinArgs));
            }
        }

        ensureOption(out, null, "version", (this.profile != null && !this.profile.isEmpty()) ? this.profile : "1.7.10-ClientFN");
        ensureOption(out, null, "userProperties", "{}");
        ensureOptionIfValue(out, null, "gameDir", clientfn$pathOrNull(this.gameDir));
        ensureOptionIfValue(out, null, "assetsDir", clientfn$pathOrNull(this.assetsDir));

        clientfn$logLaunchArgSummary(out);
        return out.toArray(new String[0]);
    }

    private static void ensureOption(List<String> args, List<String> addedArgs, String key, String value) {
        String flag = "--" + key;
        for (String arg : args) {
            if (flag.equals(arg) || (arg != null && arg.startsWith(flag + "="))) {
                return;
            }
        }
        args.add(flag);
        args.add(value);
        if (addedArgs != null) {
            addedArgs.add(flag);
            addedArgs.add(value);
        }
    }

    private static void ensureOptionIfValue(List<String> args, List<String> addedArgs, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        ensureOption(args, addedArgs, key, value);
    }

    private static List<String> normalizeLaunchArguments(List<String> source) {
        List<String> out = new ArrayList<String>();
        Set<String> seenOptions = new HashSet<String>();
        for (int i = 0; i < source.size(); i++) {
            String token = source.get(i);
            if (token == null || token.isEmpty()) {
                continue;
            }

            if (!isLongOption(token)) {
                out.add(token);
                continue;
            }

            int eqIndex = token.indexOf('=');
            String optionKey = eqIndex >= 0 ? token.substring(0, eqIndex) : token;
            if (seenOptions.contains(optionKey)) {
                if (eqIndex < 0 && i + 1 < source.size() && !isLongOption(source.get(i + 1))) {
                    i++;
                }
                continue;
            }

            seenOptions.add(optionKey);
            out.add(token);
            if (eqIndex < 0 && i + 1 < source.size() && !isLongOption(source.get(i + 1))) {
                out.add(source.get(++i));
            }
        }
        return out;
    }

    private static boolean isLongOption(String token) {
        return token != null && token.startsWith("--");
    }

    private static void clientfn$logLaunchArgSummary(List<String> args) {
        clientfn$logInfo(
            "Launch args summary: hasVersion=%s hasAssetsDir=%s hasAssetIndex=%s hasUuid=%s hasAccessToken=%s hasUserProperties=%s size=%d",
            hasOption(args, "version"),
            hasOption(args, "assetsDir"),
            hasOption(args, "assetIndex"),
            hasOption(args, "uuid"),
            hasOption(args, "accessToken"),
            hasOption(args, "userProperties"),
            Integer.valueOf(args != null ? args.size() : 0)
        );
    }

    private static boolean hasOption(List<String> args, String key) {
        if (args == null) {
            return false;
        }
        String flag = "--" + key;
        for (String arg : args) {
            if (flag.equals(arg) || (arg != null && arg.startsWith(flag + "="))) {
                return true;
            }
        }
        return false;
    }

    private static String clientfn$pathOrNull(File file) {
        return file != null ? file.getAbsolutePath() : null;
    }

    private static boolean hasExternalOptiFineTweaker() {
        try {
            Object value = Launch.blackboard.get("TweakClasses");
            if (!(value instanceof List<?>)) {
                return false;
            }
            for (Object item : (List<?>) value) {
                if (item == null) {
                    continue;
                }
                String name = item.toString().toLowerCase();
                if (name.contains("optifine.optifinetweaker") || name.contains("optifine.optifineforgetweaker")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Optional probe.
        }
        return false;
    }

    private void clientfn$bootstrapOptiFineIfNeeded(LaunchClassLoader classLoader) {
        if (hasExternalOptiFineTweaker() || clientfn$optiFineBootstrapSuccess) {
            return;
        }

        synchronized (ClientFnTweaker.class) {
            if (clientfn$optiFineBootstrapAttempted || hasExternalOptiFineTweaker() || clientfn$optiFineBootstrapSuccess) {
                return;
            }
            clientfn$optiFineBootstrapAttempted = true;
            clientfn$logInfo("OptiFine bootstrap start (gameDir=%s, assetsDir=%s)", this.gameDir, this.assetsDir);

            File locatedJar = null;
            try {
                locatedJar = clientfn$findOptiFineJar();
                if (locatedJar == null) {
                    clientfn$logWarn("OptiFine bootstrap skipped: OptiFine jar not found in libraries.");
                    return;
                }
                clientfn$logInfo("OptiFine jar candidate: %s", locatedJar.getAbsolutePath());
                if (!clientfn$isOptiFineJarCompatibleWithRuntime(locatedJar, classLoader)) {
                    clientfn$logWarn("OptiFine bootstrap skipped: jar/runtime namespace mismatch. Current game uses deobf net.minecraft classes, but OptiFine jar is obfuscated.");
                    return;
                }
                classLoader.addURL(locatedJar.toURI().toURL());
                clientfn$prepareOptiFineClassloading(classLoader);

                clientfn$clearLaunchClassLoaderCaches(classLoader, OPTIFINE_TRANSFORMER_CLASS);
                classLoader.registerTransformer(OPTIFINE_TRANSFORMER_CLASS);

                if (clientfn$hasTransformer(classLoader, OPTIFINE_TRANSFORMER_CLASS)) {
                    clientfn$markOptiFineLoaded();
                    clientfn$optiFineBootstrapSuccess = true;
                    clientfn$logInfo("OptiFine transformer loaded from %s", locatedJar.getAbsolutePath());
                    return;
                }

                clientfn$logWarn("OptiFine bootstrap skipped: transformer %s was not registered.", OPTIFINE_TRANSFORMER_CLASS);
            } catch (MalformedURLException ex) {
                clientfn$logWarn("Invalid OptiFine library URL: %s", ex.getMessage());
            } catch (Throwable ex) {
                clientfn$logWarn("OptiFine bootstrap failed: %s: %s", ex.getClass().getSimpleName(), ex.getMessage());
            }
        }
    }

    private void clientfn$markOptiFineLoaded() {
        System.setProperty("clientfn.optifine.loaded", "true");
    }

    private File clientfn$findOptiFineJar() {
        List<File> roots = new ArrayList<File>();
        clientfn$collectRoots(roots, this.gameDir);
        clientfn$collectRoots(roots, this.assetsDir);
        clientfn$collectRoots(roots, new File("."));

        for (File root : roots) {
            File jar = clientfn$findOptiFineJarInLibraries(new File(root, "libraries"));
            if (jar != null) {
                return jar;
            }
        }
        return null;
    }

    private static void clientfn$collectRoots(List<File> out, File start) {
        File cursor = start;
        int depth = 0;
        while (cursor != null && depth < 6) {
            if (!out.contains(cursor)) {
                out.add(cursor);
            }
            cursor = cursor.getParentFile();
            depth++;
        }
    }

    private static File clientfn$findOptiFineJarInLibraries(File librariesDir) {
        if (librariesDir == null || !librariesDir.isDirectory()) {
            return null;
        }

        File pinned = new File(librariesDir, "optifine/OptiFine/1.7.10_HD_U_E7/OptiFine-1.7.10_HD_U_E7.jar");
        if (pinned.isFile()) {
            return pinned;
        }

        File optiFineRoot = new File(librariesDir, "optifine/OptiFine");
        if (!optiFineRoot.isDirectory()) {
            return null;
        }

        File[] versions = optiFineRoot.listFiles();
        if (versions == null) {
            return null;
        }

        for (File versionDir : versions) {
            if (versionDir == null || !versionDir.isDirectory()) {
                continue;
            }

            File[] files = versionDir.listFiles();
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file == null || !file.isFile()) {
                    continue;
                }
                String name = file.getName();
                if (name.startsWith("OptiFine-1.7.10_") && name.endsWith(".jar")) {
                    return file;
                }
            }
        }

        return null;
    }

    private static void clientfn$clearLaunchClassLoaderCaches(LaunchClassLoader classLoader, String className) {
        try {
            classLoader.clearNegativeEntries(Collections.singleton(className));
            classLoader.clearNegativeEntries(Collections.singleton(className.replace('.', '/')));
        } catch (Throwable ignored) {
            // Optional cleanup path.
        }

        try {
            java.lang.reflect.Field field = LaunchClassLoader.class.getDeclaredField("invalidClasses");
            field.setAccessible(true);
            Object value = field.get(classLoader);
            if (value instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                Set<Object> invalid = (Set<Object>) value;
                invalid.remove(className);
            }
        } catch (Throwable ignored) {
            // Optional cleanup path.
        }
    }

    private static boolean clientfn$hasTransformer(LaunchClassLoader classLoader, String className) {
        try {
            for (Object transformer : classLoader.getTransformers()) {
                if (transformer != null && className.equals(transformer.getClass().getName())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Probe failure should not crash startup.
        }
        return false;
    }

    private static boolean clientfn$isOptiFineJarCompatibleWithRuntime(File jarFile, LaunchClassLoader classLoader) {
        boolean runtimeDeobf = classLoader.getResource("net/minecraft/client/Minecraft.class") != null;
        boolean runtimeObf = classLoader.getResource("bao.class") != null;

        boolean jarDeobf = clientfn$zipHasEntry(jarFile, "net/minecraft/client/Minecraft.class");
        boolean jarObf = clientfn$zipHasEntry(jarFile, "bao.class");

        if (runtimeDeobf && jarObf && !jarDeobf) {
            return false;
        }
        if (runtimeObf && jarDeobf && !jarObf) {
            return false;
        }
        return true;
    }

    private static boolean clientfn$zipHasEntry(File file, String entryName) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry(entryName);
            return entry != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void clientfn$prepareOptiFineClassloading(LaunchClassLoader classLoader) {
        try {
            classLoader.addTransformerExclusion("optifine.");
            clientfn$logInfo("OptiFine transformer exclusion registered on LaunchClassLoader.");
        } catch (Throwable ex) {
            clientfn$logWarn("Could not register OptiFine classloader exclusions: %s", ex.getMessage());
        }
    }

    private static void clientfn$logInfo(String template, Object... args) {
        try {
            LogWrapper.info("[ClientFN] " + String.format(template, args));
        } catch (Throwable ignored) {
            System.out.println("[ClientFN] " + String.format(template, args));
        }
    }

    private static void clientfn$logWarn(String template, Object... args) {
        try {
            LogWrapper.warning("[ClientFN] " + String.format(template, args));
        } catch (Throwable ignored) {
            System.out.println("[ClientFN] " + String.format(template, args));
        }
    }

    private static void clientfn$ensureNetworkCompatibilityDefaults() {
        clientfn$ensureSystemProperty("java.net.preferIPv4Stack", "true", "Enabled java.net.preferIPv4Stack=true for multiplayer compatibility.");
        clientfn$ensureSystemProperty("java.net.preferIPv4Addresses", "true", "Enabled java.net.preferIPv4Addresses=true for multiplayer compatibility.");
    }

    private static void clientfn$logNetworkPreferenceState() {
        String preferV4Stack = String.valueOf(System.getProperty("java.net.preferIPv4Stack"));
        String preferV4Addresses = String.valueOf(System.getProperty("java.net.preferIPv4Addresses"));
        clientfn$logInfo(
            "Network prefs: java.net.preferIPv4Stack=%s java.net.preferIPv4Addresses=%s",
            preferV4Stack,
            preferV4Addresses
        );
    }

    private static void clientfn$ensureSystemProperty(String key, String expectedValue, String logMessage) {
        String current = System.getProperty(key);
        if (current == null || current.trim().isEmpty()) {
            System.setProperty(key, expectedValue);
            clientfn$logInfo(logMessage);
        }
    }
}
