package vn.edu.ptit.holidayplanner.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.dto.DestinationRequest;
import vn.edu.ptit.holidayplanner.media.ImageFileValidator;
import vn.edu.ptit.holidayplanner.media.ImageReplacementCoordinator;
import vn.edu.ptit.holidayplanner.media.ImageStorageService;
import vn.edu.ptit.holidayplanner.media.ImageUploadKind;
import vn.edu.ptit.holidayplanner.media.StoredImage;
import vn.edu.ptit.holidayplanner.repository.DestinationRepository;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@Transactional
public class DestinationService {
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "^(https?://[^\\s\"'<>\\\\]+|/[^\\s\"'<>\\\\]*)?$", Pattern.CASE_INSENSITIVE);

    private final DestinationRepository repository;
    private final ImageStorageService imageStorageService;
    private final ImageFileValidator imageFileValidator;
    private final ImageReplacementCoordinator imageReplacementCoordinator;

    public DestinationService(DestinationRepository repository,
                              ImageStorageService imageStorageService,
                              ImageFileValidator imageFileValidator,
                              ImageReplacementCoordinator imageReplacementCoordinator) {
        this.repository = repository;
        this.imageStorageService = imageStorageService;
        this.imageFileValidator = imageFileValidator;
        this.imageReplacementCoordinator = imageReplacementCoordinator;
    }

    @Transactional(readOnly = true)
    public List<Destination> activeDestinations() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Destination> allDestinations() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Page<Destination> search(String query, Boolean active, int page, int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return repository.search(normalizedQuery, active,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Transactional(readOnly = true)
    public Destination require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy điểm đến"));
    }

    public Destination create(DestinationRequest request) {
        return create(request, null);
    }

    public Destination create(DestinationRequest request, MultipartFile imageFile) {
        validateRequest(request);
        Destination destination = applyMetadata(new Destination(), request);
        StoredImage uploaded = uploadIfPresent(imageFile);
        if (uploaded != null) {
            destination.setImageUrl(uploaded.secureUrl());
            destination.setImagePublicId(uploaded.publicId());
        } else {
            destination.setImageUrl(requestedImageUrl(request));
            destination.setImagePublicId(null);
        }
        try {
            Destination saved = repository.saveAndFlush(destination);
            imageReplacementCoordinator.trackReplacement(null,
                    uploaded == null ? null : uploaded.publicId());
            return saved;
        } catch (RuntimeException exception) {
            if (uploaded != null) {
                imageReplacementCoordinator.cleanupFailedUpload(uploaded.publicId());
            }
            throw exception;
        }
    }

    public Destination update(Long id, DestinationRequest request) {
        return update(id, request, null);
    }

    public Destination update(Long id, DestinationRequest request, MultipartFile imageFile) {
        validateRequest(request);
        Destination destination = require(id);
        String previousImageUrl = destination.getImageUrl();
        String previousPublicId = destination.getImagePublicId();
        applyMetadata(destination, request);
        StoredImage uploaded = uploadIfPresent(imageFile);
        if (uploaded != null) {
            destination.setImageUrl(uploaded.secureUrl());
            destination.setImagePublicId(uploaded.publicId());
        } else {
            String requestedUrl = requestedImageUrl(request);
            destination.setImageUrl(requestedUrl);
            if (!Objects.equals(previousImageUrl, requestedUrl)) {
                destination.setImagePublicId(null);
            }
        }
        try {
            Destination saved = repository.saveAndFlush(destination);
            imageReplacementCoordinator.trackReplacement(previousPublicId,
                    destination.getImagePublicId());
            return saved;
        } catch (RuntimeException exception) {
            destination.setImageUrl(previousImageUrl);
            destination.setImagePublicId(previousPublicId);
            if (uploaded != null) {
                imageReplacementCoordinator.cleanupFailedUpload(uploaded.publicId());
            }
            throw exception;
        }
    }

    public Destination uploadImage(Long id, MultipartFile imageFile) {
        imageFileValidator.validate(imageFile);
        Destination destination = require(id);
        String previousImageUrl = destination.getImageUrl();
        String previousPublicId = destination.getImagePublicId();
        StoredImage uploaded = imageStorageService.upload(imageFile, ImageUploadKind.DESTINATION);
        destination.setImageUrl(uploaded.secureUrl());
        destination.setImagePublicId(uploaded.publicId());
        try {
            Destination saved = repository.saveAndFlush(destination);
            imageReplacementCoordinator.trackReplacement(previousPublicId, uploaded.publicId());
            return saved;
        } catch (RuntimeException exception) {
            destination.setImageUrl(previousImageUrl);
            destination.setImagePublicId(previousPublicId);
            imageReplacementCoordinator.cleanupFailedUpload(uploaded.publicId());
            throw exception;
        }
    }

    public Destination deleteImage(Long id) {
        Destination destination = require(id);
        String previousImageUrl = destination.getImageUrl();
        String previousPublicId = destination.getImagePublicId();
        destination.setImageUrl(Destination.DEFAULT_IMAGE_URL);
        destination.setImagePublicId(null);
        try {
            Destination saved = repository.saveAndFlush(destination);
            imageReplacementCoordinator.trackReplacement(previousPublicId, null);
            return saved;
        } catch (RuntimeException exception) {
            destination.setImageUrl(previousImageUrl);
            destination.setImagePublicId(previousPublicId);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public boolean isImageUploadAvailable() {
        return imageStorageService.isAvailable();
    }

    public void deactivate(Long id) {
        setActive(id, false);
    }

    public Destination setActive(Long id, boolean active) {
        Destination destination = require(id);
        if (destination.isActive() == active) {
            return destination;
        }
        destination.setActive(active);
        return repository.save(destination);
    }

    public Destination createSeed(String name, String city, String country, String description, String imageUrl) {
        Destination destination = repository.findAllByOrderByNameAsc().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Destination d = new Destination();
                    d.setName(name);
                    d.setCity(city);
                    d.setCountry(country);
                    d.setDescription(description);
                    d.setImageUrl(imageUrl);
                    d.setActive(true);
                    return repository.save(d);
                });
        String currentImageUrl = destination.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()
                && (currentImageUrl == null || currentImageUrl.isBlank()
                || currentImageUrl.startsWith("https://images.unsplash.com/"))) {
            destination.setImageUrl(imageUrl);
            destination.setImagePublicId(null);
            return repository.save(destination);
        }
        return destination;
    }

    private Destination applyMetadata(Destination destination, DestinationRequest request) {
        destination.setName(request.getName().trim());
        destination.setCity(trimToNull(request.getCity()));
        destination.setCountry(request.getCountry().trim());
        destination.setDescription(trimToNull(request.getDescription()));
        destination.setActive(request.isActive());
        return destination;
    }

    private void validateRequest(DestinationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu điểm đến là bắt buộc");
        }
        validateRequiredText(request.getName(), 140, "Tên điểm đến");
        validateOptionalText(request.getCity(), 120, "Thành phố");
        validateRequiredText(request.getCountry(), 120, "Quốc gia");
        validateOptionalText(request.getDescription(), 1500, "Mô tả");
        validateOptionalText(request.getImageUrl(), 700, "URL ảnh");
        String imageUrl = trimToNull(request.getImageUrl());
        if (imageUrl != null && !IMAGE_URL_PATTERN.matcher(imageUrl).matches()) {
            throw new IllegalArgumentException(
                    "Ảnh phải là URL HTTP(S) hoặc đường dẫn local bắt đầu bằng /");
        }
    }

    private void validateRequiredText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " tối đa " + maxLength + " ký tự");
        }
    }

    private void validateOptionalText(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " tối đa " + maxLength + " ký tự");
        }
    }

    private StoredImage uploadIfPresent(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        imageFileValidator.validate(imageFile);
        return imageStorageService.upload(imageFile, ImageUploadKind.DESTINATION);
    }

    private String requestedImageUrl(DestinationRequest request) {
        String requested = trimToNull(request.getImageUrl());
        return requested == null ? Destination.DEFAULT_IMAGE_URL : requested;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
