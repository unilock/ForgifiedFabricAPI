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

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.impl.resource.loader.FabricResourcePackProfile;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackCreator;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(PackRepository.class)
public abstract class ResourcePackManagerMixin {
	@Unique
	private static final Logger LOGGER = LoggerFactory.getLogger("ResourcePackManagerMixin");

	@Shadow
	private Map<String, Pack> available;

	@Inject(method = "rebuildSelected", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;", shift = At.Shift.BEFORE))
	private void handleAutoEnableDisable(Collection<String> enabledNames, CallbackInfoReturnable<List<Pack>> cir, @Local List<Pack> enabledAfterFirstRun) {
		ModResourcePackUtil.refreshAutoEnabledPacks(enabledAfterFirstRun, this.available);
	}

	@Inject(method = "addPack", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER))
	private void handleAutoEnable(String profile, CallbackInfoReturnable<Boolean> cir, @Local List<Pack> newlyEnabled) {
		if (ModResourcePackCreator.POST_CHANGE_HANDLE_REQUIRED.contains(profile)) {
			ModResourcePackUtil.refreshAutoEnabledPacks(newlyEnabled, this.available);
		}
	}

	@Inject(method = "removePack", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"))
	private void handleAutoDisable(String profile, CallbackInfoReturnable<Boolean> cir, @Local List<Pack> enabled) {
		if (ModResourcePackCreator.POST_CHANGE_HANDLE_REQUIRED.contains(profile)) {
			Set<String> currentlyEnabled = enabled.stream().map(Pack::getId).collect(Collectors.toSet());
			enabled.removeIf(p -> !((FabricResourcePackProfile) p).fabric_parentsEnabled(currentlyEnabled));
			LOGGER.debug("[Fabric] Internal pack auto-removed upon disabling {}, result: {}", profile, enabled.stream().map(Pack::getId).toList());
		}
	}
}
