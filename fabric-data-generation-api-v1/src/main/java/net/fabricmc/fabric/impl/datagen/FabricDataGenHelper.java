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

package net.fabricmc.fabric.impl.datagen;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataProvider;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.sinytra.fabric.data_generation_api.generated.GeneratedEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = GeneratedEntryPoint.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FabricDataGenHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(FabricDataGenHelper.class);

	/**
	 * When enabled the dedicated server startup will be hijacked to run the data generators and then quit.
	 */
	public static final boolean ENABLED = System.getProperty("fabric-api.datagen") != null;

	/**
	 * Sets the output directory for the generated data.
	 */
	private static final String OUTPUT_DIR = System.getProperty("fabric-api.datagen.output-dir");

	/**
	 * When enabled providers can enable extra validation, such as ensuring all registry entries have data generated for them.
	 */
	private static final boolean STRICT_VALIDATION = System.getProperty("fabric-api.datagen.strict-validation") != null;

	/**
	 * Filter to a specific mod ID with this property, useful if dependencies also have data generators.
	 */
	@Nullable
	private static final String MOD_ID_FILTER = System.getProperty("fabric-api.datagen.modid");

	/**
	 * Entrypoint key to register classes implementing {@link DataGeneratorEntrypoint}.
	 */
	private static final String ENTRYPOINT_KEY = "fabric-datagen";
	
	private static final Map<ModContainer, DataGeneratorEntrypoint> DATAGEN_ENTRYPOINTS = new HashMap<>();

	public static void registerDatagenEntrypoint(String modid, DataGeneratorEntrypoint entrypoint) {
		ModContainer modContainer =  FabricLoader.getInstance().getModContainer(modid).orElseThrow(() -> new RuntimeException("Failed to find effective mod container for mod id (%s)".formatted(modid)));
		DATAGEN_ENTRYPOINTS.put(modContainer, entrypoint);
	}

	private FabricDataGenHelper() {
	}

	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event) {
		Object2IntOpenHashMap<String> jsonKeySortOrders = (Object2IntOpenHashMap<String>) DataProvider.FIXED_ORDER_FIELDS;
		Object2IntOpenHashMap<String> defaultJsonKeySortOrders = new Object2IntOpenHashMap<>(jsonKeySortOrders);

		DATAGEN_ENTRYPOINTS.forEach((modContainer, entrypoint) -> {
			LOGGER.info("Running data generator for {}", modContainer.getMetadata().getId());

			try {
				HashSet<String> keys = new HashSet<>();
				entrypoint.addJsonKeySortOrders((key, value) -> {
					Objects.requireNonNull(key, "Tried to register a priority for a null key");
					jsonKeySortOrders.put(key, value);
					keys.add(key);
				});

				final RegistrySetBuilder builder = new RegistrySetBuilder();
				entrypoint.buildRegistry(builder);

				CompletableFuture<HolderLookup.Provider> registriesFuture = FabricDataGenHelper.createRegistryWrapper(event.getLookupProvider(), builder);
				FabricDataGenerator dataGenerator = new FabricDataGenerator(event.getGenerator(), event.getGenerator().getPackOutput().getOutputFolder(), modContainer, STRICT_VALIDATION, registriesFuture);
				entrypoint.onInitializeDataGenerator(dataGenerator);

				jsonKeySortOrders.keySet().removeAll(keys);
				jsonKeySortOrders.putAll(defaultJsonKeySortOrders);
			} catch (Throwable t) {
				throw new RuntimeException("Failed to run data generator from mod (%s)".formatted(modContainer.getMetadata().getId()), t);
			}
		});
	}

	private static CompletableFuture<HolderLookup.Provider> createRegistryWrapper(CompletableFuture<HolderLookup.Provider> original, RegistrySetBuilder registryBuilder) {
		// Build a list of all the RegistryBuilder's including vanilla's
		List<RegistrySetBuilder> builders = new ArrayList<>();
		builders.add(VanillaRegistries.BUILDER);

		builders.add(registryBuilder);

		// Collect all the bootstrap functions, and merge the lifecycles.
		class BuilderData {
			final ResourceKey key;
			List<RegistrySetBuilder.RegistryBootstrap<?>> bootstrapFunctions;
			Lifecycle lifecycle;

			BuilderData(ResourceKey key) {
				this.key = key;
				this.bootstrapFunctions = new ArrayList<>();
				this.lifecycle = Lifecycle.stable();
			}

			void with(RegistrySetBuilder.RegistryStub<?> registryInfo) {
				bootstrapFunctions.add(registryInfo.bootstrap());
				lifecycle = registryInfo.lifecycle().add(lifecycle);
			}

			void apply(RegistrySetBuilder builder) {
				builder.add(key, lifecycle, this::bootstrap);
			}

			void bootstrap(BootstrapContext registerable) {
				for (RegistrySetBuilder.RegistryBootstrap<?> function : bootstrapFunctions) {
					function.run(registerable);
				}
			}
		}

		Map<ResourceKey<?>, BuilderData> builderDataMap = new HashMap<>();

		// Ensure all dynamic registries are present.
		for (RegistryDataLoader.RegistryData<?> key : DynamicRegistries.getDynamicRegistries()) {
			builderDataMap.computeIfAbsent(key.key(), BuilderData::new);
		}

		for (RegistrySetBuilder builder : builders) {
			for (RegistrySetBuilder.RegistryStub<?> info : builder.entries) {
				builderDataMap.computeIfAbsent(info.key(), BuilderData::new)
						.with(info);
			}
		}

		// Apply all the builders into one.
		RegistrySetBuilder merged = new RegistrySetBuilder();

		for (BuilderData value : builderDataMap.values()) {
			value.apply(merged);
		}
		var builderKeys = new HashSet<>(merged.getEntryKeys());
		DataPackRegistriesHooks.getDataPackRegistriesWithDimensions().filter(data -> !builderKeys.contains(data.key())).forEach(data -> merged.add(data.key(), context -> {}));

		return RegistryPatchGenerator.createLookup(original, merged)
			.thenApply(RegistrySetBuilder.PatchedRegistries::full)
			.thenApply(provider -> {
				VanillaRegistries.validateThatAllBiomeFeaturesHaveBiomeFilter(provider);
				return provider;
			});
	}

	/**
	 * Used to keep track of conditions associated to generated objects.
	 */
	private static final Map<Object, ResourceCondition[]> CONDITIONS_MAP = new IdentityHashMap<>();

	public static void addConditions(Object object, ResourceCondition[] conditions) {
		CONDITIONS_MAP.merge(object, conditions, ArrayUtils::addAll);
	}

	@Nullable
	public static ResourceCondition[] consumeConditions(Object object) {
		return CONDITIONS_MAP.remove(object);
	}

	/**
	 * Adds {@code conditions} to {@code baseObject}.
	 * @param baseObject the base JSON object to which the conditions are inserted
	 * @param conditions the conditions to insert
	 * @throws IllegalArgumentException if the object already has conditions
	 */
	public static void addConditions(JsonObject baseObject, ResourceCondition... conditions) {
		if (baseObject.has(ResourceConditions.CONDITIONS_KEY)) {
			throw new IllegalArgumentException("Object already has a condition entry: " + baseObject);
		} else if (conditions == null || conditions.length == 0) {
			// Datagen might pass null conditions.
			return;
		}

		baseObject.add(ResourceConditions.CONDITIONS_KEY, ResourceCondition.LIST_CODEC.encodeStart(JsonOps.INSTANCE, Arrays.asList(conditions)).getOrThrow());
	}
}
