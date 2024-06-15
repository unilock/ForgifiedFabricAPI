package net.fabricmc.fabric.mixin.gametest;

import net.neoforged.neoforge.gametest.GameTestHooks;
import org.sinytra.fabric.gametest_api_v1.FabricGameTestApiV1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(value = GameTestHooks.class, remap = false)
public class GameTestHooksMixin {

    @Inject(method = "getTemplateNamespace", at = @At("TAIL"), cancellable = true)
    private static void provideFabricTestNamespace(Method method, CallbackInfoReturnable<String> cir) {
        String namespace = FabricGameTestApiV1.getGameTestNamespace(method);
        if (namespace != null) {
            cir.setReturnValue(namespace);
        }
    }
}
