package org.atcgroup.netcompat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = NetworkCompat.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = NetworkCompat.MODID, value = Dist.CLIENT)
public final class NCClient {
    public static final String NOTIFY_MOD_NOT_EXIST = "[NC_NOTIFY_NOT_EXIST]";
    public static final ClientState STATE = new ClientState();
    public static final AtomicBoolean NEED_INV_REBUILD = new AtomicBoolean(false);

    public NCClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        //NeoForge.EVENT_BUS.register(ClientListener.class);
    }

    // =========================
    // 1. GUI 点击拦截（主路径）
    // =========================
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!STATE.isUnsafeEnvironment()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null) {
            return;
        }

        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {

            Slot slot = screen.getSlotUnderMouse();
            if (slot == null) {
                return;
            }

            ItemStack stack = slot.getItem();
            ItemStack carried = mc.player.containerMenu.getCarried();

            if (STATE.isUnsafeItem(stack) || STATE.isUnsafeItem(carried)) {
                event.setCanceled(true);
            }
        }
    }

    // =========================
    // 3. tick 兜底（防绕过）
    // =========================
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!STATE.isUnsafeEnvironment()) {
            return;
        }

        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        var carried = mc.player.containerMenu.getCarried();

        if (!carried.isEmpty() && STATE.isUnsafeItem(carried)) {
            mc.player.containerMenu.setCarried(new ItemStack(Items.STONE));
        }
    }

    // =========================
    // 4. hotbar / key swap 防护
    // =========================
    @SubscribeEvent
    public static void onKey(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        if (!STATE.isUnsafeEnvironment()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ItemStack carried = mc.player.containerMenu.getCarried();

        if (!carried.isEmpty() && STATE.isUnsafeItem(carried)) {
            mc.player.containerMenu.setCarried(new ItemStack(Items.STONE));
        }
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        //noinspection StringBufferReplaceableByString
        var sb = new StringBuilder();

        sb.append("[NetworkCompat] ");
        sb.append("Unsafe: ");

        sb.append(STATE.isUnsafeEnvironment() ? "§cUNSAFE" : "§aSECURE");
        sb.append("§f, Neoforged: ");
        sb.append(STATE.isVanillaConnection() ? "§cFALSE" : "§aTRUE");

        event.getLeft().add(sb.toString());
    }


    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        for (var item : new HashSet<>(event.getParentEntries())) {
            if (STATE.isUnsafeItem(item)) {
                event.remove(item, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
        for (var item : new HashSet<>(event.getSearchEntries())) {
            if (STATE.isUnsafeItem(item)) {
                event.remove(item, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    public static boolean allowModNetworkMixins(){
        return !STATE.isVanillaConnection();
    }
}
