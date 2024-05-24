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

package net.fabricmc.fabric.api.datagen.v1.provider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl;

/**
 * A provider to help with data-generation of dynamic registry objects,
 * such as biomes, features, or message types.
 */
public abstract class FabricDynamicRegistryProvider implements DataProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(FabricDynamicRegistryProvider.class);

	private final FabricDataOutput output;
	private final CompletableFuture<HolderLookup.Provider> registriesFuture;

	public FabricDynamicRegistryProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	protected abstract void configure(HolderLookup.Provider registries, Entries entries);

	public static final class Entries {
		private final HolderLookup.Provider registries;
		// Registry ID -> Entries for that registry
		private final Map<ResourceLocation, RegistryEntries<?>> queuedEntries;
		private final String modId;

		@ApiStatus.Internal
		Entries(HolderLookup.Provider registries, String modId) {
			this.registries = registries;
			this.queuedEntries = DynamicRegistries.getDynamicRegistries().stream()
					// Some modded dynamic registries might not be in the wrapper lookup, filter them out
					.filter(e -> registries.lookup(e.key()).isPresent())
					.collect(Collectors.toMap(
							e -> e.key().location(),
							e -> RegistryEntries.create(registries, e)
					));
			this.modId = modId;
		}

		/**
		 * Gets access to all registry lookups.
		 */
		public HolderLookup.Provider getLookups() {
			return registries;
		}

		/**
		 * Gets a lookup for entries from the given registry.
		 */
		public <T> HolderGetter<T> getLookup(ResourceKey<? extends Registry<T>> registryKey) {
			return registries.lookupOrThrow(registryKey);
		}

		/**
		 * Returns a lookup for placed features. Useful when creating biomes.
		 */
		public HolderGetter<PlacedFeature> placedFeatures() {
			return getLookup(Registries.PLACED_FEATURE);
		}

		/**
		 * Returns a lookup for configured carvers. Useful when creating biomes.
		 */
		public HolderGetter<ConfiguredWorldCarver<?>> configuredCarvers() {
			return getLookup(Registries.CONFIGURED_CARVER);
		}

		/**
		 * Gets a reference to a registry entry for use in other registrations.
		 */
		public <T> Holder<T> ref(ResourceKey<T> key) {
			RegistryEntries<T> entries = getQueuedEntries(key);
			return Holder.Reference.createStandAlone(entries.lookup, key);
		}

		/**
		 * Adds a new object to be data generated.
		 *
		 * @return a reference to it for use in other objects.
		 */
		public <T> Holder<T> add(ResourceKey<T> registry, T object) {
			return getQueuedEntries(registry).add(registry.location(), object);
		}

		/**
		 * Adds a new {@link ResourceKey} from a given {@link HolderLookup.RegistryLookup} to be data generated.
		 *
		 * @return a reference to it for use in other objects.
		 */
		public <T> Holder<T> add(HolderLookup.RegistryLookup<T> registry, ResourceKey<T> valueKey) {
			return add(valueKey, registry.getOrThrow(valueKey).value());
		}

		/**
		 * All the registry entries whose namespace matches the current effective mod ID will be data generated.
		 */
		public <T> List<Holder<T>> addAll(HolderLookup.RegistryLookup<T> registry) {
			return registry.listElementIds()
					.filter(registryKey -> registryKey.location().getNamespace().equals(modId))
					.map(key -> add(registry, key))
					.toList();
		}

		@SuppressWarnings("unchecked")
		<T> RegistryEntries<T> getQueuedEntries(ResourceKey<T> key) {
			RegistryEntries<?> regEntries = queuedEntries.get(key.registry());

			if (regEntries == null) {
				throw new IllegalArgumentException("Registry " + key.registry() + " is not loaded from datapacks");
			}

			return (RegistryEntries<T>) regEntries;
		}
	}

	private static class RegistryEntries<T> {
		final HolderOwner<T> lookup;
		final ResourceKey<? extends Registry<T>> registry;
		final Codec<T> elementCodec;
		Map<ResourceKey<T>, T> entries = new IdentityHashMap<>();

		RegistryEntries(HolderOwner<T> lookup,
						ResourceKey<? extends Registry<T>> registry,
						Codec<T> elementCodec) {
			this.lookup = lookup;
			this.registry = registry;
			this.elementCodec = elementCodec;
		}

		static <T> RegistryEntries<T> create(HolderLookup.Provider lookups, RegistryDataLoader.RegistryData<T> loaderEntry) {
			HolderLookup.RegistryLookup<T> lookup = lookups.lookupOrThrow(loaderEntry.key());
			return new RegistryEntries<>(lookup, loaderEntry.key(), loaderEntry.elementCodec());
		}

		public Holder<T> add(ResourceKey<T> key, T value) {
			if (entries.put(key, value) != null) {
				throw new IllegalArgumentException("Trying to add registry key " + key + " more than once.");
			}

			return Holder.Reference.createStandAlone(lookup, key);
		}

		public Holder<T> add(ResourceLocation id, T value) {
			return add(ResourceKey.create(registry, id), value);
		}
	}

	@Override
	public CompletableFuture<?> run(CachedOutput writer) {
		return registriesFuture.thenCompose(registries -> {
			return CompletableFuture
					.supplyAsync(() -> {
						Entries entries = new Entries(registries, output.getModId());
						configure(registries, entries);
						return entries;
					})
					.thenCompose(entries -> {
						final RegistryOps<JsonElement> dynamicOps = registries.createSerializationContext(JsonOps.INSTANCE);
						ArrayList<CompletableFuture<?>> futures = new ArrayList<>();

						for (RegistryEntries<?> registryEntries : entries.queuedEntries.values()) {
							futures.add(writeRegistryEntries(writer, dynamicOps, registryEntries));
						}

						return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
					});
		});
	}

	private <T> CompletableFuture<?> writeRegistryEntries(CachedOutput writer, RegistryOps<JsonElement> ops, RegistryEntries<T> entries) {
		final ResourceKey<? extends Registry<T>> registry = entries.registry;
		final boolean shouldOmitNamespace = registry.location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) || !DynamicRegistriesImpl.FABRIC_DYNAMIC_REGISTRY_KEYS.contains(registry);
		final String directoryName = shouldOmitNamespace ? registry.location().getPath() : registry.location().getNamespace() + "/" + registry.location().getPath();
		final PackOutput.PathProvider pathResolver = output.createPathProvider(PackOutput.Target.DATA_PACK, directoryName);
		final List<CompletableFuture<?>> futures = new ArrayList<>();

		for (Map.Entry<ResourceKey<T>, T> entry : entries.entries.entrySet()) {
			Path path = pathResolver.json(entry.getKey().location());
			futures.add(writeToPath(path, writer, ops, entries.elementCodec, entry.getValue()));
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	private static <E> CompletableFuture<?> writeToPath(Path path, CachedOutput cache, DynamicOps<JsonElement> json, Encoder<E> encoder, E value) {
		Optional<JsonElement> optional = encoder.encodeStart(json, value).resultOrPartial((error) -> {
			LOGGER.error("Couldn't serialize element {}: {}", path, error);
		});

		if (optional.isPresent()) {
			return DataProvider.saveStable(cache, optional.get(), path);
		}

		return CompletableFuture.completedFuture(null);
	}
}
