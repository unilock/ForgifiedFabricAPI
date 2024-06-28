package org.sinytra.fabric.transfer_api.compat;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public final class NeoCompatUtil {
    public static int toForgeBucket(int amount) {
        return (int) (amount / (double) FluidConstants.BUCKET * FluidType.BUCKET_VOLUME);
    }

    public static int toFabricBucket(int amount) {
        return (int) (amount / (double) FluidType.BUCKET_VOLUME * FluidConstants.BUCKET);
    }

    public static FluidStack toForgeFluidStack(StorageView<FluidVariant> view) {
        if (view != null && !view.isResourceBlank()) {
            FluidVariant resource = view.getResource();
            return new FluidStack(resource.getRegistryEntry(), toForgeBucket((int) view.getAmount()), resource.getComponents());
        }
        return FluidStack.EMPTY;
    }

    public static FluidStack toForgeFluidStack(FluidVariant variant, int amount) {
        return !variant.isBlank() && amount > 0 ? new FluidStack(variant.getRegistryEntry(), toForgeBucket(amount), variant.getComponents()) : FluidStack.EMPTY;
    }

    public static FluidVariant toFluidStorageView(FluidStack stack) {
        return !stack.isEmpty() ? FluidVariant.of(stack.getFluid(), stack.getComponentsPatch()) : FluidVariant.blank();
    }
}
