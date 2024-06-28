package org.sinytra.fabric.transfer_api.compat;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record NeoFluidView(IFluidHandler handler, int tank) implements StorageView<FluidVariant> {
    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        FluidStack stack = getStack();
        if (!stack.isEmpty() && resource.isOf(stack.getFluid()) && resource.componentsMatch(stack.getComponentsPatch())) {
            FluidStack existing = stack.copyWithAmount(NeoCompatUtil.toForgeBucket((int) maxAmount));
            FluidStack drained = handler.drain(existing, IFluidHandler.FluidAction.SIMULATE);
            transaction.addCloseCallback((context, result) -> {
                if (result.wasCommitted()) {
                    handler.drain(existing, IFluidHandler.FluidAction.EXECUTE);
                }
            });
            return NeoCompatUtil.toFabricBucket(drained.getAmount());
        }
        return 0;
    }

    @Override
    public boolean isResourceBlank() {
        return handler.getFluidInTank(tank).isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        return NeoCompatUtil.toFluidStorageView(getStack());
    }

    @Override
    public long getAmount() {
        return NeoCompatUtil.toFabricBucket(getStack().getAmount());
    }

    @Override
    public long getCapacity() {
        return NeoCompatUtil.toFabricBucket(handler.getTankCapacity(tank));
    }

    private FluidStack getStack() {
        return handler.getFluidInTank(tank);
    }
}

