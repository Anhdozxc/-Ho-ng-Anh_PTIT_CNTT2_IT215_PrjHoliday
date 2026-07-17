package vn.edu.ptit.holidayplanner.media;

public class ImageStorageUnavailableException extends ImageStorageException {
    public ImageStorageUnavailableException() {
        super("Tải ảnh lên chưa khả dụng vì Cloudinary chưa được cấu hình");
    }
}
