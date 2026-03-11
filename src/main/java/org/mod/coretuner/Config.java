package org.mod.coretuner;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CoreTuner.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue OPTIMIZE_HOPPERS;
    public static final ForgeConfigSpec.BooleanValue OPTIMIZE_VILLAGERS;

    // Tu nueva variable configurable
    public static final ForgeConfigSpec.IntValue HOPPER_SLEEP_TICKS;

    static {
        BUILDER.push("Optimizaciones del Servidor");

        OPTIMIZE_HOPPERS = BUILDER
                .comment("Activa o desactiva la optimización de las tolvas (Caché de colisión y Sleep State).")
                .define("optimizeHoppers", true);

        // Rango de 8 a 40 introducido por ti
        HOPPER_SLEEP_TICKS = BUILDER
                .comment("Ticks que duerme una tolva inactiva. Vanilla=8. Rango: 8-40.",
                        "Valores altos mejoran mucho el rendimiento con 100+ tolvas.",
                        "ADVERTENCIA: valores >8 pueden hacer contraptions de timing más lentas.")
                .defineInRange("hopperSleepTicks", 8, 8, 40);

        OPTIMIZE_VILLAGERS = BUILDER
                .comment("Congela la IA de los aldeanos encerrados en 1x1, manteniendo las granjas funcionales.")
                .define("optimizeVillagers", true);

        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) { }
}