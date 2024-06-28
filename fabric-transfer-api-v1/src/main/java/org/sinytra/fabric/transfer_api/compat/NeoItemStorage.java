package org.sinytra.fabric.transfer_api.compat;

import com.google.common.primitives.Ints;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NeoItemStorage implements Storage<ItemVariant> {
    private final IItemHandler handler;

    public NeoItemStorage(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        int normalMaxAmount = Ints.saturatedCast(maxAmount);
        int inserted = ItemHandlerHelper.insertItem(this.handler, resource.toStack(normalMaxAmount), true).getCount();
        transaction.addCloseCallback((context, result) -> {
            if (result.wasCommitted()) {
                ItemHandlerHelper.insertItem(this.handler, resource.toStack(normalMaxAmount), false);
            }
        });
        return normalMaxAmount - inserted;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        int normalMaxAmount = Ints.saturatedCast(maxAmount);
        int extracted = extractItem(this.handler, resource, normalMaxAmount, true).getCount();
        transaction.addCloseCallback((context, result) -> {
            if (result.wasCommitted()) {
                extractItem(this.handler, resource, normalMaxAmount, false);
            }
        });
        return extracted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (int i = 0; i < this.handler.getSlots(); i++) {
            views.add(new NeoItemView(this.handler, i));
        }
        return views.iterator();
    }

    @NotNull
    public static ItemStack extractItem(IItemHandler dest, @NotNull ItemVariant variant, int maxAmount, boolean simulate) {
        if (dest == null)
            return ItemStack.EMPTY;

        int total = 0;
        for (int i = 0; i < dest.getSlots(); i++) {
            ItemStack available = dest.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(available, variant.toStack())) {
                total += dest.extractItem(i, maxAmount - total, simulate).getCount();
            }
        }

        return variant.toStack(total);
    }
}

