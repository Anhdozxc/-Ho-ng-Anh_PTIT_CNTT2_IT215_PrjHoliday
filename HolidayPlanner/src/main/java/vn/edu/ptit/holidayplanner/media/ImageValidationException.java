package vn.edu.ptit.holidayplanner.media;

public class ImageValidationException extends IllegalArgumentException {
    public ImageValidationException(String message) {
        super(message);
    }

    public ImageValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
