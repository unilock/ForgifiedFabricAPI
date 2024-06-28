package org.sinytra.fabric.transfer_api;

import com.google.common.base.Suppliers;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.sinytra.fabric.transfer_api.compat.*;
import org.sinytra.fabric.transfer_api.generated.GeneratedEntryPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = GeneratedEntryPoint.MOD_ID)
public class TransferApiNeoCompat {
    private static final Map<Storage<?>, Supplier<?>> CAPS = new HashMap<>();
    /**
     * This lock has two purposes: avoiding recursive calls between {@link net.minecraft.world.level.Level#getCapability(BlockCapability, BlockPos, Object)}}
     * and {@link BlockApiLookup#find(net.minecraft.world.level.Level, net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, net.minecraft.world.level.block.entity.BlockEntity, Object) find} as well as influencing the
     * behavior of {@code find} if it was called from {@code getCapability}.
     * <p>
     * The recursive calls occur because our capabilities providers need to access the block lookup API to check if they
     * should provide a capability (for Fabric from Neo compat), but the block lookup API needs to query the
     * capabilities (for Neo from Fabric compat). This lock is set immediately before one API calls the other, which
     * then disables the call from the other API to the first, breaking the recursion.
     * <p>
     * Additionally, this lock is used to conditionally disable some of the block lookup API's fallback providers, if
     * they got invoked by a capability provider. This is needed because Fabric has fallback providers for many Vanilla
     * things, but Neo already implements their own compat for those.
     */
    public static final ThreadLocal<Boolean> COMPUTING_CAPABILITY_LOCK = ThreadLocal.withInitial(() -> false);

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    private static void onAttachBlockEntityCapabilities(RegisterCapabilitiesEvent event) {
        for (Block type : BuiltInRegistries.BLOCK) {
            event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, context) -> {
                    if (!COMPUTING_CAPABILITY_LOCK.get() && (blockEntity == null || blockEntity.hasLevel())) {
                        COMPUTING_CAPABILITY_LOCK.set(true);
                        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, state, blockEntity, context);
                        COMPUTING_CAPABILITY_LOCK.set(false);
                        if (storage != null) {
                            Supplier<? extends IItemHandler> supplier = (Supplier<? extends IItemHandler>) CAPS.computeIfAbsent(storage, s -> Suppliers.memoize(() -> storage instanceof SlottedStorage<ItemVariant> slotted ? new SlottedItemStorageItemHandler(slotted) : new ItemStorageItemHandler(storage)));
                            return supplier.get();
                        }
                    }
                    return null;
                },
                type
            );
        }
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                type,
                (be, side) -> {
                    if (!COMPUTING_CAPABILITY_LOCK.get() && be.hasLevel()) {
                        COMPUTING_CAPABILITY_LOCK.set(true);
                        Storage<FluidVariant> storage = FluidStorage.SIDED.find(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, side);
                        COMPUTING_CAPABILITY_LOCK.set(false);
                        if (storage != null) {
                            Supplier<? extends IFluidHandler> supplier = (Supplier<? extends IFluidHandler>) CAPS.computeIfAbsent(storage, s -> Suppliers.memoize(() -> new FluidStorageFluidHandler(storage)));
                            return supplier.get();
                        }
                    }
                    return null;
                }
            );
        }
        // TODO Find a way to pass interaction context
        // This is currently broken; when interacting forge blocks with fabric fluid containers, there is no way for
        // the container to access the player's inventory and empty itself. As a result, the fabric item provides an
        // infinite source of fluid.
//        for (Item item : BuiltInRegistries.ITEM) {
//            event.registerItem(
//                Capabilities.FluidHandler.ITEM,
//                (stack, ctx) -> {
//                    if (!COMPUTING_CAPABILITY_LOCK.get()) {
//                        COMPUTING_CAPABILITY_LOCK.set(true);
//                        Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
//                        COMPUTING_CAPABILITY_LOCK.set(false);
//                        if (storage != null) {
//                            Supplier<? extends IFluidHandlerItem> supplier = (Supplier<? extends IFluidHandlerItem>) CAPS.computeIfAbsent(storage, b -> Suppliers.memoize(() -> new FluidStorageFluidHandlerItem(storage, stack)));
//                            return supplier.get();
//                        }
//                    }
//                    return null;
//                },
//                item
//            );
//        }
    }

    public static void registerTransferApiFluidNeoBridge() {
        // FFAPI: Forge Capabilities fallback bridge
        FluidStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
            if (blockEntity != null && !COMPUTING_CAPABILITY_LOCK.get()) {
                COMPUTING_CAPABILITY_LOCK.set(true);
                Storage<FluidVariant> storage = Optional.ofNullable(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, blockEntity, direction))
                    .map(NeoFluidStorage::new)
                    .orElse(null);
                COMPUTING_CAPABILITY_LOCK.set(false);
                return storage;
            }
            return null;
        });
        FluidStorage.ITEM.registerFallback((stack, context) -> {
            if (stack != null && !COMPUTING_CAPABILITY_LOCK.get()) {
                COMPUTING_CAPABILITY_LOCK.set(true);
                Storage<FluidVariant> storage = Optional.ofNullable(stack.getCapability(Capabilities.FluidHandler.ITEM))
                    .map(NeoFluidStorage::new)
                    .orElse(null);
                COMPUTING_CAPABILITY_LOCK.set(false);
                return storage;
            }
            return null;
        });
    }

    public static void registerTransferApiItemNeoBridge() {
        // FFAPI: Forge Capabilities fallback bridge
        ItemStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
            if (blockEntity != null && !COMPUTING_CAPABILITY_LOCK.get()) {
                COMPUTING_CAPABILITY_LOCK.set(true);
                Storage<ItemVariant> storage = Optional.ofNullable(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, direction))
                    .map(NeoItemStorage::new)
                    .orElse(null);
                COMPUTING_CAPABILITY_LOCK.set(false);
                return storage;
            }
            return null;
        });
    }

    public static BlockApiLookup.BlockApiProvider<Storage<ItemVariant>, @Nullable Direction> wrapProviderSafely(BlockApiLookup.BlockApiProvider<Storage<ItemVariant>, @Nullable Direction> provider) {
        return (world, pos, state, blockEntity, direction) -> {
            // FFAPI: do not provide composter compat if queried from our capability provider
            if (TransferApiNeoCompat.COMPUTING_CAPABILITY_LOCK.get()) {
                return null;
            }
            return provider.find(world, pos, state, blockEntity, direction);
        };
    }
}
