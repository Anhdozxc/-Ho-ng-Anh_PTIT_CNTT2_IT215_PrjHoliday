package vn.edu.ptit.holidayplanner.dto;

import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;

public record ProfileResponse(Long id, String fullName, String email, String avatarUrl,
                              Role role, UserStatus status) {
    public static ProfileResponse from(UserAccount user) {
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getAvatarUrl(), user.getRole(), user.getStatus());
    }
}
