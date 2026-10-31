package org.atcgroup.netcompat;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NetworkCompat.MODID)
public final class  NetworkCompat {
    public static final String MODID = "networkcompat";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static boolean SABLE_PRESENT = false;

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public NetworkCompat(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    public static void warn() {
        LOGGER.warn("============================================================");
        LOGGER.warn("!!! NETWORK-COMPAT WARNING !!!");
        LOGGER.warn("============================================================");
        LOGGER.warn("NetworkCompat is attempting to establish a connection under");
        LOGGER.warn("NON-STANDARD / UNSAFE OPERATING CONDITIONS.");
        LOGGER.warn("------------------------------------------------------------");
        LOGGER.warn("This indicates that the network pipeline has been modified,");
        LOGGER.warn("or that lifecycle assumptions have been violated.");
        LOGGER.warn("------------------------------------------------------------");
        LOGGER.warn("Continuing in this state may result in:");
        LOGGER.warn(" - Protocol desynchronization");
        LOGGER.warn(" - Silent data corruption");
        LOGGER.warn(" - Undefined mod interaction behavior");
        LOGGER.warn("============================================================");
    }

    @SubscribeEvent
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        SABLE_PRESENT = ModList.get().getModContainerById("sable").isPresent();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
