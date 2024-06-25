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

package net.fabricmc.fabric.impl.attachment;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.mixin.attachment.BaseMappedRegistryAccessor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class AttachmentRegistryImpl {
    private static final Map<net.neoforged.neoforge.attachment.AttachmentType<?>, AttachmentType<?>> FABRIC_ATTACHMENT_TYPES = new HashMap<>();

    public static <A> net.neoforged.neoforge.attachment.AttachmentType<A> register(ResourceLocation id, net.neoforged.neoforge.attachment.AttachmentType<A> attachmentType) {
        ((BaseMappedRegistryAccessor) NeoForgeRegistries.ATTACHMENT_TYPES).invokeUnfreeze();
        Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, id, attachmentType);
        NeoForgeRegistries.ATTACHMENT_TYPES.freeze();
        return attachmentType;
    }

    public static <A> AttachmentRegistry.Builder<A> builder() {
        return new BuilderImpl<>();
    }

    @SuppressWarnings("unchecked")
    public static <A> AttachmentType<A> getFabricAttachmentType(net.neoforged.neoforge.attachment.AttachmentType<A> neoType) {
        return (AttachmentType<A>) FABRIC_ATTACHMENT_TYPES.get(neoType);
    }

    public static class BuilderImpl<A> implements AttachmentRegistry.Builder<A> {
        @Nullable
        private Supplier<A> defaultInitializer = null;
        @Nullable
        private Codec<A> persistenceCodec = null;
        private boolean copyOnDeath = false;

        @Override
        public AttachmentRegistry.Builder<A> persistent(Codec<A> codec) {
            Objects.requireNonNull(codec, "codec cannot be null");

            this.persistenceCodec = codec;
            return this;
        }

        @Override
        public AttachmentRegistry.Builder<A> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        @Override
        public AttachmentRegistry.Builder<A> initializer(Supplier<A> initializer) {
            Objects.requireNonNull(initializer, "initializer cannot be null");

            this.defaultInitializer = initializer;
            return this;
        }

        @Override
        public AttachmentType<A> buildAndRegister(ResourceLocation id) {
            net.neoforged.neoforge.attachment.AttachmentType<A> neoType = register(id, toNeoForgeAttachmentType()); 
            AttachmentType<A> attachmentType = new AttachmentTypeImpl<>(neoType, id, defaultInitializer, persistenceCodec, copyOnDeath);
            FABRIC_ATTACHMENT_TYPES.put(neoType, attachmentType);
            return attachmentType;
        }

        private net.neoforged.neoforge.attachment.AttachmentType<A> toNeoForgeAttachmentType() {
            net.neoforged.neoforge.attachment.AttachmentType.Builder<A> builder = net.neoforged.neoforge.attachment.AttachmentType.builder(this.defaultInitializer != null ? this.defaultInitializer : () -> null);
            if (this.persistenceCodec != null) {
                builder.serialize(this.persistenceCodec);
                if (this.copyOnDeath) {
                    builder.copyOnDeath();
                }
            }
            return builder.build();
        }
    }
}
