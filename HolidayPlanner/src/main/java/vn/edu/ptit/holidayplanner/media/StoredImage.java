package vn.edu.ptit.holidayplanner.media;

public record StoredImage(String secureUrl, String publicId) {
    public StoredImage {
        if (secureUrl == null || secureUrl.isBlank() || publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Cloud image response is missing secure URL or public ID");
        }
    }
}
