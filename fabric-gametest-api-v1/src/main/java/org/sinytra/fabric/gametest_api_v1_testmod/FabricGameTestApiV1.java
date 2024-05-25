package org.sinytra.fabric.gametest_api_v1_testmod;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FabricGameTestApiV1 {
    public static final String PREFIX_MOD_FILE_PROPERTY = "forgified-fabric-api:game-test-prefix";

    private static final Supplier<Map<String, String>> PREFIX_GAME_TEST_MODULES = Suppliers.memoize(() ->
        ModList.get().getModFiles().stream()
            .map(i -> {
                String prefix = (String) i.getFileProperties().get(PREFIX_MOD_FILE_PROPERTY);
                if (prefix != null) {
                    return Pair.of(i.moduleName(), prefix);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))
    );

    public static boolean shouldProcess(Method method) {
        String moduleName = method.getDeclaringClass().getModule().getName();
        return PREFIX_GAME_TEST_MODULES.get().containsKey(moduleName);
    }

    @Nullable
    public static String getGameTestNamespace(Method method) {
        String moduleName = method.getDeclaringClass().getModule().getName();
        return PREFIX_GAME_TEST_MODULES.get().get(moduleName);
    }
}
