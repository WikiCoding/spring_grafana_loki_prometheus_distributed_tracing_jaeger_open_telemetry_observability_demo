package com.wikicoding.grafanalokidemo.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeController {
    private final Logger log = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/hello")
    public ResponseEntity<Object> getHello() {
        log.info("Hello endpoint log");
        return ResponseEntity.ok("Hello");
    }

    @GetMapping("/world")
    public ResponseEntity<Object> getWorld() {
        log.info("World endpoint log");
        return ResponseEntity.ok("World");
    }
}
