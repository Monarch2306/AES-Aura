package com.aes.answersheets;

import com.aes.common.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorageService {

    private final long maxSizeBytes;
    private final Set<String> allowedMimeTypes;
    private final Path storageDir;

    public FileStorageService(
            @Value("${app.upload.max-size-bytes}") long maxSizeBytes,
            @Value("${app.upload.allowed-mime-types}") String allowedMimeTypesCsv,
            @Value("${app.upload.storage-dir}") String storageDirPath
    ) {
        this.maxSizeBytes = maxSizeBytes;
        this.allowedMimeTypes = Set.of(allowedMimeTypesCsv.split(","));
        this.storageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload storage directory: " + storageDir, e);
        }
    }

    /** Phase 16: MIME validation, size validation, safe (UUID-based) filenames,
     * no executable uploads (extension whitelist derived from MIME type). */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException("File exceeds maximum allowed size of " + (maxSizeBytes / (1024 * 1024)) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new BadRequestException("Unsupported file type: " + contentType +
                    ". Allowed types: " + String.join(", ", allowedMimeTypes));
        }

        String extension = extensionFor(contentType);
        try {
            validateMagicBytes(file, contentType);
        } catch (IOException e) {
            throw new BadRequestException("Unable to validate uploaded file");
        }
        String safeName = UUID.randomUUID() + extension; // never trust the client-supplied filename
        Path target = storageDir.resolve(safeName).normalize();
        if (!target.startsWith(storageDir)) {
            throw new BadRequestException("Invalid file path"); // path traversal guard
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store uploaded file: " + e.getMessage());
        }

        return new StoredFile(target.toString(), safeName, contentType, file.getSize(),
                file.getOriginalFilename() != null ? sanitizeDisplayName(file.getOriginalFilename()) : safeName);
    }

    private String sanitizeDisplayName(String name) {
        // Keep only a safe display name (no path components), purely cosmetic.
        String base = Paths.get(name).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }

    private void validateMagicBytes(MultipartFile file, String contentType) throws IOException {
        byte[] b = file.getBytes();
        boolean valid = switch (contentType) {
            case "image/jpeg" -> b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
            case "image/png" -> b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47
                    && b[4] == 0x0d && b[5] == 0x0a && b[6] == 0x1a && b[7] == 0x0a;
            case "image/webp" -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            case "application/pdf" -> b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
            default -> false;
        };
        if (!valid) throw new BadRequestException("File content does not match declared type (" + contentType + ")");
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    public record StoredFile(String absolutePath, String storedName, String mimeType, long size, String originalDisplayName) {}
}
