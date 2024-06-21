package org.sinytra.fabric.rendering_fluids;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRendererCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;

public class FabricRenderingFabricV1 implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        bus.addListener(FluidRendererCompat::onClientSetup);
    }
}
