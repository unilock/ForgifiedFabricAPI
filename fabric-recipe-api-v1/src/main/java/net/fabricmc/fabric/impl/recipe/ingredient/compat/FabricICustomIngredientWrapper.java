package net.fabricmc.fabric.impl.recipe.ingredient.compat;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import java.util.List;

public class FabricICustomIngredientWrapper implements CustomIngredient {
    private final ICustomIngredient ingredient;

    public FabricICustomIngredientWrapper(ICustomIngredient ingredient) {
        this.ingredient = ingredient;
    }

    @Override
    public boolean test(ItemStack stack) {
        return this.ingredient.test(stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return this.ingredient.getItems().toList();
    }

    @Override
    public boolean requiresTesting() {
        return !this.ingredient.isSimple();
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        throw new UnsupportedOperationException();
    }
}
