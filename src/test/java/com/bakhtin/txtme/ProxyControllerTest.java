package com.bakhtin.txtme;

import com.bakhtin.txtme.backend.MyRealBackend;
import com.bakhtin.txtme.dto.MyEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class ProxyControllerTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    @LocalServerPort
    private int port;
    private URL base;
    @Autowired
    private TestRestTemplate template;

    @Autowired
    private MyRealBackend<MyEvent> backend;

    @Value("${cachequeue.capacity:100}")
    private int capacity;

    @Test
    @SneakyThrows
    void checkWhenBackendAlive() {

        backend.setBackendOnline(true);
        backend.clearCompletedRequests();

        int send = capacity * 2;
        AtomicInteger successPostEvent = postEvents(send);

        Thread.sleep(1000);
        log.debug("checkWhenBackendAlive - Events: sent to proxy {}, add to front {}, sent to back {}",
                send, successPostEvent.get(), backend.getCompletedRequests());
        assertEquals(successPostEvent.get(), backend.getCompletedRequests());
        assertEquals(send, backend.getCompletedRequests());
    }

    @Test
    @SneakyThrows
    void checkCapacityWhenBackendDead() {
        backend.setBackendOnline(false);
        backend.clearCompletedRequests();

        // отправляем больше чем размер очереди
        int oversize = 10;
        int send = capacity + oversize;
        AtomicInteger successPostEvent = postEvents(send);

        // задержка чтобы посмотреть как отрабатывает retry
        Thread.sleep(3000);

        backend.setBackendOnline(true);
        Thread.sleep(1000);

        log.debug("checkCapacityWhenBackendDead - Events: sent to proxy {}, add to front {}, sent to back {}",
                send, successPostEvent.get(), backend.getCompletedRequests());
        assertEquals(successPostEvent.get(), backend.getCompletedRequests());
        // часть не вошла в очередь - потеряли
        assertEquals(send - oversize + 1, backend.getCompletedRequests());
    }

    @Test
    @SneakyThrows
    void checkWhenBackendAliveThenDead() {
        backend.setBackendOnline(true);
        backend.clearCompletedRequests();

        int aliveCount = 10;
        AtomicInteger successPostEvent1 = postEvents(aliveCount);

        Thread.sleep(1000);

        backend.setBackendOnline(false);

        int deadCount = 20;
        AtomicInteger successPostEvent2 = postEvents(deadCount);

        // задержка чтобы посмотреть как отрабатывает retry
        Thread.sleep(3000);
        backend.setBackendOnline(true);

        Thread.sleep(1000);

        int successPostEvent = successPostEvent1.get() + successPostEvent2.get();
        log.debug("checkWhenBackendAliveThenDead - Events: sent to proxy {}, add to front {}, sent to back {}",
                aliveCount + deadCount, successPostEvent, backend.getCompletedRequests());
        assertEquals(successPostEvent, backend.getCompletedRequests());
    }

    @SneakyThrows
    AtomicInteger postEvents(int eventCount) {
        AtomicInteger successPostEvent = new AtomicInteger();
        base = new URL("http://localhost:" + port + "/");

        for (int i = 0; i < eventCount; i++) {
            int id = i;
            executor.execute(() -> {

                MyEvent event = new MyEvent(id);
                ResponseEntity<String> result = template.postForEntity(base.toString(), event, String.class);
                if (result.getStatusCodeValue() == 200) {
                    successPostEvent.getAndIncrement();
                }
            });
        }
        return successPostEvent;
    }
}