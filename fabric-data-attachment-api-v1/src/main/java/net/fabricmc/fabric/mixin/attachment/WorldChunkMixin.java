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

package net.fabricmc.fabric.mixin.attachment;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.impl.attachment.AttachmentEntrypoint;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public class WorldChunkMixin {
	@WrapOperation(
			method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",
			at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/attachment/AttachmentInternals;copyChunkAttachmentsOnPromotion(Lnet/minecraft/core/HolderLookup$Provider;Lnet/neoforged/neoforge/attachment/AttachmentHolder$AsField;Lnet/neoforged/neoforge/attachment/AttachmentHolder$AsField;)V")
	)
	private void transferProtoChunkAttachement(HolderLookup.Provider provider, AttachmentHolder.AsField from, AttachmentHolder.AsField to, Operation<Void> operation) {
		operation.call(provider, from, to);
		AttachmentEntrypoint.transfer(from, to, true, false);
	}
}
