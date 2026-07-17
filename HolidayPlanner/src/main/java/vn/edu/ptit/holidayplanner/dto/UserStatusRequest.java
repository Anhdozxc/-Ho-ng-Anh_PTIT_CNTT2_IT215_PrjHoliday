package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;

public class UserStatusRequest {
    @NotNull(message = "Trạng thái tài khoản không được để trống")
    private UserStatus status;

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
