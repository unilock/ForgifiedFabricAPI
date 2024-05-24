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

package net.fabricmc.fabric.test.dimension;

import static net.minecraft.world.entity.EntityType.COW;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

public class FabricDimensionTest implements ModInitializer {
	// The dimension options refer to the JSON-file in the dimension subfolder of the data pack,
	// which will always share its ID with the world that is created from it
	private static final ResourceKey<LevelStem> DIMENSION_KEY = ResourceKey.create(Registries.LEVEL_STEM, new ResourceLocation("fabric_dimension", "void"));
	private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Component.literal("Teleportation failed!"));

	private static ResourceKey<Level> WORLD_KEY = ResourceKey.create(Registries.DIMENSION, DIMENSION_KEY.location());

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, new ResourceLocation("fabric_dimension", "void"), VoidChunkGenerator.CODEC);

		WORLD_KEY = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("fabric_dimension", "void"));

		if (System.getProperty("fabric-api.gametest") != null) {
			// The gametest server does not support custom worlds
			return;
		}

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel overworld = server.getLevel(Level.OVERWORLD);
			ServerLevel world = server.getLevel(WORLD_KEY);

			if (world == null) throw new AssertionError("Test world doesn't exist.");

			Entity entity = COW.create(overworld);

			if (entity == null) throw new AssertionError("Could not create entity!");
			if (!entity.level().dimension().equals(Level.OVERWORLD)) throw new AssertionError("Entity starting world isn't the overworld");

			PortalInfo target = new PortalInfo(Vec3.ZERO, new Vec3(1, 1, 1), 45f, 60f);

			Entity teleported = FabricDimensions.teleport(entity, world, target);

			if (teleported == null) throw new AssertionError("Entity didn't teleport");

			if (!teleported.level().dimension().equals(WORLD_KEY)) throw new AssertionError("Target world not reached.");

			if (!teleported.position().equals(target.pos)) throw new AssertionError("Target Position not reached.");
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("fabric_dimension_test")
					.executes(FabricDimensionTest.this::swapTargeted));

			// Used to test https://github.com/FabricMC/fabric/issues/2239
			// Dedicated-only
			if (environment != Commands.CommandSelection.INTEGRATED) {
				dispatcher.register(literal("fabric_dimension_test_desync")
						.executes(FabricDimensionTest.this::testDesync));
			}

			// Used to test https://github.com/FabricMC/fabric/issues/2238
			dispatcher.register(literal("fabric_dimension_test_entity")
					.executes(FabricDimensionTest.this::testEntityTeleport));

			// Used to test teleport to vanilla dimension
			dispatcher.register(literal("fabric_dimension_test_tp")
					.then(argument("target", DimensionArgument.dimension())
					.executes((context) ->
							testVanillaTeleport(context, DimensionArgument.getDimension(context, "target")))));
		});
	}

	private int swapTargeted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayer();

		if (player == null) {
			context.getSource().sendSuccess(() -> Component.literal("You must be a player to execute this command."), false);
			return 1;
		}

		ServerLevel serverWorld = player.serverLevel();
		ServerLevel modWorld = getModWorld(context);

		if (serverWorld != modWorld) {
			PortalInfo target = new PortalInfo(new Vec3(0.5, 101, 0.5), Vec3.ZERO, 0, 0);
			FabricDimensions.teleport(player, modWorld, target);

			if (player.level() != modWorld) {
				throw FAILED_EXCEPTION.create();
			}

			modWorld.setBlockAndUpdate(new BlockPos(0, 100, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
			modWorld.setBlockAndUpdate(new BlockPos(0, 101, 0), Blocks.TORCH.defaultBlockState());
		} else {
			PortalInfo target = new PortalInfo(new Vec3(0, 100, 0), Vec3.ZERO,
					(float) Math.random() * 360 - 180, (float) Math.random() * 360 - 180);
			FabricDimensions.teleport(player, getWorld(context, Level.OVERWORLD), target);
		}

		return 1;
	}

	private int testDesync(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();

		if (player == null) {
			context.getSource().sendSuccess(() -> Component.literal("You must be a player to execute this command."), false);
			return 1;
		}

		PortalInfo target = new PortalInfo(player.position().add(5, 0, 0), player.getDeltaMovement(), player.getYRot(), player.getXRot());
		FabricDimensions.teleport(player, (ServerLevel) player.level(), target);

		return 1;
	}

	private int testEntityTeleport(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();

		if (player == null) {
			context.getSource().sendSuccess(() -> Component.literal("You must be a player to execute this command."), false);
			return 1;
		}

		Entity entity = player.level()
				.getEntities(player, player.getBoundingBox().inflate(100, 100, 100))
				.stream()
				.findFirst()
				.orElse(null);

		if (entity == null) {
			context.getSource().sendSuccess(() -> Component.literal("No entities found."), false);
			return 1;
		}

		PortalInfo target = new PortalInfo(player.position(), player.getDeltaMovement(), player.getYRot(), player.getXRot());
		FabricDimensions.teleport(entity, (ServerLevel) entity.level(), target);

		return 1;
	}

	private int testVanillaTeleport(CommandContext<CommandSourceStack> context, ServerLevel targetWorld) throws CommandSyntaxException {
		Entity entity = context.getSource().getEntityOrException();
		PortalInfo target = new PortalInfo(entity.position(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
		FabricDimensions.teleport(entity, targetWorld, target);

		return 1;
	}

	private ServerLevel getModWorld(CommandContext<CommandSourceStack> context) {
		return getWorld(context, WORLD_KEY);
	}

	private ServerLevel getWorld(CommandContext<CommandSourceStack> context, ResourceKey<Level> dimensionRegistryKey) {
		return context.getSource().getServer().getLevel(dimensionRegistryKey);
	}
}
