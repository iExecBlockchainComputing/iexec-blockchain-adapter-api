package com.iexec.chain;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/metrics")
public class MetricController {

    @GetMapping
    public ResponseEntity<String> getMetrics() {
        return ResponseEntity.ok(Instant.now().toString());
    }

}