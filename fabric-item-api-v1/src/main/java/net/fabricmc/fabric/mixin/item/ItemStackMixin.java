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

package net.fabricmc.fabric.mixin.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.impl.item.ItemExtensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements FabricItemStack {
	@Shadow public abstract Item getItem();

	@WrapOperation(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/Runnable;)V"))
	private void hookDamage(ItemStack instance, int amount, RandomSource random, ServerPlayer serverPlayerEntity, Runnable runnable, Operation<Void> original, @Local(argsOnly = true) EquipmentSlot slot) {
		CustomDamageHandler handler = ((ItemExtensions) getItem()).fabric_getCustomDamageHandler();

		if (handler != null) {
			// Track whether an item has been broken by custom handler
			MutableBoolean mut = new MutableBoolean(false);
			amount = handler.damage((ItemStack) (Object) this, amount, serverPlayerEntity, slot, () -> {
				mut.setTrue();
				runnable.run();
			});

			// If item is broken, there's no reason to call the original.
			if (mut.booleanValue()) return;
		}

		original.call(instance, amount, random, serverPlayerEntity, runnable);
	}
}
