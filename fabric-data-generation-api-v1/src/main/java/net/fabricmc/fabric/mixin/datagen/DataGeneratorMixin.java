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

package net.fabricmc.fabric.mixin.datagen;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.impl.datagen.DataGeneratorExtension;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;

@Mixin(DataGenerator.class)
public class DataGeneratorMixin implements DataGeneratorExtension {
	@Shadow
	private PackOutput vanillaPackOutput;

	@Override
	public DataGenerator.PackGenerator createPack(String name, PackOutput output) {
		DataGenerator generator = (DataGenerator) (Object) this;
		return generator.new PackGenerator(true, name, output);
	}

	@Override
	public Pair<DataGenerator.PackGenerator, Path> createBuiltinResourcePack(boolean shouldRun, ResourceLocation packName, ModContainer modInfo, boolean strictValidation) {
		Path path = this.vanillaPackOutput.getOutputFolder().resolve("resourcepacks").resolve(packName.getPath());
		DataGenerator generator = (DataGenerator) (Object) this;
		return Pair.of(generator.new PackGenerator(shouldRun, packName.toString(), new FabricDataOutput(modInfo, path, strictValidation)), path);
	}
}
