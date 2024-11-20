package net.fabricmc.fabric.mixin.networking;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.HandlerNames;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;
import net.neoforged.neoforge.network.payload.SplitPacketPayload;
import org.sinytra.fabric.networking_api.NeoNetworkRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GenericPacketSplitter.class)
public class GenericPacketSplitterMixin {

    /*
     * Disable NeoForge packet splitting for Fabric packets
     */

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    public void encode(ChannelHandlerContext ctx, Packet<?> packet, List<Object> out, CallbackInfo ci) {
        if (packet instanceof ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
            if (ctx.pipeline().get(HandlerNames.ENCODER) instanceof PacketEncoder<?> encoder) {
                var registry = NeoNetworkRegistrar.getPayloadRegistry(encoder.getProtocolInfo().id(), PacketFlow.CLIENTBOUND);
                if (registry.get(clientboundCustomPayloadPacket.payload().type()) != null) {
                    out.add(packet);
                    ci.cancel();
                }
            }
        } else if (packet instanceof ServerboundCustomPayloadPacket serverboundCustomPayloadPacket) {
            if (ctx.pipeline().get(HandlerNames.ENCODER) instanceof PacketEncoder<?> encoder) {
                var registry = NeoNetworkRegistrar.getPayloadRegistry(encoder.getProtocolInfo().id(), PacketFlow.SERVERBOUND);
                if (registry.get(serverboundCustomPayloadPacket.payload().type()) != null) {
                    out.add(packet);
                    ci.cancel();
                }
            }
        }
    }

}
