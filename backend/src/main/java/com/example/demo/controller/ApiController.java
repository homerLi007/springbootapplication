package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @PostMapping("/delete")
    public ResponseEntity<?> deleteFiles(@RequestBody DeleteRequest request) {
        List<String> deleted = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        if (request == null || request.paths == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "paths is required"));
        }

        for (String raw : request.paths) {
            if (raw == null || raw.isBlank()) {
                failed.add(String.valueOf(raw));
                continue;
            }
            try {
                Path path = Paths.get(raw).toAbsolutePath().normalize();
                if (Files.deleteIfExists(path)) {
                    deleted.add(path.toString());
                } else {
                    failed.add(path + " (not found)");
                }
            } catch (IOException | RuntimeException e) {
                failed.add(raw + " (" + e.getMessage() + ")");
            }
        }

        return ResponseEntity.ok(Map.of(
                "deleted", deleted,
                "failed", failed
        ));
    }

    public static class DeleteRequest {
        public List<String> paths;
    }
}
