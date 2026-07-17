package vn.edu.ptit.holidayplanner.media;

import org.junit.jupiter.api.Test;
import vn.edu.ptit.holidayplanner.config.CloudinaryProperties;
import vn.edu.ptit.holidayplanner.config.MediaStorageConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class MediaStorageConfigurationTest {
    private final MediaStorageConfiguration configuration = new MediaStorageConfiguration();

    @Test
    void usesUnavailableFallbackWhenCloudinaryIsNotConfigured() {
        CloudinaryProperties properties = new CloudinaryProperties();

        ImageStorageService service = configuration.imageStorageService(properties);

        assertThat(service).isInstanceOf(UnavailableImageStorageService.class);
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void createsCloudinaryStorageOnlyWhenAllValuesArePresent() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("demo-cloud");
        properties.setApiKey("123456");
        properties.setApiSecret("test-only-secret");
        properties.setFolder("holiday-planner-test");

        ImageStorageService service = configuration.imageStorageService(properties);

        assertThat(service).isInstanceOf(CloudinaryImageStorageService.class);
        assertThat(service.isAvailable()).isTrue();
    }
}
