package org.atcgroup.netcompat.mixin.client;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.atcgroup.netcompat.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientIntentionPacket.class)
public class MixinClientIntentionPacket {

    @ModifyVariable(
            method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static String modifyHostName(String hostName) {
        return Config.CL_VHOST.get();
    }
}