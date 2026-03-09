package org.mod.coretuner;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CoreTuner.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    // El constructor de nuestra configuración automática
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- NUESTROS INTERRUPTORES (TOGGLES) ---

    public static final ForgeConfigSpec.BooleanValue OPTIMIZE_HOPPERS;
    // Aquí agregaremos OPTIMIZE_VILLAGERS en el futuro, etc.

    static {
        BUILDER.push("Optimizaciones del Servidor"); // Crea una categoría en el archivo

        OPTIMIZE_HOPPERS = BUILDER
                .comment("Activa o desactiva la optimización de las tolvas (Caché de colisión y Sleep State).")
                .define("optimizeHoppers", true); // Por defecto viene activado

        BUILDER.pop();
    }

    // Esta variable contiene el archivo final generado
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Este evento se dispara si el Admin edita el archivo de texto mientras el juego está abierto
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Aquí podríamos recargar cosas si fuera necesario, el valor se actualiza solo.
    }
}