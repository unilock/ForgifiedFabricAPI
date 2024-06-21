package org.sinytra.fabric.rendering.generated;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.impl.client.rendering.ClientRenderingEventHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;

public class FabricRenderingV1 implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();

        bus.addListener(ClientRenderingEventHooks::onRegisterBlockColors);
        bus.addListener(ClientRenderingEventHooks::onRegisterItemColors);
        bus.addListener(ClientRenderingEventHooks::onRegisterColorResolvers);
        bus.addListener(ClientRenderingEventHooks::onRegisterShaders);
        bus.addListener(ClientRenderingEventHooks::onRegisterEntityRenderers);
        bus.addListener(ClientRenderingEventHooks::onRegisterLayerDefinitions);

        NeoForge.EVENT_BUS.addListener(ClientRenderingEventHooks::onPostRenderHud);
    }
}
