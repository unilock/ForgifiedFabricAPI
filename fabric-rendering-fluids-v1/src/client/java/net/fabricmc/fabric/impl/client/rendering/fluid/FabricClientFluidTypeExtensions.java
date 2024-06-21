package net.fabricmc.fabric.impl.client.rendering.fluid;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

public record FabricClientFluidTypeExtensions(Fluid fluid, FluidRenderHandler handler) implements IClientFluidTypeExtensions {
    private TextureAtlasSprite[] getSprites() {
        return handler.getFluidSprites(null, null, fluid.defaultFluidState());
    }

    @Override
    public ResourceLocation getStillTexture() {
        TextureAtlasSprite[] sprites = getSprites();
        return sprites[0].contents().name();
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        TextureAtlasSprite[] sprites = getSprites();
        return sprites[1].contents().name();
    }

    @Nullable
    @Override
    public ResourceLocation getOverlayTexture() {
        TextureAtlasSprite[] sprites = getSprites();
        return sprites.length > 2 ? sprites[2].contents().name() : null;
    }

    @Override
    public int getTintColor() {
        int baseColor = handler.getFluidColor(null, null, fluid.defaultFluidState());
        return 0xFF000000 | baseColor;
    }

    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        int baseColor = handler.getFluidColor(getter, pos, state);
        return 0xFF000000 | baseColor;
    }
}
