package org.atcgroup.netcompat.mixin;

import com.google.common.collect.ImmutableSet;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforge.network.configuration.CheckExtensibleEnums;
import net.neoforged.neoforge.network.configuration.CheckFeatureFlags;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.atcgroup.netcompat.Config;
import org.atcgroup.netcompat.NCClient;
import org.atcgroup.netcompat.NetworkCompat;
import org.atcgroup.netcompat.mixin.client.MixinItemDisplayParameters;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static net.neoforged.neoforge.network.registration.NetworkRegistry.getInitialListeningChannels;

@SuppressWarnings("UnstableApiUsage")
@Mixin(NetworkRegistry.class)
public final class MixinNetworkRegistry {
    @Final
    @Shadow
    private static Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> PAYLOAD_REGISTRATIONS;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "initializeNeoForgeConnection(Lnet/minecraft/network/protocol/configuration/ClientConfigurationPacketListener;Lnet/neoforged/neoforge/network/registration/NetworkPayloadSetup;)V", at = @At("HEAD"))
    private static void nc$beforeInit(ClientConfigurationPacketListener listener, NetworkPayloadSetup setup, CallbackInfo ci) {
        NCClient.NEED_INV_REBUILD.set(true);

        NCClient.STATE.setVanillaConnection(false);
        NCClient.STATE.setUnsafeEnvironment(false);
    }

    /**
     * @author GB2022
     * @reason remove check in unsafe env.
     */
    @Overwrite
    public static void initializeOtherConnection(ClientConfigurationPacketListener listener) {
        NCClient.STATE.setVanillaConnection(true);

        ChannelAttributes.setPayloadSetup(listener.getConnection(), NetworkPayloadSetup.empty());
        ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());

        var registrations = PAYLOAD_REGISTRATIONS.keySet().iterator();

        ConfigTracker.INSTANCE.loadDefaultServerConfigs();
        LOGGER.info("Running at No-NF mode, providing default configuration to NF.");

        NegotiationResult negotiationResult;
        do {
            if (!registrations.hasNext()) {
                if (!CheckExtensibleEnums.handleVanillaServerConnection(listener)) {
                    return;
                }

                if (!CheckFeatureFlags.handleVanillaServerConnection(listener)) {
                    return;
                }

                NetworkFilters.injectIfNecessary(listener.getConnection());

                var nowListeningOn = ImmutableSet.<ResourceLocation>builder();
                nowListeningOn.addAll(getInitialListeningChannels(listener.flow()));

                (PAYLOAD_REGISTRATIONS.get(ConnectionProtocol.CONFIGURATION)).entrySet()
                                                                             .stream()
                                                                             .filter((registration) -> registration.getValue()
                                                                                                                   .matchesFlow(listener.flow()))
                                                                             .filter((registration) -> registration.getValue()
                                                                                                                   .optional())
                                                                             .forEach((registration) -> nowListeningOn.add(registration.getKey()));
                listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
                return;
            }

            var protocol = registrations.next();

            negotiationResult = NetworkComponentNegotiator.negotiate(List.of(), (PAYLOAD_REGISTRATIONS.get(protocol)).entrySet()
                                                                                                                     .stream()
                                                                                                                     .map((entry) -> new NegotiableNetworkComponent(entry.getKey(), (entry.getValue()).version(), (entry.getValue()).flow(), (entry.getValue()).optional()))
                                                                                                                     .toList());
            NCClient.STATE.setUnsafeEnvironment(false);
        } while (negotiationResult.success());

        if (!Config.CL_ALLOW_UNSAFE.get()) {
            var key = "neoforge.network.negotiation.failure.vanilla.server.not_supported";
            var fallback = "You are trying to connect to a server that is not running NeoForge, but you have mods that require it. A connection could not be established.";
            var msg = Component.translatableWithFallback(key, fallback, NeoForgeVersion.getVersion());
            listener.getConnection().disconnect(msg);
            return;
        }

        NetworkCompat.warn();
        NCClient.STATE.setUnsafeEnvironment(true);
    }

    /**
     * @author GB2022
     * @reason Send negotiation failure reasons even when the connection is allowed to continue.
     */
    @Overwrite
    public static void initializeNeoForgeConnection(ServerConfigurationPacketListener listener, Map<ConnectionProtocol, Set<ModdedNetworkQueryComponent>> clientChannels) {
        ChannelAttributes.setPayloadSetup(listener.getConnection(), NetworkPayloadSetup.empty());
        ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());

        var results = new IdentityHashMap<ConnectionProtocol, NegotiationResult>();
        var failureReasons = new HashMap<ResourceLocation, Component>();
        for (var protocol : PAYLOAD_REGISTRATIONS.keySet()) {
            var negotiationResult = NetworkComponentNegotiator.negotiate(PAYLOAD_REGISTRATIONS.get(protocol)
                                                                                              .values()
                                                                                              .stream()
                                                                                              .map(NegotiableNetworkComponent::new)
                                                                                              .toList(), clientChannels.getOrDefault(protocol, Collections.emptySet())
                                                                                                                       .stream()
                                                                                                                       .map(NegotiableNetworkComponent::new)
                                                                                                                       .toList());
            failureReasons.putAll(negotiationResult.failureReasons());

            if (!failureReasons.isEmpty()) {
                listener.send(new ModdedNetworkSetupFailedPayload(failureReasons));
            }

            if (!negotiationResult.success()) {
                listener.disconnect(Component.translatable("multiplayer.disconnect.incompatible", "NeoForge %s".formatted(NeoForgeVersion.getVersion())));
                return;
            }

            results.put(protocol, negotiationResult);
        }

        var setup = NetworkPayloadSetup.from(results);
        ChannelAttributes.setPayloadSetup(listener.getConnection(), setup);
        NetworkFilters.injectIfNecessary(listener.getConnection());
        listener.send(new ModdedNetworkPayload(setup));

        var nowListeningOn = ImmutableSet.<ResourceLocation>builder();
        nowListeningOn.addAll(getInitialListeningChannels(listener.flow()));
        nowListeningOn.addAll(setup.getChannels(ConnectionProtocol.CONFIGURATION).keySet());
        listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
    }


    @Inject(method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ClientCommonPacketListener;)V", at = @At("HEAD"), cancellable = true)
    private static void nc$before(Packet<?> packet, ClientCommonPacketListener listener, CallbackInfo ci) {
        ci.cancel();
        if (NCClient.STATE.isUnsafeEnvironment()) {
            ci.cancel();
        }
    }

    @Inject(method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V", at = @At("HEAD"), cancellable = true)
    private static void nc$before(Packet<?> packet, ServerCommonPacketListener listener, CallbackInfo ci) {
        ci.cancel();
        if (NCClient.STATE.isUnsafeEnvironment()) {
            ci.cancel();
        }
    }
}
