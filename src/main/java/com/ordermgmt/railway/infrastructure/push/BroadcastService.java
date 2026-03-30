package com.ordermgmt.railway.infrastructure.push;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Broadcasts events to all connected Vaadin UIs for live updates.
 * Register listeners in view's onAttach(), unregister in onDetach().
 * Always update UI inside ui.access(() -> { ... }).
 */
@Service
public class BroadcastService {

    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Registration register(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> {
            CopyOnWriteArrayList<Consumer<?>> list = listeners.get(eventType);
            if (list != null) {
                list.remove(listener);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <T> void broadcast(T event) {
        CopyOnWriteArrayList<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            list.forEach(listener -> ((Consumer<T>) listener).accept(event));
        }
    }
}
