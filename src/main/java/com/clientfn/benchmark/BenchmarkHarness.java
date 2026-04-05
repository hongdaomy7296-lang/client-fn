package com.clientfn.benchmark;

import com.clientfn.optifine.OptiFineDetector;
import com.clientfn.optifine.RenderPathSelector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class BenchmarkHarness {
    public static final BenchmarkHarness INSTANCE = new BenchmarkHarness();

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long PREWARM_NS = 5L * NANOS_PER_SECOND;
    private static final int NORMAL_SECONDS = 60;
    private static final int SKY_GAZE_SECONDS = 60;
    private static final int STABILITY_SECONDS = 600;
    private static final long TOGGLE_DEBOUNCE_MS = 300L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter ISO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final BenchmarkSampler normalSampler = new BenchmarkSampler(NORMAL_SECONDS, 7000);
    private final BenchmarkSampler skySampler = new BenchmarkSampler(SKY_GAZE_SECONDS, 9000);
    private final BenchmarkSampler stabilitySampler = new BenchmarkSampler(STABILITY_SECONDS, 7000);

    private Phase phase = Phase.IDLE;
    private BenchmarkCamera activeScene = null;
    private long phaseStartNs = 0L;
    private long lastFrameNs = -1L;
    private long lastToggleMs = 0L;
    private boolean autoStartChecked = false;
    private SceneMetrics normalMetrics = SceneMetrics.empty();
    private SceneMetrics skyMetrics = SceneMetrics.empty();

    private BenchmarkHarness() {
    }

    public synchronized void onClientTick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        handleToggleKey(minecraft);
        handleAutoStart(minecraft);
        if (this.phase == Phase.LOADING_WORLD) {
            BenchmarkWorldManager.ensureBenchmarkWorldLoaded(minecraft);
        }
        if (this.phase != Phase.IDLE && minecraft.theWorld != null && minecraft.thePlayer != null) {
            BenchmarkWorldManager.ensureBenchmarkConditions(minecraft.theWorld, minecraft.thePlayer);
            if (this.activeScene != null) {
                this.activeScene.activate(minecraft.thePlayer);
            }
        }
    }

    public synchronized void onFrame(Minecraft minecraft, long nowNs) {
        if (this.phase == Phase.IDLE || minecraft == null) {
            return;
        }

        long frameTimeNs = -1L;
        if (this.lastFrameNs > 0L) {
            frameTimeNs = nowNs - this.lastFrameNs;
        }
        this.lastFrameNs = nowNs;

        switch (this.phase) {
            case LOADING_WORLD:
                if (isWorldReady(minecraft)) {
                    transition(Phase.PREWARM, nowNs, BenchmarkCamera.NORMAL);
                    log("World ready, prewarm 5s.");
                }
                break;
            case PREWARM:
                if ((nowNs - this.phaseStartNs) >= PREWARM_NS) {
                    this.normalSampler.begin(nowNs);
                    transition(Phase.NORMAL, nowNs, BenchmarkCamera.NORMAL);
                    log("Sampling NORMAL for 60s.");
                }
                break;
            case NORMAL:
                if (frameTimeNs > 0L) {
                    this.normalSampler.recordFrame(frameTimeNs, nowNs);
                }
                if (this.normalSampler.shouldFinish(nowNs)) {
                    this.normalSampler.finish(nowNs);
                    this.normalMetrics = SceneMetrics.fromSampler(this.normalSampler, NORMAL_SECONDS);
                    this.skySampler.begin(nowNs);
                    transition(Phase.SKY_GAZE, nowNs, BenchmarkCamera.SKY_GAZE);
                    log("Sampling SKY_GAZE for 60s.");
                }
                break;
            case SKY_GAZE:
                if (frameTimeNs > 0L) {
                    this.skySampler.recordFrame(frameTimeNs, nowNs);
                }
                if (this.skySampler.shouldFinish(nowNs)) {
                    this.skySampler.finish(nowNs);
                    this.skyMetrics = SceneMetrics.fromSampler(this.skySampler, SKY_GAZE_SECONDS);
                    this.stabilitySampler.begin(nowNs);
                    transition(Phase.STABILITY, nowNs, BenchmarkCamera.NORMAL);
                    log("Sampling STABILITY for 600s.");
                }
                break;
            case STABILITY:
                if (frameTimeNs > 0L) {
                    this.stabilitySampler.recordFrame(frameTimeNs, nowNs);
                }
                if (this.stabilitySampler.shouldFinish(nowNs)) {
                    this.stabilitySampler.finish(nowNs);
                    writeOutputs(minecraft);
                    resetState();
                    log("Benchmark complete.");
                }
                break;
            default:
                break;
        }

        if (this.phase != Phase.IDLE && minecraft.theWorld != null && minecraft.thePlayer != null) {
            BenchmarkWorldManager.ensureBenchmarkConditions(minecraft.theWorld, minecraft.thePlayer);
            if (this.activeScene != null) {
                this.activeScene.activate(minecraft.thePlayer);
            }
        }
    }

    public synchronized void onWorldChanged(Minecraft minecraft) {
        if (minecraft == null || this.phase == Phase.IDLE) {
            return;
        }
        if (minecraft.theWorld != null && minecraft.thePlayer != null) {
            BenchmarkWorldManager.ensureBenchmarkConditions(minecraft.theWorld, minecraft.thePlayer);
            if (this.activeScene != null) {
                this.activeScene.activate(minecraft.thePlayer);
            }
        }
    }

    public synchronized void onRespawn(Minecraft minecraft) {
        onWorldChanged(minecraft);
    }

    private void handleToggleKey(Minecraft minecraft) {
        if (!Keyboard.isKeyDown(Keyboard.KEY_F6)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastToggleMs < TOGGLE_DEBOUNCE_MS) {
            return;
        }
        this.lastToggleMs = now;

        if (this.phase == Phase.IDLE) {
            startBenchmark(minecraft, "hotkey:F6");
        } else {
            log("Benchmark aborted by hotkey.");
            resetState();
        }
    }

    private void handleAutoStart(Minecraft minecraft) {
        if (this.autoStartChecked) {
            return;
        }
        this.autoStartChecked = true;
        if (shouldAutoStartFromCommandLine()) {
            startBenchmark(minecraft, "command-line");
        }
    }

    private void startBenchmark(Minecraft minecraft, String trigger) {
        if (this.phase != Phase.IDLE) {
            return;
        }
        this.normalMetrics = SceneMetrics.empty();
        this.skyMetrics = SceneMetrics.empty();
        this.lastFrameNs = -1L;
        transition(Phase.LOADING_WORLD, System.nanoTime(), null);
        BenchmarkWorldManager.ensureBenchmarkWorldLoaded(minecraft);
        log("Benchmark triggered (" + trigger + ").");
    }

    private void transition(Phase next, long nowNs, BenchmarkCamera scene) {
        if (this.activeScene != null && this.activeScene != scene) {
            this.activeScene.release();
        }
        this.activeScene = scene;
        this.phase = next;
        this.phaseStartNs = nowNs;
    }

    private void resetState() {
        if (this.activeScene != null) {
            this.activeScene.release();
        }
        BenchmarkCamera.releaseAll();
        this.activeScene = null;
        this.phase = Phase.IDLE;
        this.phaseStartNs = 0L;
        this.lastFrameNs = -1L;
    }

    private static boolean isWorldReady(Minecraft minecraft) {
        return minecraft != null && minecraft.theWorld != null && minecraft.thePlayer != null;
    }

    private static boolean shouldAutoStartFromCommandLine() {
        if (Boolean.getBoolean("clientfn.benchmark.autorun")) {
            return true;
        }
        String command = System.getProperty("sun.java.command", "");
        return command.contains("--clientfn-benchmark")
            || command.contains("--benchmark")
            || command.contains("-benchmark");
    }

    private void writeOutputs(Minecraft minecraft) {
        LocalDateTime now = LocalDateTime.now();
        String fileStamp = FILE_TIME_FORMAT.format(now);
        String isoTimestamp = ISO_TIME_FORMAT.format(now);
        String runMode = RenderPathSelector.USE_EXTREME_PATH ? "extreme" : "compat";
        boolean optifinePresent = OptiFineDetector.INSTANCE.isOptiFinePresent();
        String jvmArgs = getJvmArgs();
        long gcCount = this.stabilitySampler.getGcCount();
        long gcPauseMs = this.stabilitySampler.getGcPauseMs();
        long[] heapSnapshots = this.stabilitySampler.getHeapSnapshotBytes();
        String heapTrend = classifyHeapTrend(heapSnapshots);

        JsonObject root = new JsonObject();
        root.addProperty("version", "1.0");
        root.addProperty("build", resolveBuildId());
        root.addProperty("optifinePresent", optifinePresent);
        root.addProperty("runMode", runMode);
        root.addProperty("jvmArgs", jvmArgs);

        JsonObject scenes = new JsonObject();
        scenes.add("normal", toSceneJson(this.normalMetrics));
        scenes.add("skyGaze", toSceneJson(this.skyMetrics));
        root.add("scenes", scenes);

        JsonObject stability = new JsonObject();
        stability.addProperty("durationSec", STABILITY_SECONDS);
        stability.addProperty("gcCount", gcCount);
        stability.addProperty("gcPauseMs", gcPauseMs);
        stability.addProperty("heapTrend", heapTrend);
        JsonArray heapArray = new JsonArray();
        for (long snapshot : heapSnapshots) {
            heapArray.add(new JsonPrimitive(Long.valueOf(snapshot)));
        }
        stability.add("heapSnapshotBytes", heapArray);
        root.add("stability", stability);
        root.addProperty("timestamp", isoTimestamp);

        Path outputDir = resolveOutputDir(minecraft);
        String baseName = "benchmark_" + fileStamp;
        Path jsonPath = outputDir.resolve(baseName + ".json");
        Path summaryPath = outputDir.resolve(baseName + "_summary.txt");

        try {
            Files.createDirectories(outputDir);
            try (BufferedWriter writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            String summary = buildSummary(
                isoTimestamp,
                runMode,
                optifinePresent,
                this.normalMetrics,
                this.skyMetrics,
                gcCount,
                gcPauseMs,
                heapTrend,
                heapSnapshots,
                jsonPath
            );
            try (BufferedWriter writer = Files.newBufferedWriter(summaryPath, StandardCharsets.UTF_8)) {
                writer.write(summary);
            }
            System.out.println(summary);
        } catch (IOException ex) {
            log("Failed to write benchmark output: " + ex.getMessage());
        }
    }

    private static JsonObject toSceneJson(SceneMetrics metrics) {
        JsonObject obj = new JsonObject();
        obj.addProperty("avgFps", metrics.avgFps);
        obj.addProperty("p95FrameMs", metrics.p95FrameMs);
        obj.addProperty("p99FrameMs", metrics.p99FrameMs);
        obj.addProperty("p999FrameMs", metrics.p999FrameMs);
        return obj;
    }

    private static String buildSummary(
        String timestamp,
        String runMode,
        boolean optifinePresent,
        SceneMetrics normal,
        SceneMetrics sky,
        long gcCount,
        long gcPauseMs,
        String heapTrend,
        long[] heapSnapshots,
        Path jsonPath
    ) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("ClientFN Benchmark Summary\n");
        sb.append("timestamp: ").append(timestamp).append('\n');
        sb.append("runMode: ").append(runMode).append('\n');
        sb.append("optifinePresent: ").append(optifinePresent).append('\n');
        sb.append(String.format(Locale.ROOT, "normal  avgFps=%.2f p95=%.3fms p99=%.3fms p999=%.3fms%n",
            normal.avgFps, normal.p95FrameMs, normal.p99FrameMs, normal.p999FrameMs));
        sb.append(String.format(Locale.ROOT, "skyGaze avgFps=%.2f p95=%.3fms p99=%.3fms p999=%.3fms%n",
            sky.avgFps, sky.p95FrameMs, sky.p99FrameMs, sky.p999FrameMs));
        sb.append("stability durationSec=").append(STABILITY_SECONDS)
            .append(" gcCount=").append(gcCount)
            .append(" gcPauseMs=").append(gcPauseMs)
            .append(" heapTrend=").append(heapTrend)
            .append('\n');
        sb.append("heapSnapshots=");
        for (int i = 0; i < heapSnapshots.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(heapSnapshots[i]);
        }
        sb.append('\n');
        sb.append("json: ").append(jsonPath).append('\n');
        return sb.toString();
    }

    private static String classifyHeapTrend(long[] heapSnapshots) {
        if (heapSnapshots == null || heapSnapshots.length < 2) {
            return "stable";
        }
        long first = heapSnapshots[0];
        long last = heapSnapshots[heapSnapshots.length - 1];
        if (first <= 0L) {
            return "stable";
        }
        double ratio = (double) last / (double) first;
        return ratio > 1.12D ? "growing" : "stable";
    }

    private static Path resolveOutputDir(Minecraft minecraft) {
        if (minecraft != null && minecraft.mcDataDir != null) {
            return minecraft.mcDataDir.toPath().resolve("clientfn_benchmark");
        }
        return Paths.get("clientfn_benchmark");
    }

    private static String getJvmArgs() {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (args == null || args.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(args.get(i));
        }
        return sb.toString();
    }

    private static String resolveBuildId() {
        String fromProperty = System.getProperty("clientfn.build.commit", "").trim();
        if (!fromProperty.isEmpty()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("CLIENTFN_BUILD_COMMIT");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        return "unknown";
    }

    private static void log(String message) {
        System.out.println("[ClientFN][Benchmark] " + message);
    }

    private enum Phase {
        IDLE,
        LOADING_WORLD,
        PREWARM,
        NORMAL,
        SKY_GAZE,
        STABILITY
    }

    private static final class SceneMetrics {
        final double avgFps;
        final double p95FrameMs;
        final double p99FrameMs;
        final double p999FrameMs;

        private SceneMetrics(double avgFps, double p95FrameMs, double p99FrameMs, double p999FrameMs) {
            this.avgFps = avgFps;
            this.p95FrameMs = p95FrameMs;
            this.p99FrameMs = p99FrameMs;
            this.p999FrameMs = p999FrameMs;
        }

        static SceneMetrics empty() {
            return new SceneMetrics(0.0D, 0.0D, 0.0D, 0.0D);
        }

        static SceneMetrics fromSampler(BenchmarkSampler sampler, int windowSeconds) {
            return new SceneMetrics(
                sampler.getAverageFps(windowSeconds),
                sampler.getFrameTimePercentile(0.95D),
                sampler.getFrameTimePercentile(0.99D),
                sampler.getFrameTimePercentile(0.999D)
            );
        }
    }
}
