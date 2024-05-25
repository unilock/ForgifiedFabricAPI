package net.fabricmc.fabric.mixin.gametest;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import org.sinytra.fabric.gametest_api_v1_testmod.FabricGameTestApiV1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Method;

@Mixin(value = GameTestRegistry.class)
public class GameTestRegistryMixin {

    @ModifyVariable(method = "turnMethodIntoTestFunction", at = @At(value = "INVOKE", target = "Lnet/minecraft/gametest/framework/GameTest;batch()Ljava/lang/String;"), ordinal = 3)
    private static String provideFabricTestName(String str, Method method) {
        if (FabricGameTestApiV1.shouldProcess(method)) {
            GameTest gametest = method.getAnnotation(GameTest.class);
            if (gametest.templateNamespace().isEmpty() && !gametest.template().isEmpty()) {
                return gametest.template();
            }
        }
        return str;
    }

    @WrapOperation(method = "lambda$turnMethodIntoConsumer$5(Ljava/lang/reflect/Method;Ljava/lang/Object;)V", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Method;invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object redirectTestInvoker(Method method, Object instance, Object[] args, Operation<Object> original) {
        if (instance instanceof FabricGameTest fabricGameTest && args.length > 0 && args[0] instanceof GameTestHelper ctx) {
            fabricGameTest.invokeTestMethod(ctx, method);
            return null;
        }
        return original.call(method, instance, args);
    }
}
