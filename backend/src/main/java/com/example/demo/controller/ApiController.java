package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final Path baseDir;

    public ApiController(@Value("${app.delete.base-dir}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFiles(@RequestBody DeleteRequest request, Authentication authentication) {
        List<String> deleted = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        if (request == null || request.paths() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "paths is required"));
        }

        String principal = authentication != null ? authentication.getName() : "unknown";

        Path realBase;
        try {
            realBase = baseDir.toRealPath();
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "base directory unavailable"));
        }

        for (String raw : request.paths()) {
            if (raw == null || raw.isBlank()) {
                failed.add(String.valueOf(raw));
                continue;
            }
            try {
                Path target = realBase.resolve(raw).normalize();
                if (!target.startsWith(realBase)) {
                    failed.add(raw);
                    continue;
                }
                Path relative = realBase.relativize(target);
                if (relative.getNameCount() == 0) {
                    failed.add(raw);
                    continue;
                }
                if (!deleteViaSecureDirectoryStream(realBase, relative)) {
                    failed.add(raw);
                    continue;
                }
                log.info("Deleted file '{}' by user '{}'", relative, principal);
                deleted.add(raw);
            } catch (IOException | InvalidPathException e) {
                failed.add(raw);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        body.put("failed", failed);
        return ResponseEntity.ok(body);
    }

    private static boolean deleteViaSecureDirectoryStream(Path realBase, Path relative) throws IOException {
        Path parentRelative = relative.getNameCount() == 1 ? null : relative.getParent();
        Path fileName = relative.getFileName();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(realBase)) {
            if (!(ds instanceof SecureDirectoryStream<Path> secureBase)) {
                // KNOWN LIMITATION: SecureDirectoryStream is not supported on this filesystem; TOCTOU window cannot be fully closed.
                return false;
            }
            SecureDirectoryStream<Path> current = secureBase;
            try {
                if (parentRelative != null) {
                    for (Path part : parentRelative) {
                        SecureDirectoryStream<Path> next = current.newDirectoryStream(part, LinkOption.NOFOLLOW_LINKS);
                        if (current != secureBase) {
                            current.close();
                        }
                        current = next;
                    }
                }
                BasicFileAttributes attrs = current.getFileAttributeView(fileName, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
                if (!attrs.isRegularFile()) {
                    return false;
                }
                current.deleteFile(fileName);
                return true;
            } finally {
                if (current != secureBase) {
                    current.close();
                }
            }
        }
    }

    public record DeleteRequest(List<String> paths) {}
}
