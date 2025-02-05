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

package net.fabricmc.fabric.test.lookup.api;

import org.jetbrains.annotations.NotNull;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class ItemApis {
	public static final BlockApiLookup<ItemInsertable, @NotNull Direction> INSERTABLE =
			BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath("testmod", "item_insertable"), ItemInsertable.class, Direction.class);
	public static final BlockApiLookup<ItemExtractable, @NotNull Direction> EXTRACTABLE =
			BlockApiLookup.get(ResourceLocation.fromNamespaceAndPath("testmod", "item_extractable"), ItemExtractable.class, Direction.class);

	private ItemApis() {
	}
}
