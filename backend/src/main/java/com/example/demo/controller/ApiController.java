package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Path BASE_DIR = Paths.get(System.getProperty("user.home"), "safe-delete-root").toAbsolutePath().normalize();

    public record DeleteRequest(List<String> paths) {}
    public record DeleteResult(String path, boolean deleted, String error) {}
    public record DeleteResponse(List<DeleteResult> results) {}

    @PostMapping("/delete")
    public ResponseEntity<DeleteResponse> delete(@RequestBody DeleteRequest request) {
        List<DeleteResult> results = new ArrayList<>();
        if (request == null || request.paths() == null) {
            return ResponseEntity.badRequest().body(new DeleteResponse(results));
        }
        for (String raw : request.paths()) {
            results.add(deleteOne(raw));
        }
        return ResponseEntity.ok(new DeleteResponse(results));
    }

    private DeleteResult deleteOne(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DeleteResult(raw, false, "empty path");
        }
        try {
            Path target = BASE_DIR.resolve(raw).normalize();
            if (!target.startsWith(BASE_DIR)) {
                return new DeleteResult(raw, false, "path escapes base directory");
            }
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return new DeleteResult(raw, false, "not found");
            }
            if (Files.isSymbolicLink(target)) {
                Files.delete(target);
                return new DeleteResult(raw, true, null);
            }
            if (Files.isDirectory(target)) {
                try (var stream = Files.walk(target)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
                }
            } else {
                Files.delete(target);
            }
            return new DeleteResult(raw, true, null);
        } catch (Exception e) {
            return new DeleteResult(raw, false, e.getMessage());
        }
    }
}
