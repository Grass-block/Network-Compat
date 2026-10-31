package org.atcgroup.netcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientState {
    private final Map<ResourceLocation, Boolean> cachedInvalidItems = new ConcurrentHashMap<>();
    private final Set<String> notPresentMods = new HashSet<>();
    private boolean isUnsafeEnvironment = false;
    private boolean isVanillaConnection = false;

    public void setNotPresentMods(HashSet<ResourceLocation> mods) {
        this.cachedInvalidItems.clear();
        this.notPresentMods.clear();
        for (var id : mods) {
            this.notPresentMods.add(id.getNamespace());
        }
    }

    public void setUnsafeEnvironment(boolean unsafeEnvironment) {
        this.isUnsafeEnvironment = unsafeEnvironment;
    }

    public void setVanillaConnection(boolean vanillaConnection) {
        this.isVanillaConnection = vanillaConnection;
    }

    public boolean isUnsafeEnvironment() {
        return isUnsafeEnvironment;
    }

    public boolean isVanillaConnection() {
        return isVanillaConnection;
    }

    public Map<ResourceLocation, Boolean> getCachedInvalidItems() {
        return cachedInvalidItems;
    }

    public Set<String> getNotPresentMods() {
        return notPresentMods;
    }

    public boolean isUnsafeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (this.isVanillaConnection) {
            return !key.getNamespace().equals("minecraft");
        }

        if (this.cachedInvalidItems.containsKey(key)) {
            return this.cachedInvalidItems.get(key);
        }

        var b = this.notPresentMods.contains(key.getNamespace());
        this.cachedInvalidItems.put(key, b);

        NetworkCompat.LOGGER.info("Item validate: {} -> {}", key, b);

        return b;
    }
}
