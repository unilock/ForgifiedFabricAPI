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

package net.fabricmc.fabric.impl.resource.loader;

import java.util.Set;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;

@Mod("fabric_resource_loader_v0")
public class ResourceLoaderImpl {
	private static final String DUMMY_CLIENT_NAMESPACE = "fabric-resource-loader-v0-client";
	private static final String DUMMY_SERVER_NAMESPACE = "fabric-resource-loader-v0-server";

	public ResourceLoaderImpl() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(ResourceLoaderImpl::addPackFinders);
	}

	private static void addPackFinders(AddPackFindersEvent event) {
		event.addRepositorySource(new ModResourcePackCreator(event.getPackType()));
	}

	@Nullable
	public static ResourceType guessResourceType(ResourceManager manager) {
		Set<String> namespaces = manager.getAllNamespaces();
		return namespaces.contains(DUMMY_CLIENT_NAMESPACE) && !namespaces.contains(DUMMY_SERVER_NAMESPACE) ? ResourceType.CLIENT_RESOURCES
				: namespaces.contains(DUMMY_SERVER_NAMESPACE) && !namespaces.contains(DUMMY_CLIENT_NAMESPACE) ? ResourceType.SERVER_DATA
				: null;
	}
}
