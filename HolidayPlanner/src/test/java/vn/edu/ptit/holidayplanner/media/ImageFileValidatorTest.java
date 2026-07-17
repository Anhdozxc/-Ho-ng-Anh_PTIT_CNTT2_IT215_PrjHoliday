package vn.edu.ptit.holidayplanner.media;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {
    private final ImageFileValidator validator = new ImageFileValidator();

    @Test
    void acceptsJpegPngAndWebpWithMatchingContent() {
        assertThatCode(() -> validator.validate(file("photo.jpg", "image/jpeg",
                bytes(0xff, 0xd8, 0xff, 0x00)))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("photo.png", "image/png",
                bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(file("photo.webp", "image/webp",
                bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsFilesLargerThanFiveMegabytes() {
        byte[] content = new byte[(int) ImageFileValidator.MAX_FILE_SIZE + 1];
        content[0] = (byte) 0xff;
        content[1] = (byte) 0xd8;
        content[2] = (byte) 0xff;

        assertThatThrownBy(() -> validator.validate(file("large.jpg", "image/jpeg", content)))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void rejectsWrongMimeExtensionAndSpoofedContent() {
        assertThatThrownBy(() -> validator.validate(file("photo.jpg", "text/plain",
                bytes(0xff, 0xd8, 0xff))))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("JPG");
        assertThatThrownBy(() -> validator.validate(file("photo.png", "image/jpeg",
                bytes(0xff, 0xd8, 0xff))))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("không khớp");
        assertThatThrownBy(() -> validator.validate(file("photo.jpg", "image/jpeg",
                "not-an-image".getBytes())))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("không phải ảnh");
    }

    @Test
    void rejectsMissingAndEmptyFiles() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ImageValidationException.class);
        assertThatThrownBy(() -> validator.validate(
                file("empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(ImageValidationException.class);
    }

    private MockMultipartFile file(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("image", filename, contentType, content);
    }

    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
