package org.mod.coretuner.profiler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.mod.coretuner.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DashboardServer {

    private static HttpServer server;

    public static void start() {
        try {
            // Levantamos el servidor en el puerto 8080
            server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Creamos las rutas de la API
            server.createContext("/api/stats", new StatsHandler());
            // NUEVA RUTA PARA RECIBIR COMANDOS DE LOS BOTONES
            server.createContext("/api/config", new ConfigHandler());

            server.setExecutor(null);
            server.start();
            System.out.println("[CoreTuner] API del Dashboard iniciada en http://localhost:8080/api/stats");
        } catch (IOException e) {
            System.out.println("[CoreTuner] Error al iniciar la API del Dashboard: " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[CoreTuner] API del Dashboard detenida.");
        }
    }

    // --- MANEJADOR DE ESTADÍSTICAS (ENVIAR DATOS A LA WEB) ---
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.addProperty("tps", ServerProfiler.getTPS());
            json.addProperty("ram_mb", ServerProfiler.getUsedRAM());
            json.addProperty("cpu_load", ServerProfiler.getCpuLoad());
            json.addProperty("tick_ms", ServerProfiler.getAverageTickTimeMs());

            json.addProperty("villagers_total", ServerProfiler.lastTotalVillagers);
            json.addProperty("villagers_confined", ServerProfiler.lastConfinedVillagers);
            json.addProperty("hoppers_total", ServerProfiler.lastTotalHoppers);
            json.addProperty("hoppers_sleeping", ServerProfiler.lastSleepingHoppers);

            String response = json.toString();

            t.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // --- MANEJADOR DE CONFIGURACIÓN (RECIBIR ÓRDENES DE LA WEB) ---
    static class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Habilitar CORS para permitir solicitudes tipo POST
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Si el navegador hace una comprobación previa (Preflight Request)
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            // Procesar el clic del botón (POST)
            if (t.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    InputStream is = t.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    // 1. Botón de Tolvas
                    if (json.has("optimizeHoppers")) {
                        boolean state = json.get("optimizeHoppers").getAsBoolean();
                        Config.OPTIMIZE_HOPPERS.set(state);
                        System.out.println("[CoreTuner] Dashboard ordenó: Tolvas = " + state);
                    }

                    // 2. Botón de Aldeanos
                    if (json.has("optimizeVillagers")) {
                        boolean state = json.get("optimizeVillagers").getAsBoolean();
                        Config.OPTIMIZE_VILLAGERS.set(state);
                        System.out.println("[CoreTuner] Dashboard ordenó: Aldeanos = " + state);
                    }

                    // 3. Botón de Entity Cramming (Vanilla Gamerule)
                    if (json.has("entityCramming")) {
                        boolean active = json.get("entityCramming").getAsBoolean();
                        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) {
                            int limit = active ? 24 : 0;
                            server.getGameRules().getRule(GameRules.RULE_MAX_ENTITY_CRAMMING).set(limit, server);
                            System.out.println("[CoreTuner] Dashboard ordenó: Entity Cramming = " + limit);
                        }
                    }

                    // Enviar respuesta de éxito a la web
                    String response = "{\"status\":\"success\"}";
                    t.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    // Manejo de errores si la web manda algo raro
                    String response = "{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}";
                    t.sendResponseHeaders(400, response.getBytes().length);
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        }
    }
}