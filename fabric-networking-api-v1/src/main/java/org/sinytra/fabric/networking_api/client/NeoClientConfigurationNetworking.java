package org.sinytra.fabric.networking_api.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.mixin.networking.accessor.NetworkRegistryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;
import org.sinytra.fabric.networking_api.NeoGlobalHandlerRegistry;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

public class NeoClientConfigurationNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, ClientConfigurationNetworking.ConfigurationPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_S2C, type.id(), PacketFlow.CLIENTBOUND, ConnectionProtocol.CONFIGURATION);
        return NeoGlobalHandlerRegistry.registerGlobalReceiver(type, ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, neoHandler -> neoHandler.registerHandler(PacketFlow.CLIENTBOUND, handler, WrapNeoContext::new, handler::receive));
    }

    public static ClientConfigurationNetworking.ConfigurationPayloadHandler<?> unregisterGlobalReceiver(ResourceLocation id) {
        // TODO Support unregistration
        return null;
    }

    public static Set<ResourceLocation> getGlobalReceivers() {
        return NetworkRegistryAccessor.getPayloadRegistrations().get(ConnectionProtocol.CONFIGURATION).keySet();
    }

    public static <T extends CustomPacketPayload> boolean registerReceiver(CustomPacketPayload.Type<T> type, ClientConfigurationNetworking.ConfigurationPayloadHandler<T> handler) {
        // TODO Per-channel networking
        return registerGlobalReceiver(type, handler);
    }

    public static ClientConfigurationNetworking.ConfigurationPayloadHandler<?> unregisterReceiver(ResourceLocation id) {
        // TODO Support unregistration
        return null;
    }

    public static Set<ResourceLocation> getReceived() throws IllegalStateException {
        // TODO Per-channel networking
        return getGlobalReceivers();
    }

    public static Set<ResourceLocation> getSendable() throws IllegalStateException {
        // TODO Per-channel networking
        return getGlobalReceivers();
    }

    public static boolean canSend(ResourceLocation channelName) throws IllegalArgumentException {
        return NetworkRegistry.hasChannel(Minecraft.getInstance().getConnection(), channelName);
    }

    public static PacketSender getSender() {
        return new NeoClientPacketSender(Minecraft.getInstance().getConnection().getConnection());
    }

    public static void send(CustomPacketPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        Objects.requireNonNull(payload.type(), "CustomPayload#getId() cannot return null for payload class: " + payload.getClass());

        // TODO
//        final ClientConfigurationNetworkAddon addon = ClientNetworkingImpl.getClientConfigurationAddon();
//
//        if (addon != null) {
//            addon.sendPacket(payload);
//            return;
//        }

        throw new IllegalStateException("Cannot send packet while not configuring!");
    }

    // TODO
    public static void onServerReady(ClientPacketListener handler, Minecraft client) {
        try {
            ClientPlayConnectionEvents.JOIN.invoker().onPlayReady(handler, new NeoClientPacketSender(handler.getConnection()), client);
        } catch (RuntimeException e) {
            LOGGER.error("Exception thrown while invoking ClientPlayConnectionEvents.JOIN", e);
        }
    }

    private record WrapNeoContext(IPayloadContext context) implements ClientConfigurationNetworking.Context {
        @Override
        public PacketSender responseSender() {
            return new NeoClientPacketSender(context.connection());
        }
    }
}
