package net.fabricmc.fabric.impl.client.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class ClientRenderingEventHooks {
    public static final ThreadLocal<String> FABRIC_PROGRAM_NAMESPACE = ThreadLocal.withInitial(() -> null);

    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        ColorProviderRegistryImpl.BLOCK.initialize(event.getBlockColors());
    }

    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ColorProviderRegistryImpl.ITEM.initialize(event.getItemColors());
    }

    public static void onRegisterColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        ColorResolverRegistryImpl.getCustomResolvers().forEach(event::register);
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            CoreShaderRegistrationCallback.RegistrationContext context = (id, vertexFormat, loadCallback) -> {
                ShaderInstance program = new ShaderInstance(event.getResourceProvider(), id, vertexFormat);
                event.registerShader(program, loadCallback);
            };
            CoreShaderRegistrationCallback.EVENT.invoker().registerShaders(context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        EntityRendererRegistryImpl.setup((type, provider) -> event.registerEntityRenderer((EntityType) type, provider));

        BlockEntityRendererRegistryImpl.setup((t, factory) -> event.registerBlockEntityRenderer((BlockEntityType) t, factory));
    }

    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        EntityModelLayerImpl.PROVIDERS.forEach((name, provider) -> event.registerLayerDefinition(name, provider::createModelData));
    }

    public static void onPostRenderHud(RenderGuiEvent.Post event) {
        HudRenderCallback.EVENT.invoker().onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }
}
