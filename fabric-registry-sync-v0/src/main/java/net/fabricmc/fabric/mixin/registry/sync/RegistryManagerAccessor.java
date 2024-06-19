package net.fabricmc.fabric.mixin.registry.sync;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryManager.class)
public interface RegistryManagerAccessor {
    @Invoker
    static void invokeTrackModdedRegistry(ResourceLocation registry) {
        throw new UnsupportedOperationException();
    }
}
