package com.bakhtin.txtme.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MyRealBackend<E>{

    private boolean isBackendOnline;
    private AtomicInteger completedRequests = new AtomicInteger() ;

    /**
     * Send event to backend
     * @param event - the event
     * @return false if backend is dead and we need more attempts to send data
     * true if all ok
     * In case of business errors, true should be returned
     */
    public boolean send(E event) {
        if (isBackendOnline) {
            int count = completedRequests.incrementAndGet();
            log.debug("Backend. Sent {} complete. count {}", event, count);
            return true;
        } else {
            log.debug("Backend. Try sent {} - Backend is dead", event);
            return false;
        }
    }

    public void clearCompletedRequests() {
        completedRequests.set(0);
    }

    public int getCompletedRequests() {
        return completedRequests.get();
    }

    public void setBackendOnline(boolean isBackendOnline) {
        this.isBackendOnline = isBackendOnline;
        log.debug("=============== Backend is online: {} =============== ", this.isBackendOnline);
    }
}
