package com.wikicoding.distributed.tracing.servicec.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerApp2 {
    @KafkaListener(topics = "topic1", groupId = "app-2")
    public void listen(String message) {
            log.info("App 2 received message: {}", message);
            log.info("App 2 finished processing the message");
    }
}

