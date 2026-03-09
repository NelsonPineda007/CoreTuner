package org.mod.coretuner.profiler;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "coretuner", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerProfiler {

    // Cronómetro principal del servidor
    private static long tickStartTime = 0;
    private static final long[] tickTimes = new long[100];
    private static int tickIndex = 0;

    // --- NUESTRAS BASES DE DATOS EN RAM (Se limpian solas, impacto cero) ---
    public static final Map<String, Long> categoryUsage = new HashMap<>(); // Ej. "Tolvas", "Aldeanos"
    public static final Map<String, Long> chunkUsage = new HashMap<>();    // Ej. "Chunk [15, -4]"
    public static final Map<String, Long> modUsage = new HashMap<>();      // Ej. "create", "mekanism"

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickStartTime = System.nanoTime();
        } else if (event.phase == TickEvent.Phase.END) {
            tickTimes[tickIndex] = System.nanoTime() - tickStartTime;
            tickIndex = (tickIndex + 1) % 100;
        }
    }

    // --- MÉTODOS DE RECOLECCIÓN (Los Mixins enviarán datos aquí) ---

    public static void recordCategory(String category, long durationNs) {
        categoryUsage.put(category, categoryUsage.getOrDefault(category, 0L) + durationNs);
    }

    public static void recordChunk(int chunkX, int chunkZ, long durationNs) {
        String chunkKey = "[" + chunkX + ", " + chunkZ + "]";
        chunkUsage.put(chunkKey, chunkUsage.getOrDefault(chunkKey, 0L) + durationNs);
    }

    public static void recordMod(String modId, long durationNs) {
        modUsage.put(modId, modUsage.getOrDefault(modId, 0L) + durationNs);
    }

    // --- MÉTODOS PARA EL DASHBOARD (Consola en vivo) ---

    // 1. Calcula el tiempo promedio que tarda un tick (ideal: menos de 50ms)
    public static double getAverageTickTimeMs() {
        long sum = 0;
        for (long t : tickTimes) sum += t;
        return (sum / 100.0) / 1_000_000.0; // Convertimos nanosegundos a milisegundos
    }

    // 2. Calcula los TPS reales del servidor (Máximo 20)
    public static double getTPS() {
        double avgTickMs = getAverageTickTimeMs();
        // Si el tick tarda más de 50ms, el servidor se "lagea" y bajan los TPS
        return avgTickMs > 50.0 ? 1000.0 / avgTickMs : 20.0;
    }

    // 3. Obtiene la memoria RAM usada en Megabytes (MB)
    public static long getUsedRAM() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    // 4. Obtiene el uso estimado de CPU basado en la carga del Tick
    public static double getCpuLoad() {
        double avgTickMs = getAverageTickTimeMs();
        // 50ms = 100% de uso del hilo principal del servidor
        double load = (avgTickMs / 50.0) * 100.0;
        return Math.min(load, 100.0); // Lo topamos al 100% para la gráfica
    }

    // Inicia a contar el tiempo
    public static long startMeasurement() {
        return System.nanoTime();
    }

    // Detiene el tiempo y lo guarda en la base de datos de la categoría (ej. "Tolvas")
    public static void stopMeasurement(String categoryName, long startTime) {
        long durationNs = System.nanoTime() - startTime;
        recordCategory(categoryName, durationNs);
    }
}