package net.fabricmc.fabric.mixin.block;

import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IBlockExtension.class)
public interface IBlockExtensionMixin extends FabricBlock {

}
