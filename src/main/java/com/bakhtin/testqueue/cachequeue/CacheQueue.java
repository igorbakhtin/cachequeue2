package com.bakhtin.testqueue.cachequeue;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache Queue for backend.
 * If the backend is alive, then send event directly
 * If the backend is dead, then send event to queue
 */
@Slf4j
public class CacheQueue<E> {
    private final BlockingQueue<E> queue;
    private final Sender<E> backend;
    private final AtomicBoolean backendIsAlive;

    /**
     * Creates a {@code CacheQueue} with the given (fixed) capacity.
     * @param backend the backend
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if {@code capacity} is not greater
     *         than zero
     */
    public CacheQueue(Sender<E> backend, int capacity) {
        this.backend = backend;
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.backendIsAlive = new AtomicBoolean(true);
        Executors.newSingleThreadScheduledExecutor().execute(this::sendFromQueue);
    }

    /**
     * Send event to backend or in queue
     * @param event the event
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     */
    public void send(E event) {
        if (!backendIsAlive.get() || !sendToBackend(event)) {
            log.debug("Backend id dead. {} added to queue", event);
            queue.add(event);
        }
    }

    private boolean sendToBackend(E event) {
        boolean result = backend.send(event);
        backendIsAlive.set(result);
        return result;
    }

    @SneakyThrows
    private void sendFromQueue() {
        while (true) {
            E event = queue.take();
            while (!sendToBackend(event)) {
                log.debug("Backend id dead.... wait 1 sec");
                Thread.sleep(1000);
            }
        }
    }
}
