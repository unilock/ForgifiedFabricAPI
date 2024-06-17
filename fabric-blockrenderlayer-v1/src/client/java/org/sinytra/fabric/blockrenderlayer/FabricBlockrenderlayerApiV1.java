package org.sinytra.fabric.blockrenderlayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.impl.blockrenderlayer.BlockRenderLayerMapImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class FabricBlockrenderlayerApiV1 implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        bus.addListener(FMLClientSetupEvent.class, event -> BlockRenderLayerMapImpl.initialize(ItemBlockRenderTypes::setRenderLayer, ItemBlockRenderTypes::setRenderLayer));
    }
}
