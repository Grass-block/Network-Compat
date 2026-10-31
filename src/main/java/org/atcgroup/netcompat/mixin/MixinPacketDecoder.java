package org.atcgroup.netcompat.mixin;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.atcgroup.netcompat.NetworkCompat;
import org.atcgroup.netcompat.NCClient;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.List;

@Mixin(PacketDecoder.class)
public class MixinPacketDecoder<T extends PacketListener> {
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();

    @Final
    @Shadow
    private ProtocolInfo<T> protocolInfo;

    /**
     * @author GB2022
     * @reason Suppressing packet error by simply ignoring them.
     */
    @Overwrite
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
        int i = in.readableBytes();
        if (i != 0) {
            try {
                var id = this.protocolInfo.id();
                var packet = this.protocolInfo.codec().decode(in);
                var type = packet.type();

                JvmProfiler.INSTANCE.onPacketReceived(id, type, context.channel().remoteAddress(), i);

                if (in.readableBytes() > 0) {
                    var error = "Packet %s/%s(%s) was larger than I expected, found %s bytes bytes extra whilst reading packet %s.";
                    throw new IOException(error.formatted(id.id(), type, packet.getClass()
                                                                               .getSimpleName(), in.readableBytes(), type));
                }

                out.add(packet);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(Connection.PACKET_RECEIVED_MARKER, " IN: [{}:{}] {} -> {} bytes", id.id(), type, packet.getClass()
                                                                                                                        .getName(), i);
                }

                ProtocolSwapHandler.handleInboundTerminalPacket(context, packet);
            } catch (Throwable t) {
                if (NCClient.STATE.isUnsafeEnvironment()) {
                    NetworkCompat.LOGGER.warn("Suppressed packet decoding: {}", i);
                    return;
                }

                throw t;
            }
        }
    }
}
