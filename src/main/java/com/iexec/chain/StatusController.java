package com.iexec.chain;

import com.iexec.common.replicate.ReplicateStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok(Instant.now().toString());
    }

}
