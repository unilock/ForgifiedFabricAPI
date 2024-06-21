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

package net.fabricmc.fabric.impl.client.rendering;

import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ColorResolverRegistryImpl {
	// Does not include vanilla resolvers
	private static final Set<ColorResolver> CUSTOM_RESOLVERS = new HashSet<>();
	private static final Set<ColorResolver> CUSTOM_RESOLVERS_VIEW = Collections.unmodifiableSet(CUSTOM_RESOLVERS);

	private ColorResolverRegistryImpl() {
	}

	public static void register(ColorResolver resolver) {
		CUSTOM_RESOLVERS.add(resolver);
	}

	@UnmodifiableView
	public static Set<ColorResolver> getCustomResolvers() {
		return CUSTOM_RESOLVERS_VIEW;
	}
}
