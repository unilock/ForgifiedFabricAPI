package org.sinytra.fabric.networking_api;

import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.mixin.networking.accessor.NetworkRegistryAccessor;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class NeoGlobalHandlerRegistry {
    private static final Map<ResourceLocation, NeoCommonNetworkHandler<?>> REGISTERED_PAYLOADS = new HashMap<>();
    public static final StreamCodec<?, ?> DUMMY_CODEC = StreamCodec.of((a, b) -> { throw new UnsupportedOperationException(); }, a -> { throw new UnsupportedOperationException(); });

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(CustomPacketPayload.Type<T> type, ConnectionProtocol protocol, PacketFlow flow, Consumer<NeoCommonNetworkHandler<T>> consumer) {
        NeoCommonNetworkHandler<T> neoHandler = getOrRegisterHandler(type, protocol);

        if (neoHandler.hasHandler(flow)) {
            return false;
        }

        consumer.accept(neoHandler);

        return true;
    }

    public static <T extends CustomPacketPayload, U> U unregisterGlobalReceiver(ResourceLocation id, PacketFlow flow) {
        NeoCommonNetworkHandler<T> neoHandler = (NeoCommonNetworkHandler<T>) REGISTERED_PAYLOADS.get(id);
        return neoHandler != null ? neoHandler.unregisterHandler(flow) : null;
    }

    public static boolean hasCodecFor(ConnectionProtocol protocol, PacketFlow flow, ResourceLocation id) {
        PayloadTypeRegistryImpl<? extends FriendlyByteBuf> registry = getPayloadRegistry(protocol, flow);
        return registry.get(id) != null;
    }

    public static PayloadTypeRegistryImpl<? extends FriendlyByteBuf> getPayloadRegistry(ConnectionProtocol protocol, PacketFlow flow) {
        if (protocol == ConnectionProtocol.PLAY) {
            return flow == PacketFlow.SERVERBOUND ? PayloadTypeRegistryImpl.PLAY_C2S : PayloadTypeRegistryImpl.PLAY_S2C;
        } else if (protocol == ConnectionProtocol.CONFIGURATION) {
            return flow == PacketFlow.SERVERBOUND ? PayloadTypeRegistryImpl.CONFIGURATION_C2S : PayloadTypeRegistryImpl.CONFIGURATION_S2C;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> NeoCommonNetworkHandler<T> getOrRegisterHandler(CustomPacketPayload.Type<T> type, ConnectionProtocol protocol) {
        return (NeoCommonNetworkHandler<T>) REGISTERED_PAYLOADS.computeIfAbsent(type.id(), k -> {
            NeoCommonNetworkHandler<T> handler = new NeoCommonNetworkHandler<>();
            boolean setup = NetworkRegistryAccessor.getSetup(); 
            NetworkRegistryAccessor.setSetup(false);
            NetworkRegistry.register(type, (StreamCodec<? super FriendlyByteBuf, T>) NeoNetworkRegistrar.DUMMY_CODEC, handler, List.of(protocol), Optional.empty(), "1.0", setup);
            NetworkRegistryAccessor.setSetup(setup);
            // TODO Send registration message when registering late
            return handler;
        });
    }

    @SuppressWarnings("rawtypes")
    public static class NeoCommonNetworkHandler<T extends CustomPacketPayload> implements IPayloadHandler<T> {
        private final Map<PacketFlow, NeoHandler> receivers = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public void handle(T arg, IPayloadContext context) {
            NeoHandler handler = receivers.get(context.flow());
            if (handler != null) {
                context.enqueueWork(() -> handler.handle(arg, context));
            }
        }

        public boolean hasHandler(PacketFlow flow) {
            return receivers.containsKey(flow);
        }

        public <C, U> void registerHandler(PacketFlow flow, U original, Function<IPayloadContext, C> contextFactory, BiConsumer<T, C> consumer) {
            if (!hasHandler(flow)) {
                receivers.put(flow, new NeoHandler<>(original, contextFactory, consumer));
            }
            // TODO Throw?
        }

        @Nullable
        public <U> U unregisterHandler(PacketFlow flow) {
            NeoHandler handler = receivers.remove(flow);
            return handler != null ? (U) handler.original() : null;
        }
    }

    public record NeoHandler<C, T extends CustomPacketPayload, U>(U original, Function<IPayloadContext, C> contextFactory, BiConsumer<T, C> consumer) {
        public void handle(T payload, IPayloadContext context) {
            consumer.accept(payload, contextFactory.apply(context));
        }
    }
}
