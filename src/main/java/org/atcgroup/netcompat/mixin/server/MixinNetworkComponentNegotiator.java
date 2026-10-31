package org.atcgroup.netcompat.mixin.server;

import com.google.common.collect.HashBasedTable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiatedNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import org.atcgroup.netcompat.Config;
import org.atcgroup.netcompat.NetworkCompat;
import org.atcgroup.netcompat.NCClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
@Mixin(NetworkComponentNegotiator.class)
@OnlyIn(Dist.DEDICATED_SERVER)
public abstract class MixinNetworkComponentNegotiator {
    @Shadow
    private static List<NegotiableNetworkComponent> buildDisabledOptionalComponents(List<NegotiableNetworkComponent> currentSide, List<NegotiableNetworkComponent> otherSide) {
        throw new AssertionError();
    }

    @Shadow
    public static Optional<NetworkComponentNegotiator.ComponentNegotiationResult> validateComponent(NegotiableNetworkComponent left, NegotiableNetworkComponent right, String requestingSide) {
        throw new AssertionError();
    }

    /**
     * @author GB2022
     * @reason Modify strategy of missing on server from "fail" to "warn".
     */
    @Overwrite
    public static NegotiationResult negotiate(List<NegotiableNetworkComponent> serverComponents, List<NegotiableNetworkComponent> clientComponents) {
        var matches = HashBasedTable.<ResourceLocation, NegotiableNetworkComponent, NegotiableNetworkComponent>create();
        var server = new ArrayList<>(serverComponents);
        var client = new ArrayList<>(clientComponents);

        var disabledOptionalOnClient = buildDisabledOptionalComponents(client, server);
        var disabledOptionalOnServer = buildDisabledOptionalComponents(server, client);
        var failedComponents = new ArrayList<ResourceLocation>();

        client.removeAll(disabledOptionalOnClient);
        server.removeAll(disabledOptionalOnServer);

        server.forEach(serverComponent -> client.forEach(clientComponent -> {
            if (serverComponent.id().equals(clientComponent.id())) {
                matches.put(serverComponent.id(), serverComponent, clientComponent);
            }
        }));

        client.removeIf(component -> matches.containsRow(component.id()));
        server.removeIf(component -> matches.containsRow(component.id()));

        //================[START MODIFICATION]================
        if (!client.isEmpty()) {
            if (!Config.SV_ALLOW_UNSAFE.get()) {
                //use vanilla impl
                var failureReasons = new HashMap<ResourceLocation, Component>();
                client.forEach((c) -> {
                    var channelFailureReason = Component.translatable("neoforge.network.negotiation.failure.missing.client.server");
                    var modDisplayName = ModList.get()
                                                .getModContainerById(c.id().getNamespace())
                                                .map((mc) -> mc.getModInfo().getDisplayName())
                                                .orElse("");
                    failureReasons.put(c.id(), modDisplayName.isEmpty() ? channelFailureReason : Component.translatable("neoforge.network.negotiation.failure.mod", new Object[]{modDisplayName, channelFailureReason}));
                });
                return new NegotiationResult(List.of(), false, failureReasons);
            }

            client.forEach(component -> failedComponents.add(component.id()));

            //SEND FAILED CHANNEL TO CLIENT

            NetworkCompat.LOGGER.info("Suppressing {} Server-missing channel: {}", failedComponents.size(), failedComponents);
        }
        //================[ END MODIFICATION ]================

        if (!server.isEmpty()) {
            var failureReasons = new HashMap<ResourceLocation, Component>();
            server.forEach(component -> {
                var reason = Component.translatable("neoforge.network.negotiation.failure.missing.server.client");
                var modDisplayName = ModList.get()
                                            .getModContainerById(component.id().getNamespace())
                                            .map(container -> container.getModInfo().getDisplayName())
                                            .orElse("");
                failureReasons.put(component.id(), modDisplayName.isEmpty() ? reason : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName, reason));
            });
            return new NegotiationResult(List.of(), false, failureReasons);
        }

        var result = new ArrayList<NegotiatedNetworkComponent>();
        var failureReasons = new HashMap<ResourceLocation, Component>();
        for (var match : matches.cellSet()) {
            var serverComponent = match.getColumnKey();
            var clientComponent = match.getValue();
            var modDisplayName = ModList.get()
                                        .getModContainerById(serverComponent.id().getNamespace())
                                        .map(container -> container.getModInfo().getDisplayName())
                                        .orElse("");

            var serverToClient = validateComponent(serverComponent, clientComponent, "client");
            if (serverToClient.isPresent() && !serverToClient.get().success()) {
                var reason = serverToClient.get().failureReason();
                failureReasons.put(serverComponent.id(), modDisplayName.isEmpty() ? reason : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName, reason));
                continue;
            }

            var clientToServer = validateComponent(clientComponent, serverComponent, "server");
            if (clientToServer.isPresent() && !clientToServer.get().success()) {
                var reason = clientToServer.get().failureReason();
                failureReasons.put(serverComponent.id(), modDisplayName.isEmpty() ? reason : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName, reason));
                continue;
            }

            result.add(new NegotiatedNetworkComponent(serverComponent.id(), serverComponent.version()));
        }

        if (failureReasons.isEmpty()) {
            if (!failedComponents.isEmpty()) {
                for (var component : failedComponents) {
                    failureReasons.put(component, Component.literal(NCClient.NOTIFY_MOD_NOT_EXIST));
                }
            }
            return new NegotiationResult(result, true, failureReasons);
        }
        return new NegotiationResult(List.of(), false, failureReasons);
    }
}
