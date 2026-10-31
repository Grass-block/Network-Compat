package org.atcgroup.netcompat.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Mixin(Registry.class)
public interface MixinRegistry<T> {

    @Shadow
    @Nullable
    T get(@org.jetbrains.annotations.Nullable ResourceKey<T> resourceKey);

    @Shadow
    ResourceKey<? extends Registry<T>> key();

    @Shadow Optional<Holder.Reference<T>> getAny();

    /**
     * @author GrassBlock2022
     * @reason Add default value to registry.
     */
    @Overwrite
    default T getOrThrow(ResourceKey<T> key) {
        T t = get(key);
        return Objects.requireNonNullElseGet(t, () -> getAny().orElseThrow().value());
    }
}
