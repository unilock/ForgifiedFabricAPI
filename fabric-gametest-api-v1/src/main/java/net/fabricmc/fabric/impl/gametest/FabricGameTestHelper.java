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

package net.fabricmc.fabric.impl.gametest;

import cpw.mods.modlauncher.api.LambdaExceptionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.resources.FileToIdConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class FabricGameTestHelper {
	/**
	 * When enabled the {@link TestCommand} and related arguments will be registered.
	 *
	 * <p>When {@link EnvType#CLIENT} the default value is true.
	 *
	 * <p>When {@link EnvType#SERVER} the default value is false.
	 */
	public static final boolean COMMAND_ENABLED = Boolean.parseBoolean(System.getProperty("fabric-api.gametest.command", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "true" : "false"));

	private static final Logger LOGGER = LoggerFactory.getLogger(FabricGameTestHelper.class);

	private static final String GAMETEST_STRUCTURE_PATH = "gametest/structures";

	public static final FileToIdConverter GAMETEST_STRUCTURE_FINDER = new FileToIdConverter(GAMETEST_STRUCTURE_PATH, ".snbt");

	private FabricGameTestHelper() {
	}

	public static void invokeTestMethod(GameTestHelper testContext, Method method, Object testObject) {
		LambdaExceptionUtils.uncheck(() -> method.invoke(testObject, testContext));
	}
}
