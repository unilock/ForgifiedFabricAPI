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

package net.fabricmc.fabric.impl.client.event.lifecycle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.event.lifecycle.LoadedChunksCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class ClientLifecycleEventsImpl implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Part of impl for block entity events
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            ((LoadedChunksCache) world).fabric_markLoaded(chunk);
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            ((LoadedChunksCache) world).fabric_markUnloaded(chunk);
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(blockEntity, world);
            }
        });

        // Sinytra impl
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, ev -> {
            if (ev.getLevel() instanceof ClientLevel cw && ev.getChunk() instanceof LevelChunk wc) {
                ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(cw, wc);
            }
        });
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, ev -> {
            if (ev.getLevel() instanceof ClientLevel cw && ev.getChunk() instanceof LevelChunk wc) {
                ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(cw, wc);
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, ev -> ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ev -> ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Pre.class, ev -> {
            if (ev.getLevel() instanceof ClientLevel cw) {
                ClientTickEvents.START_WORLD_TICK.invoker().onStartTick(cw);
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, ev -> {
            if (ev.getLevel() instanceof ClientLevel cw) {
                ClientTickEvents.END_WORLD_TICK.invoker().onEndTick(cw);
            }
        });
    }
}
