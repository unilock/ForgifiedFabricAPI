package org.sinytra.fabric.networking_api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.commands.DebugConfigCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.sinytra.fabric.networking_api.server.NeoServerPlayNetworking;

@EventBusSubscriber
public class NetworkingEventHooks {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            // Command is registered when isDevelopment is set.
            return;
        }

        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // Only register this command in a dev env
            return;
        }

        DebugConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerReady(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            NeoServerPlayNetworking.onClientReady(event.getPlayer());
        }
    }
}
