package net.fabricmc.fabric.impl.recipe.ingredient.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.sinytra.fabric.recipe_api.FabricRecipeApiV1;

import java.util.Objects;
import java.util.stream.Stream;

public record NeoCustomIngredientWrapper(CustomIngredient ingredient) implements ICustomIngredient {
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomIngredient> CUSTOM_INGREDIENT_SERIALIZER_STREAM_CODEC = ResourceLocation.STREAM_CODEC
        .<RegistryFriendlyByteBuf>cast()
        .dispatch(i -> i.getSerializer().getIdentifier(), l -> Objects.requireNonNull(CustomIngredientSerializer.get(l)).getPacketCodec());
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoCustomIngredientWrapper> STREAM_CODEC = StreamCodec.composite(
        CUSTOM_INGREDIENT_SERIALIZER_STREAM_CODEC,
        w -> w.ingredient,
        NeoCustomIngredientWrapper::new
    );
    public static final Codec<CustomIngredient> CUSTOM_INGREDIENT_CODEC = CustomIngredientImpl.CODEC.dispatch(CustomIngredient::getSerializer, s -> s.getCodec(true));
    public static final MapCodec<NeoCustomIngredientWrapper> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CUSTOM_INGREDIENT_CODEC.fieldOf("ingredient").forGetter(w -> w.ingredient)
    ).apply(instance, NeoCustomIngredientWrapper::new));

    @Override
    public boolean test(ItemStack arg) {
        return this.ingredient.test(arg);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return this.ingredient.getMatchingStacks().stream();
    }

    @Override
    public boolean isSimple() {
        return !this.ingredient.requiresTesting();
    }

    @Override
    public IngredientType<?> getType() {
        return FabricRecipeApiV1.FABRIC_INGREDIENT_WRAPPER.get();
    }
}
