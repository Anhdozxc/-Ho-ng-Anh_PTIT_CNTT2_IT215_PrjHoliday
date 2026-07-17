package vn.edu.ptit.holidayplanner.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CloudinaryImageStorageService implements ImageStorageService {
    private final Cloudinary cloudinary;
    private final String rootFolder;

    public CloudinaryImageStorageService(Cloudinary cloudinary, String rootFolder) {
        this.cloudinary = cloudinary;
        this.rootFolder = normalizeFolder(rootFolder);
    }

    @Override
    public StoredImage upload(MultipartFile file, ImageUploadKind kind) {
        String publicId = rootFolder + "/" + kind.folder() + "/" + UUID.randomUUID();
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("public_id", publicId);
        options.put("overwrite", false);
        options.put("unique_filename", false);
        options.put("transformation", kind.transformation());

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            return new StoredImage(value(result.get("secure_url")), value(result.get("public_id")));
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException("Không thể tải ảnh lên Cloudinary. Ảnh hiện tại vẫn được giữ nguyên", exception);
        }
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        if (!isOwnedPublicId(publicId)) {
            throw new ImageStorageException("Không thể xóa ảnh nằm ngoài thư mục quản lý của ứng dụng");
        }
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException exception) {
            throw new ImageStorageException("Không thể xóa ảnh cũ khỏi Cloudinary", exception);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private boolean isOwnedPublicId(String publicId) {
        return publicId.startsWith(rootFolder + "/avatars/")
                || publicId.startsWith(rootFolder + "/destinations/");
    }

    private String normalizeFolder(String folder) {
        String value = folder == null || folder.isBlank() ? "holiday-planner" : folder.trim();
        value = value.replace('\\', '/').replaceAll("/+", "/");
        value = value.replaceAll("^/+|/+$", "");
        value = value.replaceAll("[^A-Za-z0-9_/-]", "-");
        return value.isBlank() ? "holiday-planner" : value;
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }
}
