package org.atcgroup.netcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import org.atcgroup.netcompat.NCClient;
import org.atcgroup.netcompat.NetworkCompat;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(PacketDecoder.class)
public class MixinPacketDecoder {

    @WrapMethod(method = "decode")
    private void nc$ignoreDecodeException(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, Operation<Void> original) {
        try {
            original.call(ctx, in, out);
        } catch (Throwable t) {
            if (NCClient.STATE.isUnsafeEnvironment()) {
                NetworkCompat.LOGGER.warn("Suppressed packet decoding: {}", in);
                return;
            }

            throw t;
        }
    }
}
