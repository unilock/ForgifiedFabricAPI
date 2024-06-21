package net.fabricmc.fabric.mixin.client.rendering.fluid;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.impl.client.rendering.fluid.FabricClientFluidTypeExtensions;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IClientFluidTypeExtensions.class)
public interface IClientFluidTypeExtensionsMixin {

    @ModifyReturnValue(method = "of(Lnet/minecraft/world/level/material/FluidState;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;", at = @At("RETURN"))
    private static IClientFluidTypeExtensions of(IClientFluidTypeExtensions ret, FluidState state) {
        if (ret == IClientFluidTypeExtensions.DEFAULT) {
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.getOverride(state.getType());
            if (handler != null) {
                return new FabricClientFluidTypeExtensions(state.getType(), handler);
            }
        }
        return ret;
    }

    @ModifyReturnValue(method = "of(Lnet/minecraft/world/level/material/Fluid;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;", at = @At("RETURN"))
    private static IClientFluidTypeExtensions of(IClientFluidTypeExtensions ret, Fluid fluid) {
        if (ret == IClientFluidTypeExtensions.DEFAULT) {
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.getOverride(fluid);
            if (handler != null) {
                return new FabricClientFluidTypeExtensions(fluid, handler);
            }
        }
        return ret;
    }
}
