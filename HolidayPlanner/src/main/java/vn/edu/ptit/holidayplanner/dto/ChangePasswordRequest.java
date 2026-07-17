package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.ptit.holidayplanner.validation.FieldMatch;

@FieldMatch(first = "newPassword", second = "confirmPassword", message = "Xác nhận mật khẩu mới không khớp")
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu mới phải từ 8 đến 72 ký tự")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "Mật khẩu mới phải có cả chữ và số")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
