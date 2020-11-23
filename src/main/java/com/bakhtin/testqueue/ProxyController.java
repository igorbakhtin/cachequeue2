package com.bakhtin.testqueue;

import com.bakhtin.testqueue.backend.MyCachedBackend;
import com.bakhtin.testqueue.dto.MyEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ProxyController {

    private MyCachedBackend<MyEvent> cachedRealBackend;

    @PostMapping("/")
    public void addEvent(@RequestBody MyEvent myEvent) {
        cachedRealBackend.send(myEvent);
    }
}

