package org.atcgroup.netcompat.mixin.client;

import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload;
import org.atcgroup.netcompat.NCClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public abstract class MixinClientConfigurationPacketListener {
    @Redirect(
            method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/payload/ModdedNetworkSetupFailedPayload;failureReasons()Ljava/util/Map;",
                    remap = false
            )
    )
    private Map<ResourceLocation, Component> nc$handleFailureReasons(ModdedNetworkSetupFailedPayload payload) {
        return networkCompat$handle(payload.failureReasons());
    }

    @Unique
    private static Map<ResourceLocation, Component> networkCompat$handle(Map<ResourceLocation, Component> failureReasons) {
        var data = new HashMap<ResourceLocation, Component>();
        var mods = new HashSet<ResourceLocation>();

        failureReasons.forEach((key, value) -> {
            if (!(value.getContents() instanceof PlainTextContents cc)) {
                data.put(key, value);
                return;
            }

            if (cc.text().equals(NCClient.NOTIFY_MOD_NOT_EXIST)) {
                mods.add(key);
                return;
            }

            data.put(key, value);
        });

        NCClient.STATE.setNotPresentMods(mods);

        return data;
    }
}
