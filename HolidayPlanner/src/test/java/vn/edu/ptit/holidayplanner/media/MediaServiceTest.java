package vn.edu.ptit.holidayplanner.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.dto.DestinationRequest;
import vn.edu.ptit.holidayplanner.repository.DestinationRepository;
import vn.edu.ptit.holidayplanner.repository.UserAccountRepository;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock private UserAccountRepository userRepository;
    @Mock private DestinationRepository destinationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ImageStorageService imageStorageService;

    private UserService userService;
    private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        ImageFileValidator validator = new ImageFileValidator();
        ImageReplacementCoordinator coordinator = new ImageReplacementCoordinator(imageStorageService);
        userService = new UserService(userRepository, passwordEncoder, imageStorageService,
                validator, coordinator);
        destinationService = new DestinationService(destinationRepository, imageStorageService,
                validator, coordinator);
    }

    @Test
    void avatarReplacementSavesNewReferenceBeforeDeletingOldImage() {
        UserAccount user = userWithAvatar("https://old.example/avatar.jpg", "holiday-planner/avatars/old");
        MockMultipartFile image = jpeg("avatar.jpg");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(imageStorageService.upload(image, ImageUploadKind.AVATAR))
                .thenReturn(new StoredImage("https://new.example/avatar.jpg", "holiday-planner/avatars/new"));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserAccount result = userService.updateAvatar("USER@example.com", image);

        assertThat(result.getAvatarUrl()).isEqualTo("https://new.example/avatar.jpg");
        assertThat(result.getAvatarPublicId()).isEqualTo("holiday-planner/avatars/new");
        InOrder order = inOrder(userRepository, imageStorageService);
        order.verify(userRepository).findByEmailIgnoreCase("user@example.com");
        order.verify(imageStorageService).upload(image, ImageUploadKind.AVATAR);
        order.verify(userRepository).saveAndFlush(user);
        order.verify(imageStorageService).delete("holiday-planner/avatars/old");
    }

    @Test
    void failedAvatarUploadPreservesExistingAvatar() {
        UserAccount user = userWithAvatar("https://old.example/avatar.jpg", "holiday-planner/avatars/old");
        MockMultipartFile image = jpeg("avatar.jpg");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(imageStorageService.upload(image, ImageUploadKind.AVATAR))
                .thenThrow(new ImageStorageException("upload failed"));

        assertThatThrownBy(() -> userService.updateAvatar("user@example.com", image))
                .isInstanceOf(ImageStorageException.class);
        assertThat(user.getAvatarUrl()).isEqualTo("https://old.example/avatar.jpg");
        assertThat(user.getAvatarPublicId()).isEqualTo("holiday-planner/avatars/old");
        verify(userRepository, never()).saveAndFlush(any());
        verify(imageStorageService, never()).delete("holiday-planner/avatars/old");
    }

    @Test
    void deletingAvatarClearsDatabaseBeforeManagedCloudImage() {
        UserAccount user = userWithAvatar("https://old.example/avatar.jpg", "holiday-planner/avatars/old");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserAccount result = userService.deleteAvatar("user@example.com");

        assertThat(result.getAvatarUrl()).isNull();
        assertThat(result.getAvatarPublicId()).isNull();
        InOrder order = inOrder(userRepository, imageStorageService);
        order.verify(userRepository).saveAndFlush(user);
        order.verify(imageStorageService).delete("holiday-planner/avatars/old");
    }

    @Test
    void invalidAvatarNeverCallsCloudStorage() {
        MockMultipartFile invalid = new MockMultipartFile(
                "image", "avatar.txt", "text/plain", "not-image".getBytes());

        assertThatThrownBy(() -> userService.updateAvatar("user@example.com", invalid))
                .isInstanceOf(ImageValidationException.class);
        verifyNoInteractions(imageStorageService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void destinationUploadTakesPrecedenceOverUrlAndReplacesManagedImageSafely() {
        Destination destination = destinationWithImage(
                "https://old.example/destination.jpg", "holiday-planner/destinations/old");
        DestinationRequest request = request("https://fallback.example/destination.jpg");
        MockMultipartFile image = jpeg("destination.jpeg");
        when(destinationRepository.findById(10L)).thenReturn(Optional.of(destination));
        when(imageStorageService.upload(image, ImageUploadKind.DESTINATION))
                .thenReturn(new StoredImage("https://new.example/destination.jpg",
                        "holiday-planner/destinations/new"));
        when(destinationRepository.saveAndFlush(destination)).thenReturn(destination);

        Destination result = destinationService.update(10L, request, image);

        assertThat(result.getImageUrl()).isEqualTo("https://new.example/destination.jpg");
        assertThat(result.getImagePublicId()).isEqualTo("holiday-planner/destinations/new");
        InOrder order = inOrder(destinationRepository, imageStorageService);
        order.verify(destinationRepository).findById(10L);
        order.verify(imageStorageService).upload(image, ImageUploadKind.DESTINATION);
        order.verify(destinationRepository).saveAndFlush(destination);
        order.verify(imageStorageService).delete("holiday-planner/destinations/old");
    }

    @Test
    void destinationUsesUrlThenLocalDefaultWhenNoFileIsUploaded() {
        DestinationRequest withUrl = request("  https://images.example/place.jpg  ");
        when(destinationRepository.saveAndFlush(any(Destination.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Destination urlDestination = destinationService.create(withUrl, null);
        Destination defaultDestination = destinationService.create(request("  "), null);

        assertThat(urlDestination.getImageUrl()).isEqualTo("https://images.example/place.jpg");
        assertThat(urlDestination.getImagePublicId()).isNull();
        assertThat(defaultDestination.getImageUrl()).isEqualTo(Destination.DEFAULT_IMAGE_URL);
        assertThat(defaultDestination.getImagePublicId()).isNull();
        verify(imageStorageService, never()).upload(any(), any());
    }

    @Test
    void destinationServiceRejectsInvalidMetadataBeforePersistenceOrUpload() {
        DestinationRequest missingCountry = request("https://images.example/place.jpg");
        missingCountry.setCountry("   ");

        assertThatThrownBy(() -> destinationService.create(missingCountry, jpeg("place.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quốc gia");

        verifyNoInteractions(destinationRepository);
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void destinationServiceRejectsUnsafeImageUrl() {
        DestinationRequest unsafeUrl = request("javascript:evil()");

        assertThatThrownBy(() -> destinationService.create(unsafeUrl, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP(S)");

        verifyNoInteractions(destinationRepository);
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void databaseFailureRestoresOldDestinationAndCleansUpNewUpload() {
        Destination destination = destinationWithImage(
                "https://old.example/destination.jpg", "holiday-planner/destinations/old");
        MockMultipartFile image = jpeg("destination.jpg");
        when(destinationRepository.findById(11L)).thenReturn(Optional.of(destination));
        when(imageStorageService.upload(image, ImageUploadKind.DESTINATION))
                .thenReturn(new StoredImage("https://new.example/destination.jpg",
                        "holiday-planner/destinations/new"));
        when(destinationRepository.saveAndFlush(destination)).thenThrow(new IllegalStateException("db failed"));

        assertThatThrownBy(() -> destinationService.update(11L, request(null), image))
                .isInstanceOf(IllegalStateException.class);
        assertThat(destination.getImageUrl()).isEqualTo("https://old.example/destination.jpg");
        assertThat(destination.getImagePublicId()).isEqualTo("holiday-planner/destinations/old");
        verify(imageStorageService).delete("holiday-planner/destinations/new");
        verify(imageStorageService, never()).delete("holiday-planner/destinations/old");
    }

    @Test
    void settingDestinationVisibilityIsIdempotent() {
        Destination destination = destinationWithImage(Destination.DEFAULT_IMAGE_URL, null);
        destination.setActive(true);
        when(destinationRepository.findById(12L)).thenReturn(Optional.of(destination));

        Destination unchanged = destinationService.setActive(12L, true);

        assertThat(unchanged.isActive()).isTrue();
        verify(destinationRepository, never()).save(any());

        when(destinationRepository.save(destination)).thenReturn(destination);
        Destination hidden = destinationService.setActive(12L, false);
        assertThat(hidden.isActive()).isFalse();
        verify(destinationRepository).save(destination);
    }

    private UserAccount userWithAvatar(String url, String publicId) {
        UserAccount user = new UserAccount();
        user.setEmail("user@example.com");
        user.setAvatarUrl(url);
        user.setAvatarPublicId(publicId);
        return user;
    }

    private Destination destinationWithImage(String url, String publicId) {
        Destination destination = new Destination();
        destination.setName("Hà Nội");
        destination.setCountry("Việt Nam");
        destination.setImageUrl(url);
        destination.setImagePublicId(publicId);
        return destination;
    }

    private DestinationRequest request(String imageUrl) {
        DestinationRequest request = new DestinationRequest();
        request.setName("Hà Nội");
        request.setCity("Hà Nội");
        request.setCountry("Việt Nam");
        request.setDescription("Điểm đến thử nghiệm");
        request.setImageUrl(imageUrl);
        request.setActive(true);
        return request;
    }

    private MockMultipartFile jpeg(String filename) {
        return new MockMultipartFile("image", filename, "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
    }
}
