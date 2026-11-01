package com.example.demo.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Path BASE_DIR = Paths.get("/var/app/uploads").toAbsolutePath().normalize();

    public record DeleteRequest(@NotNull @NotEmpty List<String> paths) {}

    @PostMapping("/delete")
    @PreAuthorize("hasRole('FILE_ADMIN')")
    public ResponseEntity<?> deleteFiles(@RequestBody @Valid DeleteRequest body) {
        List<String> paths = body.paths();
        List<String> deleted = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String p : paths) {
            if (p == null || p.isBlank()) {
                failed.add(String.valueOf(p));
                continue;
            }
            try {
                Path candidate = BASE_DIR.resolve(p).normalize();
                if (!candidate.startsWith(BASE_DIR) || candidate.equals(BASE_DIR)) {
                    failed.add(p + " (outside allowed directory)");
                    continue;
                }
                if (hasSymlinkComponent(candidate)) {
                    failed.add(p + " (symlink not allowed)");
                    continue;
                }
                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(candidate, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                } catch (IOException e) {
                    failed.add(p + " (not found)");
                    continue;
                }
                if (attrs.isSymbolicLink()) {
                    failed.add(p + " (symlink not allowed)");
                    continue;
                }
                if (!attrs.isRegularFile()) {
                    failed.add(p + " (not a regular file)");
                    continue;
                }
                Files.delete(candidate);
                deleted.add(candidate.toString());
            } catch (IOException | SecurityException e) {
                failed.add(p + " (" + e.getMessage() + ")");
            }
        }

        return ResponseEntity.ok(Map.of(
                "deleted", deleted,
                "failed", failed
        ));
    }

    private static boolean hasSymlinkComponent(Path candidate) throws IOException {
        Path current = BASE_DIR;
        Path relative = BASE_DIR.relativize(candidate);
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }
}
