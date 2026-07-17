package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.ptit.holidayplanner.validation.FieldMatch;

import java.util.Locale;

@FieldMatch(first = "password", second = "confirmPassword", message = "Xác nhận mật khẩu không khớp")
public class RegisterRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    @Pattern(regexp = ".*[\\p{L}\\p{N}].*", message = "Họ tên phải chứa chữ hoặc số")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "Mật khẩu phải có cả chữ và số")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName == null ? null : fullName.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
