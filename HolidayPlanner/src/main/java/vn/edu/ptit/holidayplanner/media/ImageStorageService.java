package vn.edu.ptit.holidayplanner.media;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    StoredImage upload(MultipartFile file, ImageUploadKind kind);

    void delete(String publicId);

    boolean isAvailable();
}
