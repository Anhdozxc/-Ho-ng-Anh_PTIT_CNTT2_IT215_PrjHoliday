package vn.edu.ptit.holidayplanner.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.dto.TripPlanRequest;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class TripPlanService {
    private final TripPlanRepository repository;
    private final UserService userService;
    private final DestinationService destinationService;

    public TripPlanService(TripPlanRepository repository, UserService userService, DestinationService destinationService) {
        this.repository = repository;
        this.userService = userService;
        this.destinationService = destinationService;
    }

    @Transactional(readOnly = true)
    public List<TripPlan> listOwned(String email) {
        return repository.findAllByOwnerAndDeletedFalseOrderByStartDateDesc(userService.requireByEmail(email));
    }

    @Transactional(readOnly = true)
    public List<TripPlan> listVisible(String email, String query, TripStatus status,
                                      String sortBy, String direction) {
        UserAccount user = userService.requireByEmail(email);
        Pageable pageable = Pageable.unpaged(safeSort(sortBy, direction));
        return searchVisible(user, normalizeQuery(query), status, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public Page<TripPlan> searchVisible(String email, String query, TripStatus status,
                                        String sortBy, String direction, int page, int size) {
        UserAccount user = userService.requireByEmail(email);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, safeSort(sortBy, direction));
        return searchVisible(user, normalizeQuery(query), status, pageable);
    }

    @Transactional(readOnly = true)
    public TripPlan requireOwned(Long id, String email) {
        UserAccount owner = userService.requireByEmail(email);
        TripPlan trip = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy kế hoạch"));
        if (!trip.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa kế hoạch này");
        }
        return trip;
    }

    @Transactional(readOnly = true)
    public TripPlan requireViewable(Long id, String email) {
        UserAccount user = userService.requireByEmail(email);
        TripPlan trip = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy kế hoạch"));
        if (!trip.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xem kế hoạch này");
        }
        return trip;
    }

    public TripPlan create(TripPlanRequest request, String email) {
        validate(request);
        TripPlan trip = new TripPlan();
        trip.setOwner(userService.requireByEmail(email));
        validateStatusTransition(trip.getStatus(), request.getStatus());
        apply(trip, request);
        return repository.save(trip);
    }

    public TripPlan update(Long id, TripPlanRequest request, String email) {
        TripPlan trip = requireOwned(id, email);
        validate(request);
        validateStatusTransition(trip.getStatus(), request.getStatus());
        apply(trip, request);
        return repository.save(trip);
    }

    public void softDelete(Long id, String email) {
        TripPlan trip = requireOwned(id, email);
        trip.setDeleted(true);
        repository.save(trip);
    }

    public TripPlanRequest toRequest(TripPlan trip) {
        TripPlanRequest request = new TripPlanRequest();
        request.setTitle(trip.getTitle());
        request.setDestinationId(trip.getDestination().getId());
        request.setStartDate(trip.getStartDate());
        request.setEndDate(trip.getEndDate());
        request.setPeopleCount(trip.getPeopleCount());
        request.setBudget(trip.getBudget());
        request.setStatus(trip.getStatus());
        request.setNotes(trip.getNotes());
        return request;
    }

    private void apply(TripPlan trip, TripPlanRequest request) {
        Destination destination = destinationService.require(request.getDestinationId());
        if (!destination.isActive() && (trip.getDestination() == null || !destination.getId().equals(trip.getDestination().getId()))) {
            throw new IllegalArgumentException("Điểm đến đã bị ẩn");
        }
        trip.setTitle(request.getTitle().trim());
        trip.setDestination(destination);
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setPeopleCount(request.getPeopleCount());
        BigDecimal budget = request.getBudget() == null ? BigDecimal.ZERO : request.getBudget();
        trip.setBudget(budget.setScale(2, RoundingMode.UNNECESSARY));
        trip.setStatus(request.getStatus());
        trip.setNotes(request.getNotes() == null || request.getNotes().isBlank() ? null : request.getNotes().trim());
    }

    private void validate(TripPlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu kế hoạch là bắt buộc");
        }
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên chuyến đi không được để trống");
        }
        if (title.trim().length() > 180) {
            throw new IllegalArgumentException("Tên chuyến đi tối đa 180 ký tự");
        }
        if (request.getDestinationId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn điểm đến");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu");
        }
        if (request.getPeopleCount() == null || request.getPeopleCount() < 1 || request.getPeopleCount() > 100) {
            throw new IllegalArgumentException("Số người phải từ 1 đến 100");
        }
        if (request.getBudget() != null) {
            validateMoney(request.getBudget(), "Ngân sách");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái là bắt buộc");
        }
        if (request.getNotes() != null && request.getNotes().trim().length() > 2000) {
            throw new IllegalArgumentException("Ghi chú tối đa 2000 ký tự");
        }
    }

    private void validateStatusTransition(TripStatus current, TripStatus requested) {
        if (current == requested) {
            return;
        }
        boolean allowed = switch (current) {
            case DRAFT -> requested == TripStatus.PLANNED || requested == TripStatus.CANCELLED;
            case PLANNED -> requested == TripStatus.ONGOING || requested == TripStatus.CANCELLED;
            case ONGOING -> requested == TripStatus.COMPLETED || requested == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new ConflictException(
                    "Không thể chuyển trạng thái chuyến đi từ " + current + " sang " + requested);
        }
    }

    private void validateMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " là bắt buộc");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " không được âm");
        }
        BigDecimal normalized = value.stripTrailingZeros();
        int fractionDigits = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        if (integerDigits > 13 || fractionDigits > 2) {
            throw new IllegalArgumentException(fieldName + " tối đa 13 chữ số nguyên và 2 chữ số thập phân");
        }
    }

    private Page<TripPlan> searchVisible(UserAccount user, String query, TripStatus status, Pageable pageable) {
        return user.getRole() == Role.ADMIN
                ? repository.searchAll(query, status, pageable)
                : repository.searchOwned(user, query, status, pageable);
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private Sort safeSort(String sortBy, String direction) {
        String property = switch (sortBy == null ? "startDate" : sortBy) {
            case "title" -> "title";
            case "createdAt" -> "createdAt";
            default -> "startDate";
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
