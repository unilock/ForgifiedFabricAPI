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

package net.fabricmc.fabric.mixin.resource.loader;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.Resource;

@Mixin(RegistryDataLoader.class)
public class RegistryLoaderMixin {
	@Unique
	private static final RegistrationInfo MOD_PROVIDED_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.stable());

	// On the server side, loading mod-provided DRM entries should not show experiments screen.
	// While the lifecycle is set to experimental on the client side (a de-sync),
	// there is no good way to fix this without breaking protocol; the lifecycle seems to be unused on
	// the client side anyway.
	@ModifyExpressionValue(method = "loadContentsFromManager(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/RegistryOps$RegistryInfoLookup;Lnet/minecraft/core/WritableRegistry;Lcom/mojang/serialization/Decoder;Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object markModProvidedAsStable(Object original, @Local Resource resource) {
		if (original instanceof RegistrationInfo info && info.knownPackInfo().isEmpty() && resource.source() instanceof ModResourcePack) {
			return MOD_PROVIDED_INFO;
		}

		return original;
	}
}
