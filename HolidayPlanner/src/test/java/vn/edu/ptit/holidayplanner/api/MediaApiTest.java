package vn.edu.ptit.holidayplanner.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.media.ImageStorageService;
import vn.edu.ptit.holidayplanner.media.ImageStorageUnavailableException;
import vn.edu.ptit.holidayplanner.media.ImageUploadKind;
import vn.edu.ptit.holidayplanner.media.StoredImage;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MediaApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private DestinationService destinationService;
    @MockitoBean private ImageStorageService imageStorageService;

    @BeforeEach
    void setUpStorage() {
        when(imageStorageService.isAvailable()).thenReturn(true);
        when(imageStorageService.upload(any(), eq(ImageUploadKind.AVATAR)))
                .thenReturn(new StoredImage("https://media.example/avatar.jpg",
                        "holiday-planner/avatars/api-test"));
        when(imageStorageService.upload(any(), eq(ImageUploadKind.DESTINATION)))
                .thenReturn(new StoredImage("https://media.example/destination.jpg",
                        "holiday-planner/destinations/api-test"));
    }

    @Test
    void profileApiNeverExposesPasswordOrCloudPublicId() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@holidayplanner.vn"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.avatarPublicId").doesNotExist());
    }

    @Test
    void userCanUploadAndDeleteOnlyOwnAvatar() throws Exception {
        var travelerBefore = userService.requireByEmail("traveler@holidayplanner.vn");
        assertThat(travelerBefore.getAvatarUrl()).isNull();

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(jpeg("image", "avatar.jpg"))
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://media.example/avatar.jpg"))
                .andExpect(jsonPath("$.avatarPublicId").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userService.requireByEmail("traveler@holidayplanner.vn").getAvatarUrl()).isNull();

        mockMvc.perform(delete("/api/profile/avatar")
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
    }

    @Test
    void invalidAvatarIsRejectedBeforeStorage() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile(
                "image", "avatar.txt", "text/plain", "not-image".getBytes());

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(invalid)
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(multipart("/api/profile/avatar")
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]))
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unavailableCloudStorageReturnsFriendly503AndKeepsAvatar() throws Exception {
        var user = userService.requireByEmail("user@holidayplanner.vn");
        String previousUrl = user.getAvatarUrl();
        when(imageStorageService.upload(any(), eq(ImageUploadKind.AVATAR)))
                .thenThrow(new ImageStorageUnavailableException());

        mockMvc.perform(multipart("/api/profile/avatar")
                        .file(jpeg("image", "avatar.jpg"))
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        assertThat(userService.requireByEmail("user@holidayplanner.vn").getAvatarUrl())
                .isEqualTo(previousUrl);
    }

    @Test
    void destinationUploadRequiresAdminAndDoesNotExposePublicId() throws Exception {
        Long destinationId = destinationService.activeDestinations().get(0).getId();

        mockMvc.perform(multipart("/api/admin/destinations/{id}/image", destinationId)
                        .file(jpeg("image", "destination.jpg"))
                        .with(httpBasic("user@holidayplanner.vn", "user1234")))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/admin/destinations/{id}/image", destinationId)
                        .file(jpeg("image", "destination.jpg"))
                        .with(httpBasic("admin@holidayplanner.vn", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://media.example/destination.jpg"))
                .andExpect(jsonPath("$.imagePublicId").doesNotExist());
    }

    @Test
    void adminDestinationSearchReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/destinations")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "2")
                        .with(httpBasic("admin@holidayplanner.vn", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void desiredUserStatusIsIdempotentAndAdminCannotLockSelf() throws Exception {
        Long travelerId = userService.requireByEmail("traveler@holidayplanner.vn").getId();
        String locked = "{\"status\":\"LOCKED\"}";

        mockMvc.perform(patch("/api/admin/users/{id}/toggle", travelerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locked)
                        .with(httpBasic("admin@holidayplanner.vn", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
        mockMvc.perform(patch("/api/admin/users/{id}/toggle", travelerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locked)
                        .with(httpBasic("admin@holidayplanner.vn", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        Long adminId = userService.requireByEmail("admin@holidayplanner.vn").getId();
        mockMvc.perform(patch("/api/admin/users/{id}/toggle", adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locked)
                        .with(httpBasic("admin@holidayplanner.vn", "admin123")))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile jpeg(String field, String filename) {
        return new MockMultipartFile(field, filename, "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
    }
}
