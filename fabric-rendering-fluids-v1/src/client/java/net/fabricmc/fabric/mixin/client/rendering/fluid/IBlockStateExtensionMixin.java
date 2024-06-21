package net.fabricmc.fabric.mixin.client.rendering.fluid;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IBlockStateExtension.class)
public interface IBlockStateExtensionMixin {

    @Inject(method = "shouldDisplayFluidOverlay", at = @At("HEAD"), cancellable = true)
    default void shouldDisplayFluidOverlay(BlockAndTintGetter level, BlockPos pos, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent((BlockState) this, level, pos, fluidState));
    }
}
