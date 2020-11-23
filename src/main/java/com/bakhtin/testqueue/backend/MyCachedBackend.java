package com.bakhtin.testqueue.backend;

import com.bakhtin.testqueue.cachequeue.CacheQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MyCachedBackend<E>{
    private final CacheQueue<E> queue;

    public MyCachedBackend(@Autowired MyRealBackend<E> backend, @Value("${cachequeue.capacity:100}") int capacity) {
        this.queue = new CacheQueue<>(backend::send, capacity);
    }

    public void send(E event) {
         queue.send(event);
    }
}
