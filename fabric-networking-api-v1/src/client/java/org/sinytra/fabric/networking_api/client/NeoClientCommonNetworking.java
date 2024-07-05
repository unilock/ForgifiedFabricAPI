package org.sinytra.fabric.networking_api.client;

import net.fabricmc.fabric.api.client.networking.v1.C2SConfigurationChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

import java.util.List;
import java.util.Set;

public class NeoClientCommonNetworking {
    public static void onRegisterPacket(ICommonPacketListener listener, Set<ResourceLocation> ids) {
        ConnectionProtocol protocol = listener.protocol();
        if (protocol == ConnectionProtocol.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> C2SConfigurationChannelEvents.REGISTER.invoker().onChannelRegister((ClientConfigurationPacketListenerImpl) listener, new NeoClientPacketSender(listener.getConnection()), Minecraft.getInstance(), List.copyOf(ids)));
        } else if (protocol == ConnectionProtocol.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> C2SPlayChannelEvents.REGISTER.invoker().onChannelRegister((ClientPacketListener) listener, new NeoClientPacketSender(listener.getConnection()), Minecraft.getInstance(), List.copyOf(ids)));
        }
    }

    public static void onUnregisterPacket(ICommonPacketListener listener, Set<ResourceLocation> ids) {
        ConnectionProtocol protocol = listener.protocol();
        if (protocol == ConnectionProtocol.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> C2SConfigurationChannelEvents.UNREGISTER.invoker().onChannelUnregister((ClientConfigurationPacketListenerImpl) listener, new NeoClientPacketSender(listener.getConnection()), Minecraft.getInstance(), List.copyOf(ids)));
        } else if (protocol == ConnectionProtocol.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> C2SPlayChannelEvents.UNREGISTER.invoker().onChannelUnregister((ClientPacketListener) listener, new NeoClientPacketSender(listener.getConnection()), Minecraft.getInstance(), List.copyOf(ids)));
        }
    }
}
