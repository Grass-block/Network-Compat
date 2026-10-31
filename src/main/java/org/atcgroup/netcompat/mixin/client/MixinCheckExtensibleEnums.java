package org.atcgroup.netcompat.mixin.client;

import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.configuration.CheckExtensibleEnums;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.ExtensibleEnumAcknowledgePayload;
import net.neoforged.neoforge.network.payload.ExtensibleEnumDataPayload;
import org.atcgroup.netcompat.Config;
import org.atcgroup.netcompat.NCClient;
import org.atcgroup.netcompat.NetworkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(CheckExtensibleEnums.class)
public abstract class MixinCheckExtensibleEnums {
    @Shadow
    private static synchronized Map<String, CheckExtensibleEnums.EnumEntry> getEnumEntries() {
        throw new AssertionError();
    }

    /**
     * @author GB2022
     * @reason Allow configured unsafe connections while keeping the extensible-enum handshake moving.
     */
    @Overwrite
    public static void handleClientboundPayload(ExtensibleEnumDataPayload payload, IPayloadContext context) {
        Map<String, CheckExtensibleEnums.EnumEntry> localEntries = getEnumEntries();
        Map<String, CheckExtensibleEnums.EnumEntry> remoteEntries = payload.enumEntries();
        Map<String, String> mismatches = new HashMap<>();

        for (String className : Sets.union(localEntries.keySet(), remoteEntries.keySet())) {
            CheckExtensibleEnums.EnumEntry local = localEntries.get(className);
            CheckExtensibleEnums.EnumEntry remote = remoteEntries.get(className);

            if ((local == null && remote.isExtended()) || (remote == null && local.isExtended())) {
                mismatches.put(className, remote == null
                        ? "Enum is extensible on the client but not on the server"
                        : "Enum is extensible on the server but not on the client");
                continue;
            }

            if ((local == null || !local.isExtended()) && (remote == null || !remote.isExtended())) {
                continue;
            }

            if (local.networkCheck() != remote.networkCheck()) {
                mismatches.put(className, "Mismatched NetworkCheck (server: " + remote.networkCheck()
                        + ", client: " + local.networkCheck() + ")");
                continue;
            }

            if (local.isExtended() != remote.isExtended()) {
                mismatches.put(className, remote.isExtended()
                        ? "Enum has additional entries on the server but not on the client"
                        : "Enum has additional entries on the client but not on the server");
                continue;
            }

            CheckExtensibleEnums.ExtensionData localData = local.data().orElseThrow();
            CheckExtensibleEnums.ExtensionData remoteData = remote.data().orElseThrow();
            if (localData.vanillaCount() != remoteData.vanillaCount()
                    || localData.totalCount() != remoteData.totalCount()) {
                mismatches.put(className, "Set of entries does not match (server: " + remoteData.entries()
                        + ", client: " + localData.entries() + ")");
                continue;
            }

            List<String> localValues = localData.entries();
            List<String> remoteValues = remoteData.entries();
            for (int i = 0; i < localData.totalCount() - localData.vanillaCount(); i++) {
                if (!localValues.get(i).equals(remoteValues.get(i))) {
                    mismatches.put(className, "Set of entries does not match (server: " + remoteValues
                            + ", client: " + localValues + ")");
                    break;
                }
            }
        }

        if (!mismatches.isEmpty()) {
            StringBuilder message = new StringBuilder(
                    "The configuration or set of values added to extensible enums on the client and server do not match");
            mismatches.forEach((className, reason) -> message.append("\n").append(className).append(": ").append(reason));

            if (!Config.CL_ALLOW_UNSAFE.get()) {
                context.disconnect(Component.translatable("neoforge.network.extensible_enums.enum_entry_mismatch"));
                NetworkCompat.LOGGER.warn(message.toString());
                return;
            }

            NCClient.STATE.setUnsafeEnvironment(true);
            NetworkCompat.LOGGER.warn("Allowing connection in unsafe mode bypassing extensible enum mismatches:{}", message);
        }

        context.reply(ExtensibleEnumAcknowledgePayload.INSTANCE);
    }
}
