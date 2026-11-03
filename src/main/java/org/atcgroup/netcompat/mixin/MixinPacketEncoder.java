package org.atcgroup.netcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import org.atcgroup.netcompat.NCClient;
import org.atcgroup.netcompat.NetworkCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketEncoder.class)
public class MixinPacketEncoder<T extends PacketListener> {

    @WrapMethod(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V")
    private void nc$ignoreDecodeException(ChannelHandlerContext ctx, Packet<T> packet, ByteBuf buffer, Operation<Void> original) {
        try {
            original.call(ctx, packet, buffer);
        } catch (Throwable t) {
            if (NCClient.STATE.isUnsafeEnvironment()) {
                NetworkCompat.LOGGER.warn("Suppressed packet decoding: {}", packet);
                return;
            }

            throw t;
        }
    }
}
