/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.client.rendering.fluid;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerInfo;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingImpl;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class FluidRendererMixin {
	@Final
	@Shadow
	private TextureAtlasSprite[] lavaIcons;
	@Final
	@Shadow
	private TextureAtlasSprite[] waterIcons;
	@Shadow
	private TextureAtlasSprite waterOverlay;

	@Inject(at = @At("RETURN"), method = "setupSprites")
	public void onResourceReloadReturn(CallbackInfo info) {
		LiquidBlockRenderer self = (LiquidBlockRenderer) (Object) this;
		((FluidRenderHandlerRegistryImpl) FluidRenderHandlerRegistry.INSTANCE).onFluidRendererReload(self, waterIcons, lavaIcons, waterOverlay);
	}

	@Inject(at = @At("HEAD"), method = "tesselate", cancellable = true)
	public void onHeadRender(BlockAndTintGetter view, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
		FluidRenderHandlerInfo info = FluidRenderingImpl.getCurrentInfo();

		if (info.handler == null) {
			FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());

			if (handler != null) {
				handler.renderFluid(pos, view, vertexConsumer, blockState, fluidState);
				ci.cancel();
			}
		}
	}
}
