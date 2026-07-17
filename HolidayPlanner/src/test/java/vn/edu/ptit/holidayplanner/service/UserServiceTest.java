package vn.edu.ptit.holidayplanner.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.dto.RegisterRequest;
import vn.edu.ptit.holidayplanner.dto.ChangePasswordRequest;
import vn.edu.ptit.holidayplanner.dto.ProfileRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("  Nguyễn Văn Test  ");
        request.setEmail("  UNIT.TEST@Example.COM ");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        UserAccount user = userService.register(request);

        assertThat(user.getFullName()).isEqualTo("Nguyễn Văn Test");
        assertThat(user.getEmail()).isEqualTo("unit.test@example.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        RegisterRequest first = new RegisterRequest();
        first.setFullName("Test One");
        first.setEmail("duplicate@example.com");
        first.setPassword("password123");
        first.setConfirmPassword("password123");
        userService.register(first);

        RegisterRequest second = new RegisterRequest();
        second.setFullName("Test Two");
        second.setEmail("DUPLICATE@example.com");
        second.setPassword("password456");
        second.setConfirmPassword("password456");

        assertThatThrownBy(() -> userService.register(second))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email đã tồn tại");
    }

    @Test
    void serviceLayerRejectsInvalidRegistrationEvenWithoutControllerValidation() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("***");
        request.setEmail("valid@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chữ cái");

        request.setFullName("Người hợp lệ");
        request.setEmail("invalid-email");
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");

        request.setEmail("valid@example.com");
        request.setPassword("onlyletters");
        request.setConfirmPassword("onlyletters");
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chữ và số");

        request.setPassword("password123");
        request.setConfirmPassword("different123");
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không khớp");
    }

    @Test
    void profileAndPasswordCanBeChangedOnlyWithCurrentPassword() {
        ProfileRequest profile = new ProfileRequest();
        profile.setFullName("  Người dùng cập nhật  ");
        UserAccount updated = userService.updateProfile("user@holidayplanner.vn", profile);
        assertThat(updated.getFullName()).isEqualTo("Người dùng cập nhật");

        ChangePasswordRequest wrong = new ChangePasswordRequest();
        wrong.setCurrentPassword("wrong-password");
        wrong.setNewPassword("newPassword123");
        wrong.setConfirmPassword("newPassword123");
        assertThatThrownBy(() -> userService.changePassword("user@holidayplanner.vn", wrong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hiện tại");

        ChangePasswordRequest valid = new ChangePasswordRequest();
        valid.setCurrentPassword("user1234");
        valid.setNewPassword("newPassword123");
        valid.setConfirmPassword("newPassword123");
        userService.changePassword("user@holidayplanner.vn", valid);
        assertThat(passwordEncoder.matches("newPassword123",
                userService.requireByEmail("user@holidayplanner.vn").getPassword())).isTrue();
    }

    @Test
    void adminCannotLockCurrentAccount() {
        UserAccount admin = userService.requireByEmail("admin@holidayplanner.vn");
        assertThatThrownBy(() -> userService.setStatus(admin.getId(), UserStatus.LOCKED, admin.getEmail()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tự khóa");
    }

    @Test
    void lastActiveAdminCannotBeLocked() {
        UserAccount originalAdmin = userService.requireByEmail("admin@holidayplanner.vn");
        UserAccount secondAdmin = userService.createSeedUser(
                "Second Administrator", "second.admin@example.com", "second123", Role.ADMIN);

        UserAccount lockedOriginal = userService.setStatus(
                originalAdmin.getId(), UserStatus.LOCKED, secondAdmin.getEmail());
        assertThat(lockedOriginal.getStatus()).isEqualTo(UserStatus.LOCKED);

        assertThatThrownBy(() -> userService.setStatus(
                secondAdmin.getId(), UserStatus.LOCKED, originalAdmin.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(secondAdmin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
