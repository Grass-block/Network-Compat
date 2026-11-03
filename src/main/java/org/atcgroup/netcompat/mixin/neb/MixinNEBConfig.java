package org.atcgroup.netcompat.mixin.neb;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthLegacyConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.atcgroup.netcompat.NCClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Objects;

@Mixin(NotEnoughBandwidthLegacyConfig.class)
@OnlyIn(Dist.CLIENT)
public class MixinNEBConfig {
    @Final
    @Shadow
    public static HashSet<String> COMMON_BYPASS_LIST;

    @Final
    @Shadow
    public static HashSet<String> COMMON_BLACK_LIST;

    @Final
    @Shadow
    public static HashSet<String> COMMON_FUZZY_BLACK_LIST;

    /**
     * @author GB2022
     * @reason Enable blacklist
     */
    @Overwrite
    public static boolean bypassType(String type) {
        if (NCClient.STATE.isVanillaConnection()) {
            return true;
        }
        return COMMON_BYPASS_LIST.contains(type);
    }

    /**
     * @author GB2022
     * @reason Enable blacklist
     */
    @Overwrite
    public static boolean skipType(String type) {
        if (NCClient.STATE.isVanillaConnection()) {
            return true;
        }

        var cfg = NotEnoughBandwidthLegacyConfig.get();
        if (!COMMON_BLACK_LIST.contains(type)) {
            var stream = COMMON_FUZZY_BLACK_LIST.stream();
            Objects.requireNonNull(type);
            if (!stream.anyMatch(type::contains) && (!cfg.compatibleMode || !cfg.blackList.contains(type))) {
                return false;
            }
        }

        return true;
    }
}
