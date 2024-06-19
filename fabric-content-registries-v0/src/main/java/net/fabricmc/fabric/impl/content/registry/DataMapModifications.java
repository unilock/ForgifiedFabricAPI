package net.fabricmc.fabric.impl.content.registry;

import net.fabricmc.fabric.api.registry.VillagerInteractionRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class DataMapModifications {
    @SuppressWarnings("unchecked")
    public static <A, T> void modify(Registry<T> registry, DataMapType<T, A> type, ResourceKey<T> key, CallbackInfoReturnable<A> cir) {
        if (type == NeoForgeDataMaps.COMPOSTABLES) {
            registry.getHolder(key).ifPresent(holder -> {
                var fromCustom = CompostingChanceRegistryImpl.CUSTOM.get(holder.value());
                if (fromCustom != null) {
                    if (fromCustom < 0) {
                        cir.setReturnValue(null);
                    } else {
                        cir.setReturnValue((A)new Compostable(fromCustom));
                    }
                }
            });
        } else if (type == NeoForgeDataMaps.RAID_HERO_GIFTS) {
            registry.getHolder(key).ifPresent(holder -> {
                var fromCustom = VillagerInteractionRegistries.MODIFIED_GIFTS.get(holder.value());
                if (fromCustom != null) {
                    cir.setReturnValue((A) new RaidHeroGift(fromCustom));
                }
            });
        }
    }
}
