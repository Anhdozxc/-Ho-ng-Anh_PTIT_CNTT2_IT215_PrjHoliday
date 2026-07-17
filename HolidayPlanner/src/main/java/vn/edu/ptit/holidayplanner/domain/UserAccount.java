package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email", unique = true))
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false, length = 100)
    private String password;
    @Column(name = "avatar_url", length = 700)
    private String avatarUrl;
    @Column(name = "avatar_public_id", length = 255)
    private String avatarPublicId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        fullName = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String v) {
        password = v;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String v) {
        avatarUrl = v;
    }

    public String getAvatarPublicId() {
        return avatarPublicId;
    }

    public void setAvatarPublicId(String v) {
        avatarPublicId = v;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        role = v;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
