package com.bakhtin.testqueue.cachequeue;

@FunctionalInterface
public interface Sender<T> {
    /**
     * Send event
     * @param t - the event
     * @return false if backend is dead and we need more attempts to send data
     * true if all ok
     * In case of business errors, true should be returned
     */
    boolean send(T t);
}
