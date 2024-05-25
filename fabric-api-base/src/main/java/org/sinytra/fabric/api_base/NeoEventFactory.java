package org.sinytra.fabric.api_base;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.impl.base.event.EventFactoryImpl;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class NeoEventFactory {
    /**
     * Create an "array-backed" Event instance that listens to Forge events.
     *
     * <p>If your factory simply delegates to the listeners without adding custom behavior,
     * consider using {@link EventFactory#createArrayBacked(Class, Object, Function) the other overload}
     * if performance of this event is critical.
     *
     * @param type           The listener class type.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param <T>            The listener type.
     * @return The Event instance.
     * 
     * @author Matyrobbrt
     */
    @ApiStatus.Internal
    public static <T, EV extends net.neoforged.bus.api.Event> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory, Class<? extends EV> neoType, BiConsumer<EV, T> listener) {
        final var event = EventFactoryImpl.createArrayBacked(type, invokerFactory);
        NeoForge.EVENT_BUS.addListener(neoType, ev -> listener.accept(ev, event.invoker()));
        return event;
    }
}
