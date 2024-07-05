package net.fabricmc.fabric.mixin.networking.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkRegistry.class)
public class NetworkRegistryClientMixin {
    @Inject(method = "initializeNeoForgeConnection(Lnet/minecraft/network/protocol/configuration/ClientConfigurationPacketListener;Lnet/neoforged/neoforge/network/registration/NetworkPayloadSetup;)V", at = @At("TAIL"))
    private static void startConfiguration(ClientConfigurationPacketListener listener, NetworkPayloadSetup setup, CallbackInfo ci) {
        ClientConfigurationConnectionEvents.START.invoker().onConfigurationStart((ClientConfigurationPacketListenerImpl) listener, Minecraft.getInstance());
    }
}
