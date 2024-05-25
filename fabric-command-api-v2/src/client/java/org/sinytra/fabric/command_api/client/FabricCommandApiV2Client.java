package org.sinytra.fabric.command_api.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FabricCommandApiV2Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
            //noinspection unchecked
            ClientCommandInternals.setActiveDispatcher((CommandDispatcher<FabricClientCommandSource>) (Object) event.getDispatcher());
            ClientCommandRegistrationCallback.EVENT.invoker().register(ClientCommandInternals.getActiveDispatcher(), event.getBuildContext());
        });
    }
}
