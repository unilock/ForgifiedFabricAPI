package net.fabricmc.fabric.mixin.recipe.ingredient;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import org.sinytra.fabric.recipe_api.FabricRecipeApiV1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftingHelper.class)
public class CraftingHelperMixin {

    @ModifyReturnValue(method = "makeIngredientMapCodec", at = @At("RETURN"))
    private static MapCodec<Ingredient> modifyIngredientCodec(MapCodec<Ingredient> original) {
        return FabricRecipeApiV1.makeIngredientMapCodec(original);
    }
}
