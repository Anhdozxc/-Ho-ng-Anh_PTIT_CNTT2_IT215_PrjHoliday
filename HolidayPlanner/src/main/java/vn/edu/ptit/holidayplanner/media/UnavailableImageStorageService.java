package vn.edu.ptit.holidayplanner.media;

import org.springframework.web.multipart.MultipartFile;

public class UnavailableImageStorageService implements ImageStorageService {
    @Override
    public StoredImage upload(MultipartFile file, ImageUploadKind kind) {
        throw new ImageStorageUnavailableException();
    }

    @Override
    public void delete(String publicId) {
        // The database reference can still be removed while cloud storage is offline/unconfigured.
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
