package net.fabricmc.fabric.impl.client.particle;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.sinytra.fabric.particles.generated.GeneratedEntryPoint;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = GeneratedEntryPoint.MOD_ID)
public class ClientParticleEventHooks {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        ParticleFactoryRegistryImpl.INSTANCE.initialize(Minecraft.getInstance().particleEngine);
    }
}
