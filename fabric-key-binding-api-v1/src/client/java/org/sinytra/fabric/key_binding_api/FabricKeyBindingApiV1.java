package org.sinytra.fabric.key_binding_api;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class FabricKeyBindingApiV1 implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        bus.addListener(RegisterKeyMappingsEvent.class, KeyBindingRegistryImpl::process);
    }
}
