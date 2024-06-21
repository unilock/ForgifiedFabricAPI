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

import com.mojang.blaze3d.shaders.Program;
import net.fabricmc.fabric.impl.client.rendering.ClientRenderingEventHooks;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderInstance.class)
abstract class ShaderProgramMixin {
    // Allow loading shader stages from arbitrary namespaces.
    @Inject(method = "getOrCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/FileUtil;getFullResourcePath(Ljava/lang/String;)Ljava/lang/String;"))
    private static void captureProgramName(ResourceProvider resourceProvider, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        if (name.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            ClientRenderingEventHooks.FABRIC_PROGRAM_NAMESPACE.set(name.substring(0, name.indexOf(ResourceLocation.NAMESPACE_SEPARATOR)));
        }
    }

    @Inject(method = "getOrCreate", at = @At("TAIL"))
    private static void releaseProgramName(ResourceProvider resourceProvider, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        ClientRenderingEventHooks.FABRIC_PROGRAM_NAMESPACE.remove();
    }
}
