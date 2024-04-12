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

package net.fabricmc.fabric.mixin.item.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;

@Mixin(value = ClientPlayerInteractionManager.class, priority = 1500)
public class ClientPlayerInteractionManagerMixin {
	@Shadow
	@Final
	private MinecraftClient client;
	@Shadow
	private ItemStack selectedStack;
	
	@Unique
	private Boolean fabric_allowContinueBlockBreaking; 

	/**
	 * Allows a FabricItem to continue block breaking progress even if the count or nbt changed.
	 * For this, we inject after vanilla decided that the stack was "not unchanged", and we set if back to "unchanged"
	 * if the item wishes to continue mining.
	 */
	@Redirect(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;canCombine(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"
			),
			method = "isCurrentlyBreaking",
			require = 0 // Optional on NeoForge
	)
	private boolean fabricItemContinueBlockBreakingInject(ItemStack stack, ItemStack otherStack) {
		this.fabric_allowContinueBlockBreaking = false;

		boolean stackUnchanged = ItemStack.canCombine(stack, this.selectedStack);

		if (!stackUnchanged) {
			// The stack changed and vanilla is about to cancel block breaking progress. Check if the item wants to continue block breaking instead.
			ItemStack oldStack = this.selectedStack;
			ItemStack newStack = this.client.player.getMainHandStack();

			if (oldStack.isOf(newStack.getItem()) && oldStack.getItem().allowContinuingBlockBreaking(this.client.player, oldStack, newStack)) {
				stackUnchanged = true;
				fabric_allowContinueBlockBreaking = true;
			}
		}

		return stackUnchanged;
	}

	@WrapOperation(
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;shouldCauseBlockBreakReset(Lnet/minecraft/item/ItemStack;)Z"
				),
			method = "isCurrentlyBreaking"
	)
	private boolean fabricItemContinueBlockBreakingInjectForge(ItemStack stack, ItemStack otherStack, Operation<Boolean> original) {
		if (this.fabric_allowContinueBlockBreaking == null) {
			boolean stackChanged = original.call(stack, otherStack);
			if (stackChanged) {
				// The stack changed and vanilla is about to cancel block breaking progress. Check if the item wants to continue block breaking instead.
				ItemStack oldStack = this.selectedStack;
				ItemStack newStack = this.client.player.getMainHandStack();

				if (oldStack.isOf(newStack.getItem()) && oldStack.getItem().allowContinuingBlockBreaking(this.client.player, oldStack, newStack)) {
					return false; // Allow continue block breaking
				}
			}
			return stackChanged;
		}
		return !this.fabric_allowContinueBlockBreaking && original.call(stack, otherStack);
	}
}
