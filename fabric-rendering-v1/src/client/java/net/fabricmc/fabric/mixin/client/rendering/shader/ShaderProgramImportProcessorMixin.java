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

package net.fabricmc.fabric.mixin.client.rendering.shader;

import net.fabricmc.fabric.impl.client.rendering.ClientRenderingEventHooks;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets modded shaders {@code #moj_import} shaders from any namespace with the
 * {@code <>} syntax.
 */
@Mixin(targets = "net.minecraft.client.renderer.ShaderInstance$1")
abstract class ShaderProgramImportProcessorMixin {
    @ModifyArg(method = "applyImport", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;getShaderImportLocation(Ljava/lang/String;ZLjava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), index = 2)
    private String modifyImportNamespace(String basePath, boolean isRelative, String name) {
        return isRelative && ClientRenderingEventHooks.FABRIC_PROGRAM_NAMESPACE.get() != null ? ClientRenderingEventHooks.FABRIC_PROGRAM_NAMESPACE.get() + ResourceLocation.NAMESPACE_SEPARATOR + name : name;
    }
}
