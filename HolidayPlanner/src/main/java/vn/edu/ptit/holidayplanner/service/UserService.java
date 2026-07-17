package vn.edu.ptit.holidayplanner.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.dto.RegisterRequest;
import vn.edu.ptit.holidayplanner.dto.ChangePasswordRequest;
import vn.edu.ptit.holidayplanner.dto.ProfileRequest;
import vn.edu.ptit.holidayplanner.repository.UserAccountRepository;
import vn.edu.ptit.holidayplanner.media.ImageFileValidator;
import vn.edu.ptit.holidayplanner.media.ImageReplacementCoordinator;
import vn.edu.ptit.holidayplanner.media.ImageStorageService;
import vn.edu.ptit.holidayplanner.media.ImageUploadKind;
import vn.edu.ptit.holidayplanner.media.StoredImage;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*", Pattern.DOTALL);
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*", Pattern.DOTALL);

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;
    private final ImageFileValidator imageFileValidator;
    private final ImageReplacementCoordinator imageReplacementCoordinator;

    public UserService(UserAccountRepository repository, PasswordEncoder passwordEncoder,
                       ImageStorageService imageStorageService,
                       ImageFileValidator imageFileValidator,
                       ImageReplacementCoordinator imageReplacementCoordinator) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.imageStorageService = imageStorageService;
        this.imageFileValidator = imageFileValidator;
        this.imageReplacementCoordinator = imageReplacementCoordinator;
    }

    public UserAccount register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đăng ký không được để trống");
        }
        String fullName = validateFullName(request.getFullName());
        String email = normalize(request.getEmail());
        validateEmail(email);
        validatePassword(request.getPassword(), request.getConfirmPassword(), "Mật khẩu");
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email đã tồn tại");
        }
        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return repository.save(user);
    }

    @Transactional(readOnly = true)
    public UserAccount requireByEmail(String email) {
        return repository.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản"));
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return repository.findAll().stream()
                .sorted((a,b) -> a.getEmail().compareToIgnoreCase(b.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> search(String query, Role role, UserStatus status, int page, int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return repository.search(normalizedQuery, role, status,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "email")));
    }

    public UserAccount updateProfile(String email, ProfileRequest request) {
        UserAccount user = requireByEmail(email);
        if (request == null) {
            throw new IllegalArgumentException("Thông tin hồ sơ không được để trống");
        }
        user.setFullName(validateFullName(request.getFullName()));
        return repository.save(user);
    }

    public UserAccount updateAvatar(String email, MultipartFile image) {
        imageFileValidator.validate(image);
        UserAccount user = requireByEmail(email);
        String previousUrl = user.getAvatarUrl();
        String previousPublicId = user.getAvatarPublicId();
        StoredImage uploaded = imageStorageService.upload(image, ImageUploadKind.AVATAR);
        user.setAvatarUrl(uploaded.secureUrl());
        user.setAvatarPublicId(uploaded.publicId());
        try {
            UserAccount saved = repository.saveAndFlush(user);
            imageReplacementCoordinator.trackReplacement(previousPublicId, uploaded.publicId());
            return saved;
        } catch (RuntimeException exception) {
            user.setAvatarUrl(previousUrl);
            user.setAvatarPublicId(previousPublicId);
            imageReplacementCoordinator.cleanupFailedUpload(uploaded.publicId());
            throw exception;
        }
    }

    public UserAccount deleteAvatar(String email) {
        UserAccount user = requireByEmail(email);
        String previousUrl = user.getAvatarUrl();
        String previousPublicId = user.getAvatarPublicId();
        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        try {
            UserAccount saved = repository.saveAndFlush(user);
            imageReplacementCoordinator.trackReplacement(previousPublicId, null);
            return saved;
        } catch (RuntimeException exception) {
            user.setAvatarUrl(previousUrl);
            user.setAvatarPublicId(previousPublicId);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public boolean isImageUploadAvailable() {
        return imageStorageService.isAvailable();
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không được để trống");
        }
        validatePassword(request.getNewPassword(), request.getConfirmPassword(), "Mật khẩu mới");
        UserAccount user = requireByEmail(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    public UserAccount setStatus(Long userId, UserStatus desiredStatus, String currentEmail) {
        if (desiredStatus == null) {
            throw new IllegalArgumentException("Trạng thái tài khoản không được để trống");
        }
        UserAccount target = repository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản"));
        if (target.getEmail().equalsIgnoreCase(normalize(currentEmail))
                && desiredStatus == UserStatus.LOCKED) {
            throw new AccessDeniedException("Không thể tự khóa tài khoản đang đăng nhập");
        }
        if (target.getStatus() == desiredStatus) {
            return target;
        }
        if (desiredStatus == UserStatus.LOCKED
                && target.getRole() == Role.ADMIN
                && target.getStatus() == UserStatus.ACTIVE
                && repository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new AccessDeniedException("Không thể khóa quản trị viên đang hoạt động cuối cùng");
        }
        target.setStatus(desiredStatus);
        return repository.save(target);
    }

    public UserAccount createSeedUser(String fullName, String email, String rawPassword, Role role) {
        return repository.findByEmailIgnoreCase(email).orElseGet(() -> {
            UserAccount user = new UserAccount();
            user.setFullName(fullName);
            user.setEmail(normalize(email));
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            return repository.save(user);
        });
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String validateFullName(String value) {
        String fullName = value == null ? "" : value.trim();
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new IllegalArgumentException("Họ tên phải từ 2 đến 100 ký tự");
        }
        if (!HAS_LETTER.matcher(fullName).matches()) {
            throw new IllegalArgumentException("Họ tên phải chứa ít nhất một chữ cái");
        }
        return fullName;
    }

    private void validateEmail(String email) {
        if (email.length() > 150 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ hoặc vượt quá 150 ký tự");
        }
    }

    private void validatePassword(String password, String confirmation, String label) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException(label + " phải từ 8 đến 72 ký tự");
        }
        if (!HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()) {
            throw new IllegalArgumentException(label + " phải có cả chữ và số");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Xác nhận " + label.toLowerCase(Locale.ROOT) + " không khớp");
        }
    }
}
