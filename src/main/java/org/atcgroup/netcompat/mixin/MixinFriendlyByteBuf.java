package org.atcgroup.netcompat.mixin;

import net.minecraft.network.FriendlyByteBuf;
import org.atcgroup.netcompat.NCClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public final class MixinFriendlyByteBuf {
    @Inject(method = "readBoolean", at = @At("HEAD"), cancellable = true)
    private void readBoolean(CallbackInfoReturnable<Boolean> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() <= 0) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "readByte", at = @At("HEAD"), cancellable = true)
    private void readByte(CallbackInfoReturnable<Byte> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() < 1) {
            cir.setReturnValue((byte) 0);
            cir.cancel();
        }
    }

    @Inject(method = "readShort", at = @At("HEAD"), cancellable = true)
    private void readShort(CallbackInfoReturnable<Short> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() < 2) {
            cir.setReturnValue((short) 0);
            cir.cancel();
        }
    }

    @Inject(method = "readFloat", at = @At("HEAD"), cancellable = true)
    private void readFloat(CallbackInfoReturnable<Float> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() < 4) {
            cir.setReturnValue(0.0f);
            cir.cancel();
        }
    }

    @Inject(method = "readDouble", at = @At("HEAD"), cancellable = true)
    private void readDouble(CallbackInfoReturnable<Double> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() < 8) {
            cir.setReturnValue(0.0d);
            cir.cancel();
        }
    }

    @Inject(method = "readInt", at = @At("HEAD"), cancellable = true)
    private void readInt(CallbackInfoReturnable<Integer> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() <= 1) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }

    @Inject(method = "readLong", at = @At("HEAD"), cancellable = true)
    private void readLong(CallbackInfoReturnable<Long> cir) {
        FriendlyByteBuf buf = (FriendlyByteBuf) (Object) this;

        if (NCClient.STATE.isUnsafeEnvironment() && buf.readableBytes() <= 1) {
            cir.setReturnValue(0L);
            cir.cancel();
        }
    }
}
