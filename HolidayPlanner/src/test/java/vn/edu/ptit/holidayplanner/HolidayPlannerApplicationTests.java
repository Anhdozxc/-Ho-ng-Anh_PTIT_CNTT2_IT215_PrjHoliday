package vn.edu.ptit.holidayplanner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.ptit.holidayplanner.media.ImageStorageService;
import vn.edu.ptit.holidayplanner.media.UnavailableImageStorageService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HolidayPlannerApplicationTests {
    @Autowired private ImageStorageService imageStorageService;

    @Test
    void contextLoadsWithoutCloudinaryConfiguration() {
        assertThat(imageStorageService).isInstanceOf(UnavailableImageStorageService.class);
        assertThat(imageStorageService.isAvailable()).isFalse();
    }
}
