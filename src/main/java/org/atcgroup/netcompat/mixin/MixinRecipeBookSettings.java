package org.atcgroup.netcompat.mixin;

import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.atcgroup.netcompat.NetworkCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RecipeBookSettings.class)
public class MixinRecipeBookSettings {
    @Shadow
    @Final
    public Map<RecipeBookType, RecipeBookSettings.TypeSettings> states;

    /**
     * @author GB2022
     * @reason Fix missing values
     */
    @Overwrite
    public void replaceFrom(RecipeBookSettings other) {
        this.states.clear();
        var types = RecipeBookType.values();

        for (var recipebooktype : types) {
            var otherSetting = other.states.get(recipebooktype);

            if (otherSetting == null) {
                NetworkCompat.LOGGER.warn("Ignored invalid RecipeBookSetting: {}", recipebooktype.name());
                continue;
            }

            this.states.put(recipebooktype, otherSetting.copy());
        }
    }
}
