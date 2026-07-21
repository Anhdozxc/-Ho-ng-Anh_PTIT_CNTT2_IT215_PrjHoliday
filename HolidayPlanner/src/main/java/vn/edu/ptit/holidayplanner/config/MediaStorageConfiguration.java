package vn.edu.ptit.holidayplanner.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import vn.edu.ptit.holidayplanner.media.CloudinaryImageStorageService;
import vn.edu.ptit.holidayplanner.media.ImageFileValidator;
import vn.edu.ptit.holidayplanner.media.ImageReplacementCoordinator;
import vn.edu.ptit.holidayplanner.media.ImageStorageService;
import vn.edu.ptit.holidayplanner.media.UnavailableImageStorageService;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class MediaStorageConfiguration {
    @Bean
    public ImageStorageService imageStorageService(CloudinaryProperties properties, Environment env) {
        String cloudinaryUrl = env.getProperty("CLOUDINARY_URL");
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
            return new CloudinaryImageStorageService(cloudinary, properties.getFolder());
        }

        if (!properties.isConfigured()) {
            return new UnavailableImageStorageService();
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true));
        return new CloudinaryImageStorageService(cloudinary, properties.getFolder());
    }

    @Bean
    public ImageFileValidator imageFileValidator() {
        return new ImageFileValidator();
    }

    @Bean
    public ImageReplacementCoordinator imageReplacementCoordinator(ImageStorageService imageStorageService) {
        return new ImageReplacementCoordinator(imageStorageService);
    }
}
