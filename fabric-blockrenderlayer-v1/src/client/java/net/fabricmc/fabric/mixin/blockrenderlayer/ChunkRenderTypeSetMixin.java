package net.fabricmc.fabric.mixin.blockrenderlayer;

import net.fabricmc.fabric.impl.blockrenderlayer.ExtendedChunkRenderTypeSet;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;

@Mixin(ChunkRenderTypeSet.class)
public abstract class ChunkRenderTypeSetMixin implements ExtendedChunkRenderTypeSet {
	@Shadow
	public abstract boolean isEmpty();

	@Shadow
	@Final
	private BitSet bits;

	@Shadow
	@Final
	private static RenderType[] CHUNK_RENDER_TYPES;

	@Override
	public RenderType sinytra$firstLayer() {
		if(isEmpty())
			return RenderType.solid();
		return CHUNK_RENDER_TYPES[bits.nextSetBit(0)];
	}
}
