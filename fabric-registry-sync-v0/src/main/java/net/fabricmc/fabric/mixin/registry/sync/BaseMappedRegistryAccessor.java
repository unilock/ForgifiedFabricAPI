package net.fabricmc.fabric.mixin.registry.sync;

import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseMappedRegistry.class)
public interface BaseMappedRegistryAccessor {
    @Accessor
    void setSync(boolean sync);

    @Invoker
    void invokeUnfreeze();
}
