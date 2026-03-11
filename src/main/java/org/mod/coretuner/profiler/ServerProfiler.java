package org.mod.coretuner.profiler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "coretuner", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerProfiler {

    private static long tickStartTime = 0;
    private static final long[] tickTimes = new long[100];
    private static int tickIndex = 0;

    public static final Map<String, Long> categoryUsage = new HashMap<>();
    public static final Map<String, Long> chunkUsage    = new HashMap<>();
    public static final Map<String, Long> modUsage      = new HashMap<>();

    public static volatile int lastTotalVillagers    = 0;
    public static volatile int lastConfinedVillagers = 0;
    public static volatile int lastTotalHoppers      = 0;
    public static volatile int lastSleepingHoppers   = 0;

    private static int tickConfinedVillagers = 0;
    private static int tickTotalHoppers      = 0;
    private static int tickSleepingHoppers   = 0;

    private static int scanTimer = 0;
    private static final int SCAN_INTERVAL = 20;

    public static void resetTickCounters() {
        tickConfinedVillagers = 0;
        tickTotalHoppers      = 0;
        tickSleepingHoppers   = 0;
    }

    public static void saveTickCounters() {
        lastConfinedVillagers = tickConfinedVillagers;
        lastTotalHoppers      = tickTotalHoppers;
        lastSleepingHoppers   = tickSleepingHoppers;
    }

    // Llamado desde customServerAiStep — 1 vez por aldeano por ciclo de AI
    public static void countVillager(boolean isConfined) {
        if (isConfined) tickConfinedVillagers++;
    }

    // Llamado desde el mixin de hopper cuando lo implementes
    public static void countHopper(boolean isSleeping) {
        tickTotalHoppers++;
        if (isSleeping) tickSleepingHoppers++;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickStartTime = System.nanoTime();
            resetTickCounters();
        } else if (event.phase == TickEvent.Phase.END) {
            tickTimes[tickIndex] = System.nanoTime() - tickStartTime;
            tickIndex = (tickIndex + 1) % 100;
            saveTickCounters();

            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                doEntityScan();
            }
        }
    }

    // Total de aldeanos — scan directo 1 vez/segundo
    // No depende del mixin, cuenta todos los chunks cargados
    private static void doEntityScan() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int totalVillagers = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof Villager) {
                    totalVillagers++;
                }
            }
        }

        lastTotalVillagers = totalVillagers;
    }

    public static void recordCategory(String category, long durationNs) {
        categoryUsage.put(category, categoryUsage.getOrDefault(category, 0L) + durationNs);
    }

    public static void recordChunk(int chunkX, int chunkZ, long durationNs) {
        String key = "[" + chunkX + ", " + chunkZ + "]";
        chunkUsage.put(key, chunkUsage.getOrDefault(key, 0L) + durationNs);
    }

    public static void recordMod(String modId, long durationNs) {
        modUsage.put(modId, modUsage.getOrDefault(modId, 0L) + durationNs);
    }

    public static double getAverageTickTimeMs() {
        long sum = 0;
        for (long t : tickTimes) sum += t;
        return (sum / 100.0) / 1_000_000.0;
    }

    public static double getTPS() {
        double avgTickMs = getAverageTickTimeMs();
        return avgTickMs > 50.0 ? 1000.0 / avgTickMs : 20.0;
    }

    public static long getUsedRAM() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    public static double getCpuLoad() {
        double avgTickMs = getAverageTickTimeMs();
        return Math.min((avgTickMs / 50.0) * 100.0, 100.0);
    }

    public static long startMeasurement() { return System.nanoTime(); }

    public static void stopMeasurement(String categoryName, long startTime) {
        recordCategory(categoryName, System.nanoTime() - startTime);
    }
}