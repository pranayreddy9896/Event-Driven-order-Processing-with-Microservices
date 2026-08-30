package com.system.dlq.controller;

import com.system.dlq.model.DeadLetterMessage;
import com.system.dlq.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DlqAdminController {

    private final DlqService dlqService;

    @GetMapping("/messages")
    public ResponseEntity<List<DeadLetterMessage>> getMessages(@RequestParam(required = false) String status) {
        if ("POISONED".equalsIgnoreCase(status)) {
            return ResponseEntity.ok(dlqService.getPoisonedMessages());
        }
        return ResponseEntity.ok(dlqService.getAllMessages());
    }

    @PostMapping("/replay/{id}")
    public ResponseEntity<DeadLetterMessage> replay(@PathVariable String id) {
        return ResponseEntity.ok(dlqService.replayMessage(id));
    }

    @PostMapping("/replay-all")
    public ResponseEntity<Map<String, Object>> replayAll() {
        int count = dlqService.replayAll();
        return ResponseEntity.ok(Map.of("replayedCount", count, "status", "SUCCESS"));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<String> discard(@PathVariable String id) {
        dlqService.discardMessage(id);
        return ResponseEntity.ok("Message marked as DISCARDED");
    }
}
