package org.atcgroup.netcompat.mixin.client;

import com.google.common.collect.Sets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsPayload;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsReplyPayload;
import net.neoforged.neoforge.registries.ClientRegistryManager;
import net.neoforged.neoforge.registries.RegistryManager;
import org.atcgroup.netcompat.Config;
import org.atcgroup.netcompat.NetworkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientRegistryManager.class)
public class MixinClientRegistryManager {

    /**
     * @author GB2022
     * @reason Remove ERRORS
     */
    @Overwrite
    public static void handleKnownDataMaps(final KnownRegistryDataMapsPayload payload, final IPayloadContext context) {
        record MandatoryEntry(ResourceKey<? extends Registry<?>> registry, ResourceLocation id) {
        }
        final Set<MandatoryEntry> ourMandatory = new HashSet<>();
        RegistryManager.getDataMaps().forEach((reg, values) -> values.values().forEach(attach -> {
            if (attach.mandatorySync()) {
                ourMandatory.add(new MandatoryEntry(reg, attach.id()));
            }
        }));

        final Set<MandatoryEntry> theirMandatory = new HashSet<>();
        payload.dataMaps().forEach((reg, values) -> values.forEach(attach -> {
            if (attach.mandatory()) {
                theirMandatory.add(new MandatoryEntry(reg, attach.id()));
            }
        }));

        final List<Component> messages = new ArrayList<>();
        final var missingOur = Sets.difference(ourMandatory, theirMandatory);
        if (!missingOur.isEmpty()) {
            messages.add(Component.translatable("neoforge.network.data_maps.missing_our", Component.literal(missingOur.stream()
                                                                                                                      .map(e -> e.id() + " (" + e.registry()
                                                                                                                                                 .location() + ")")
                                                                                                                      .collect(Collectors.joining(", ")))
                                                                                                   .withStyle(ChatFormatting.GOLD)));
        }

        final var missingTheir = Sets.difference(theirMandatory, ourMandatory);
        if (!missingTheir.isEmpty()) {
            messages.add(Component.translatable("neoforge.network.data_maps.missing_their", Component.literal(missingTheir.stream()
                                                                                                                          .map(e -> e.id() + " (" + e.registry()
                                                                                                                                                     .location() + ")")
                                                                                                                          .collect(Collectors.joining(", ")))
                                                                                                     .withStyle(ChatFormatting.GOLD)));
        }

        if (!messages.isEmpty()) {
            MutableComponent message = Component.empty();
            final var itr = messages.iterator();
            while (itr.hasNext()) {
                message = message.append(itr.next());
                if (itr.hasNext()) {
                    message = message.append("\n");
                }
            }

            if (!Config.CL_ALLOW_UNSAFE.get()) {
                context.disconnect(message);
                return;
            }

            NetworkCompat.LOGGER.warn("Connecting with missing registry: \n our: {}\ntheir: {}", missingOur, missingTheir);
        }

        final var known = new HashMap<ResourceKey<? extends Registry<?>>, Collection<ResourceLocation>>();
        RegistryManager.getDataMaps().forEach((key, vals) -> known.put(key, vals.keySet()));
        context.reply(new KnownRegistryDataMapsReplyPayload(known));
    }
}
