package org.atcgroup.netcompat.mixin.client;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import org.atcgroup.netcompat.NCClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeModeTab.ItemDisplayParameters.class)
public class MixinItemDisplayParameters {

    @Final
    @Shadow
    private FeatureFlagSet enabledFeatures;

    @Final
    @Shadow
    private boolean hasPermissions;

    @Final
    @Shadow
    private HolderLookup.Provider holders;

    /**
     * @author GrassBlock2022
     * @reason Enforce inventory rebuild
     */
    @Overwrite
    public boolean needsUpdate(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
        if(NCClient.NEED_INV_REBUILD.get()){
            NCClient.NEED_INV_REBUILD.set(false);
            return true;
        }

        return !this.enabledFeatures.equals(enabledFeatures) || this.hasPermissions != hasPermissions || this.holders != holders;
    }
}
