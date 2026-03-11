package org.mod.coretuner;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CoreTuner.MODID)
public class CoreTuner {
    public static final String MODID = "coretuner";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CoreTuner(FMLJavaModLoadingContext context) {

        // Registrar mixin config programáticamente — garantiza que Mixin

        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, Config.SPEC);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("INICIALIZANDO CORETUNER - Preparando optimizaciones...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("CoreTuner: Servidor arrancando. Iniciando API del Dashboard...");
        org.mod.coretuner.profiler.DashboardServer.start();
    }

    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        LOGGER.info("CoreTuner: Servidor deteniéndose. Apagando API...");
        org.mod.coretuner.profiler.DashboardServer.stop();
    }
}