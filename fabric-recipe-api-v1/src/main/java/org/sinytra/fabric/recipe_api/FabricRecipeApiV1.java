package org.sinytra.fabric.recipe_api;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.fabricmc.fabric.impl.recipe.ingredient.compat.NeoCustomIngredientWrapper;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.sinytra.fabric.recipe_api.generated.GeneratedEntryPoint;

import java.util.function.Function;
import java.util.stream.Stream;

public class FabricRecipeApiV1 implements ModInitializer {
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, GeneratedEntryPoint.MOD_ID);

//    public static final DeferredHolder<IngredientType<?>, IngredientType<?>> CUSTOM_DATA_INGREDIENT_TYPE = INGREDIENT_TYPES.register("custom_data", () -> new IngredientType<>(CustomDataIngredient.Serializer.ALLOW_EMPTY_CODEC, CustomDataIngredient.Serializer.PACKET_CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<NeoCustomIngredientWrapper>> FABRIC_INGREDIENT_WRAPPER = INGREDIENT_TYPES.register("fabric_wrapper", () -> new IngredientType<>(NeoCustomIngredientWrapper.CODEC, NeoCustomIngredientWrapper.STREAM_CODEC));

    @Override
    public void onInitialize() {
        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
        INGREDIENT_TYPES.register(bus);
    }

    public static MapCodec<Ingredient> makeIngredientMapCodec(MapCodec<Ingredient> original) {
        return FabricRecipeApiV1.<CustomIngredientSerializer<?>, CustomIngredient, Ingredient>dispatchMapOrElse(
                CustomIngredientImpl.TYPE_KEY,
                CustomIngredientImpl.CODEC,
                CustomIngredient::getSerializer,
                s -> s.getCodec(true),
                original
            )
            .xmap(
                e -> e.map(c -> new NeoCustomIngredientWrapper(c).toVanilla(), Function.identity()),
                s -> s.getCustomIngredient() instanceof NeoCustomIngredientWrapper wrapper ? Either.left(wrapper.ingredient()) : Either.right(s)
            );
    }

    // Stolen from NeoForgeExtraCodecs because they don't support custom type keys
    public static <A, E, B> MapCodec<Either<E, B>> dispatchMapOrElse(String typeKey, Codec<A> typeCodec, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec, MapCodec<B> fallbackCodec) {
        var dispatchCodec = typeCodec.dispatchMap(typeKey, type, codec);
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.concat(dispatchCodec.keys(ops), fallbackCodec.keys(ops)).distinct();
            }

            @Override
            public <T> DataResult<Either<E, B>> decode(DynamicOps<T> ops, MapLike<T> input) {
                if (input.get(typeKey) != null) {
                    return dispatchCodec.decode(ops, input).map(Either::left);
                } else {
                    return fallbackCodec.decode(ops, input).map(Either::right);
                }
            }

            @Override
            public <T> RecordBuilder<T> encode(Either<E, B> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.map(
                    dispatched -> dispatchCodec.encode(dispatched, ops, prefix),
                    fallback -> fallbackCodec.encode(fallback, ops, prefix));
            }

            @Override
            public String toString() {
                return "DispatchOrElse[" + dispatchCodec + ", " + fallbackCodec + "]";
            }
        };
    }
}
