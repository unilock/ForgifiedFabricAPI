package org.sinytra.fabric.networking_api.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.mixin.networking.accessor.NetworkRegistryAccessor;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;
import org.sinytra.fabric.networking_api.NeoGlobalHandlerRegistry;

import java.util.Set;

public class NeoServerConfigurationNetworking {

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, ServerConfigurationNetworking.ConfigurationPacketHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_C2S, type.id(), PacketFlow.SERVERBOUND, ConnectionProtocol.CONFIGURATION);
        return NeoGlobalHandlerRegistry.registerGlobalReceiver(type, ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, neoHandler -> neoHandler.registerHandler(PacketFlow.SERVERBOUND, handler, WrapNeoContext::new, handler::receive));
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?> unregisterGlobalReceiver(ResourceLocation id) {
        // TODO Support unregistration
        return null;
    }

    public static Set<ResourceLocation> getGlobalReceivers() {
        return NetworkRegistryAccessor.getPayloadRegistrations().get(ConnectionProtocol.CONFIGURATION).keySet();
    }

    public static <T extends CustomPacketPayload> boolean registerReceiver(ServerConfigurationPacketListenerImpl networkHandler, CustomPacketPayload.Type<T> type, ServerConfigurationNetworking.ConfigurationPacketHandler<T> handler) {
        // TODO Per-channel networking
        return registerGlobalReceiver(type, handler);
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?> unregisterReceiver(ServerConfigurationPacketListenerImpl networkHandler, ResourceLocation id) {
        // TODO Support unregistration
        return null;
    }

    public static Set<ResourceLocation> getReceived(ServerConfigurationPacketListenerImpl handler) throws IllegalStateException {
        // TODO Per-channel networking
        return getGlobalReceivers();
    }

    public static Set<ResourceLocation> getSendable(ServerConfigurationPacketListenerImpl handler) throws IllegalStateException {
        // TODO Per-channel networking
        return getGlobalReceivers();
    }

    public static boolean canSend(ServerConfigurationPacketListenerImpl handler, ResourceLocation channelName) throws IllegalArgumentException {
        return NetworkRegistry.hasChannel(handler, channelName);
    }

    public static PacketSender getSender(ServerConfigurationPacketListenerImpl handler) {
        return new NeoServerPacketSender(handler.getConnection());
    }

    private record WrapNeoContext(IPayloadContext context) implements ServerConfigurationNetworking.Context {
        @Override
        public ServerConfigurationPacketListenerImpl networkHandler() {
            return (ServerConfigurationPacketListenerImpl) context.listener();
        }

        @Override
        public PacketSender responseSender() {
            return new NeoServerPacketSender(context.connection());
        }
    }
}
