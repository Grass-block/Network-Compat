package org.atcgroup.netcompat.mixin;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.atcgroup.netcompat.NCClient;
import org.atcgroup.netcompat.NetworkCompat;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketEncoder.class)
public class MixinPacketEncoder<T extends PacketListener> {
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
    protected void encode(ChannelHandlerContext ctx, Packet<T> packet, ByteBuf buffer) throws Exception {
        var packettype = packet.type();

        try {
            this.protocolInfo.codec().encode(buffer, packet);
            var i = buffer.readableBytes();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(Connection.PACKET_SENT_MARKER, "OUT: [{}:{}] {} -> {} bytes", this.protocolInfo.id()
                                                                                                            .id(), packettype, packet.getClass()
                                                                                                                                     .getName(), i);
            }

            JvmProfiler.INSTANCE.onPacketSent(this.protocolInfo.id(), packettype, ctx.channel().remoteAddress(), i);
        } catch (Throwable e) {
            if (NCClient.STATE.isUnsafeEnvironment()) {
                NetworkCompat.LOGGER.warn("Suppressed packet encoding: {} ({})", packettype, e.getMessage());
                return;
            }

            LOGGER.error("Error sending packet {}", packettype, e);
            if (packet.isSkippable()) {
                throw new SkipPacketException(e);
            }

            throw e;
        } finally {
            ProtocolSwapHandler.handleOutboundTerminalPacket(ctx, packet);
        }
    }
}
