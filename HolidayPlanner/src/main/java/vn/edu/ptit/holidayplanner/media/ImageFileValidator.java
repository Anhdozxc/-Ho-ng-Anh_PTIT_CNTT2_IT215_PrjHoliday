package vn.edu.ptit.holidayplanner.media;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ImageFileValidator {
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.copyOf(ALLOWED_EXTENSIONS.values());

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException("Vui lòng chọn một file ảnh");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageValidationException("Ảnh không được vượt quá 5 MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageValidationException("Ảnh chỉ hỗ trợ định dạng JPG, JPEG, PNG hoặc WEBP");
        }

        String extension = extension(file.getOriginalFilename());
        if (!contentType.equals(ALLOWED_EXTENSIONS.get(extension))) {
            throw new ImageValidationException("Phần mở rộng file không khớp với định dạng ảnh");
        }

        byte[] header;
        try {
            header = file.getBytes();
        } catch (IOException exception) {
            throw new ImageValidationException("Không thể đọc file ảnh", exception);
        }
        if (!hasValidSignature(contentType, header)) {
            throw new ImageValidationException("Nội dung file không phải ảnh hợp lệ");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parametersStart = contentType.indexOf(';');
        String value = parametersStart >= 0 ? contentType.substring(0, parametersStart) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        String normalized = filename.trim().toLowerCase(Locale.ROOT);
        int dot = normalized.lastIndexOf('.');
        return dot < 0 || dot == normalized.length() - 1 ? "" : normalized.substring(dot + 1);
    }

    private boolean hasValidSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case "image/webp" -> bytes.length >= 12
                    && startsWith(bytes, 0x52, 0x49, 0x46, 0x46)
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xff) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
