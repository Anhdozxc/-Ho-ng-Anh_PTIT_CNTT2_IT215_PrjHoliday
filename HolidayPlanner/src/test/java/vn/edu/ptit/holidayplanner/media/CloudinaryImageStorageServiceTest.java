package vn.edu.ptit.holidayplanner.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageServiceTest {
    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    private CloudinaryImageStorageService service;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
        service = new CloudinaryImageStorageService(cloudinary, "/holiday planner/");
    }

    @Test
    void uploadStoresSecureUrlPublicIdAndSafeServerGeneratedOptions() throws IOException {
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://media.example/avatar.jpg",
                "public_id", "holiday-planner/avatars/generated"));

        StoredImage result = service.upload(jpeg("untrusted file name.jpg"), ImageUploadKind.AVATAR);

        assertThat(result.secureUrl()).isEqualTo("https://media.example/avatar.jpg");
        assertThat(result.publicId()).isEqualTo("holiday-planner/avatars/generated");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertThat(options.getValue().get("resource_type")).isEqualTo("image");
        assertThat(options.getValue().get("overwrite")).isEqualTo(false);
        assertThat(options.getValue().get("public_id").toString())
                .startsWith("holiday-planner/avatars/")
                .doesNotContain("untrusted file name");
        assertThat(options.getValue().get("transformation").toString()).contains("g_face");
    }

    @Test
    void deleteInvalidatesOnlyAssetsOwnedByApplication() throws IOException {
        when(uploader.destroy(eq("holiday-planner/destinations/managed"), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        service.delete("holiday-planner/destinations/managed");

        verify(uploader).destroy(eq("holiday-planner/destinations/managed"), anyMap());
        assertThatThrownBy(() -> service.delete("another-app/image"))
                .isInstanceOf(ImageStorageException.class)
                .hasMessageContaining("ngoài thư mục");
        verify(uploader, never()).destroy(eq("another-app/image"), anyMap());
    }

    @Test
    void malformedOrFailedCloudinaryResponseBecomesSafeStorageError() throws IOException {
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("public_id", "missing-url"));

        assertThatThrownBy(() -> service.upload(jpeg("avatar.jpg"), ImageUploadKind.AVATAR))
                .isInstanceOf(ImageStorageException.class)
                .hasMessageContaining("Ảnh hiện tại vẫn được giữ nguyên");
    }

    private MockMultipartFile jpeg(String filename) {
        return new MockMultipartFile("image", filename, "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
    }
}
