package org.sinytra.fabric.transfer_api.compat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class NeoFluidStorage implements Storage<FluidVariant> {
    private final IFluidHandler handler;

    public NeoFluidStorage(IFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        FluidStack stack = NeoCompatUtil.toForgeFluidStack(resource, (int) maxAmount);
        int filled = handler.fill(stack, IFluidHandler.FluidAction.SIMULATE);
        transaction.addCloseCallback((context, result) -> {
            if (result.wasCommitted()) {
                handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            }
        });
        return NeoCompatUtil.toFabricBucket(filled);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        FluidStack stack = NeoCompatUtil.toForgeFluidStack(resource, (int) maxAmount);
        FluidStack drained = handler.drain(stack, IFluidHandler.FluidAction.SIMULATE);
        transaction.addCloseCallback((context, result) -> {
            if (result.wasCommitted()) {
                handler.drain(stack, IFluidHandler.FluidAction.EXECUTE);
            }
        });
        return NeoCompatUtil.toFabricBucket(drained.getAmount());
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        for (int i = 0; i < handler.getTanks(); i++) {
            views.add(new NeoFluidView(handler, i));
        }
        return views.iterator();
    }
}
