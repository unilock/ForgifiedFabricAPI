package net.fabricmc.fabric.mixin.networking.accessor;

import io.netty.util.AttributeKey;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(NetworkRegistry.class)
public interface NetworkRegistryAccessor {
    @Accessor("PAYLOAD_REGISTRATIONS")
    static Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> getPayloadRegistrations() {
        throw new UnsupportedOperationException();
    }

    @Accessor("ATTRIBUTE_PAYLOAD_SETUP")
    static AttributeKey<NetworkPayloadSetup> getAttributePayloadSetup() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static boolean getSetup() {
        throw new UnsupportedOperationException();
    }

    @Accessor
    static void setSetup(boolean setup) {
        throw new UnsupportedOperationException();
    }
}
