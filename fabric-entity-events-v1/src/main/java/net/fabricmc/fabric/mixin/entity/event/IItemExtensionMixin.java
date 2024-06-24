package net.fabricmc.fabric.mixin.entity.event;

import net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IItemExtension.class)
public interface IItemExtensionMixin {

    @Inject(method = "canElytraFly", at = @At("HEAD"), cancellable = true)
    default void canElytraFly(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this instanceof FabricElytraItem);
    }
}
