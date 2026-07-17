package vn.edu.ptit.holidayplanner.config;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.BookingNote;
import vn.edu.ptit.holidayplanner.domain.ChecklistItem;
import vn.edu.ptit.holidayplanner.domain.Destination;
import vn.edu.ptit.holidayplanner.domain.Expense;
import vn.edu.ptit.holidayplanner.domain.ItineraryItem;
import vn.edu.ptit.holidayplanner.domain.TripPlan;
import vn.edu.ptit.holidayplanner.domain.UserAccount;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import vn.edu.ptit.holidayplanner.domain.enums.Role;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import vn.edu.ptit.holidayplanner.repository.BookingNoteRepository;
import vn.edu.ptit.holidayplanner.repository.ChecklistItemRepository;
import vn.edu.ptit.holidayplanner.repository.ExpenseRepository;
import vn.edu.ptit.holidayplanner.repository.ItineraryItemRepository;
import vn.edu.ptit.holidayplanner.repository.TripPlanRepository;
import vn.edu.ptit.holidayplanner.service.DestinationService;
import vn.edu.ptit.holidayplanner.service.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class DemoDataService {
    static final String ADMIN_EMAIL = "admin@holidayplanner.vn";
    static final String DEMO_USER_EMAIL = "user@holidayplanner.vn";
    static final String TRAVELER_EMAIL = "traveler@holidayplanner.vn";

    private final UserService userService;
    private final DestinationService destinationService;
    private final TripPlanRepository tripPlanRepository;
    private final ItineraryItemRepository itineraryRepository;
    private final ExpenseRepository expenseRepository;
    private final ChecklistItemRepository checklistRepository;
    private final BookingNoteRepository bookingRepository;
    private final Clock clock;

    public DemoDataService(
            UserService userService,
            DestinationService destinationService,
            TripPlanRepository tripPlanRepository,
            ItineraryItemRepository itineraryRepository,
            ExpenseRepository expenseRepository,
            ChecklistItemRepository checklistRepository,
            BookingNoteRepository bookingRepository,
            Clock clock) {
        this.userService = userService;
        this.destinationService = destinationService;
        this.tripPlanRepository = tripPlanRepository;
        this.itineraryRepository = itineraryRepository;
        this.expenseRepository = expenseRepository;
        this.checklistRepository = checklistRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /**
     * Creates the local demonstration dataset and safely fills any missing pieces.
     * Stable natural keys make repeated application starts idempotent.
     */
    public synchronized void seed() {
        userService.createSeedUser("Quản trị Holiday Planner", ADMIN_EMAIL, "admin123", Role.ADMIN);
        UserAccount demoUser = userService.createSeedUser(
                "Người dùng Demo", DEMO_USER_EMAIL, "user1234", Role.USER);
        userService.createSeedUser("Khách du lịch", TRAVELER_EMAIL, "traveler123", Role.USER);

        Map<String, Destination> destinations = seedDestinations();
        seedTrips(demoUser, destinations, LocalDate.now(clock));
    }

    private Map<String, Destination> seedDestinations() {
        Map<String, Destination> destinations = new HashMap<>();
        for (DestinationSeed seed : destinationSeeds()) {
            Destination destination = destinationService.createSeed(
                    seed.name(), seed.city(), seed.country(), seed.description(), seed.imageUrl());
            destinations.put(seed.key(), destination);
        }
        return destinations;
    }

    private void seedTrips(UserAccount owner, Map<String, Destination> destinations, LocalDate today) {
        Map<String, TripPlan> existingByTitle = tripPlanRepository.findAll().stream()
                .filter(trip -> trip.getOwner().getId().equals(owner.getId()))
                .collect(Collectors.toMap(
                        trip -> normalized(trip.getTitle()),
                        Function.identity(),
                        (first, ignored) -> first));

        for (TripSeed seed : tripSeeds()) {
            TripPlan trip = existingByTitle.get(normalized(seed.title()));
            if (trip == null) {
                trip = createTrip(seed, owner, destinations.get(seed.destinationKey()), today);
                existingByTitle.put(normalized(seed.title()), trip);
            }
            seedTripDetails(trip, seed, today);
        }
    }

    private TripPlan createTrip(
            TripSeed seed, UserAccount owner, Destination destination, LocalDate today) {
        TripPlan trip = new TripPlan();
        trip.setTitle(seed.title());
        trip.setDestination(destination);
        trip.setStartDate(today.plusDays(seed.startOffsetDays()));
        trip.setEndDate(today.plusDays(seed.endOffsetDays()));
        trip.setPeopleCount(seed.peopleCount());
        trip.setBudget(money(seed.budget()));
        trip.setStatus(seed.status());
        trip.setNotes(seed.notes());
        trip.setOwner(owner);
        trip.setDeleted(false);
        return tripPlanRepository.save(trip);
    }

    private void seedTripDetails(TripPlan trip, TripSeed seed, LocalDate today) {
        seedItinerary(trip, seed.itinerary());
        seedExpenses(trip, seed.expenses(), today.plusDays(seed.startOffsetDays()));
        seedChecklist(trip, seed.checklist(), today.plusDays(seed.startOffsetDays()));
        seedBookings(trip, seed.bookings());
    }

    private void seedItinerary(TripPlan trip, List<ItinerarySeed> seeds) {
        Set<String> existingActivities = itineraryRepository
                .findByTripPlanOrderByDayNoAscFromTimeAsc(trip).stream()
                .map(ItineraryItem::getActivity)
                .map(DemoDataService::normalized)
                .collect(Collectors.toCollection(HashSet::new));
        for (ItinerarySeed seed : seeds) {
            if (existingActivities.add(normalized(seed.activity()))) {
                ItineraryItem item = new ItineraryItem();
                item.setTripPlan(trip);
                item.setDayNo(seed.dayNo());
                item.setFromTime(seed.fromTime());
                item.setToTime(seed.toTime());
                item.setActivity(seed.activity());
                item.setLocation(seed.location());
                item.setNote(seed.note());
                itineraryRepository.save(item);
            }
        }
    }

    private void seedExpenses(TripPlan trip, List<ExpenseSeed> seeds, LocalDate startDate) {
        Set<String> existingNotes = expenseRepository.findByTripPlanOrderBySpentDateDesc(trip).stream()
                .map(Expense::getNote)
                .map(DemoDataService::normalized)
                .collect(Collectors.toCollection(HashSet::new));
        for (ExpenseSeed seed : seeds) {
            if (existingNotes.add(normalized(seed.note()))) {
                Expense expense = new Expense();
                expense.setTripPlan(trip);
                expense.setCategory(seed.category());
                expense.setAmount(money(seed.amount()));
                expense.setSpentDate(startDate.plusDays(seed.dayOffset()));
                expense.setNote(seed.note());
                expenseRepository.save(expense);
            }
        }
    }

    private void seedChecklist(TripPlan trip, List<ChecklistSeed> seeds, LocalDate startDate) {
        Set<String> existingTitles = checklistRepository
                .findByTripPlanOrderByDoneAscDueDateAsc(trip).stream()
                .map(ChecklistItem::getTitle)
                .map(DemoDataService::normalized)
                .collect(Collectors.toCollection(HashSet::new));
        for (ChecklistSeed seed : seeds) {
            if (existingTitles.add(normalized(seed.title()))) {
                ChecklistItem item = new ChecklistItem();
                item.setTripPlan(trip);
                item.setTitle(seed.title());
                item.setCategory(seed.category());
                item.setDone(seed.done());
                item.setDueDate(startDate.plusDays(seed.dueDateOffset()));
                checklistRepository.save(item);
            }
        }
    }

    private void seedBookings(TripPlan trip, List<BookingSeed> seeds) {
        Set<String> existingCodes = bookingRepository.findByTripPlanOrderByIdDesc(trip).stream()
                .map(BookingNote::getBookingCode)
                .map(DemoDataService::normalized)
                .collect(Collectors.toCollection(HashSet::new));
        for (BookingSeed seed : seeds) {
            if (existingCodes.add(normalized(seed.bookingCode()))) {
                BookingNote booking = new BookingNote();
                booking.setTripPlan(trip);
                booking.setType(seed.type());
                booking.setProvider(seed.provider());
                booking.setBookingCode(seed.bookingCode());
                booking.setPrice(money(seed.price()));
                booking.setNote(seed.note());
                bookingRepository.save(booking);
            }
        }
    }

    private static List<DestinationSeed> destinationSeeds() {
        return List.of(
                new DestinationSeed(
                        "ha-noi", "Hà Nội", "Hà Nội", "Việt Nam",
                        "Thủ đô giàu bản sắc với phố cổ, hồ xanh và nền ẩm thực đường phố đặc sắc.",
                        "/images/destinations/ha-noi.svg"),
                new DestinationSeed(
                        "da-nang", "Đà Nẵng", "Đà Nẵng", "Việt Nam",
                        "Thành phố biển hiện đại, gần Hội An, bán đảo Sơn Trà và nhiều bãi tắm đẹp.",
                        "/images/destinations/da-nang.svg"),
                new DestinationSeed(
                        "da-lat", "Đà Lạt", "Đà Lạt", "Việt Nam",
                        "Thành phố cao nguyên mát mẻ với rừng thông, hồ nước và những mùa hoa rực rỡ.",
                        "/images/destinations/da-lat.svg"),
                new DestinationSeed(
                        "phu-quoc", "Phú Quốc", "Phú Quốc", "Việt Nam",
                        "Đảo nghỉ dưỡng nhiệt đới với bãi cát dài, hoàng hôn đẹp và hải sản tươi ngon.",
                        "/images/destinations/phu-quoc.svg"),
                new DestinationSeed(
                        "tokyo", "Tokyo", "Tokyo", "Nhật Bản",
                        "Đô thị sôi động kết hợp tinh tế giữa đền cổ, văn hóa địa phương và công nghệ hiện đại.",
                        "/images/destinations/tokyo.svg"));
    }

    private static List<TripSeed> tripSeeds() {
        return List.of(
                new TripSeed(
                        "Chuyến Hà Nội cuối tuần", "ha-noi", -60, -58, 2, "5000000",
                        TripStatus.COMPLETED,
                        "Cuối tuần khám phá di sản, ẩm thực và nhịp sống phố cổ Hà Nội.",
                        List.of(
                                itinerary(1, "08:00", "09:00", "Ăn sáng phở phố cổ", "Phố Bát Đàn", "Bắt đầu ngày mới với món ăn Hà Nội."),
                                itinerary(1, "09:30", "11:30", "Tham quan Văn Miếu", "Văn Miếu - Quốc Tử Giám", "Dành thời gian xem các bia tiến sĩ."),
                                itinerary(2, "08:30", "10:30", "Dạo quanh Hồ Gươm", "Hồ Hoàn Kiếm", "Ghé đền Ngọc Sơn và cầu Thê Húc.")),
                        List.of(
                                expense(ExpenseCategory.TRANSPORT, "2200000", 0, "Vé máy bay khứ hồi Hà Nội"),
                                expense(ExpenseCategory.ACCOMMODATION, "2000000", 0, "Khách sạn khu phố cổ Hà Nội"),
                                expense(ExpenseCategory.FOOD, "1450000", 1, "Ẩm thực cuối tuần Hà Nội")),
                        List.of(
                                checklist("Đặt vé máy bay Hà Nội", "Di chuyển", true, -20),
                                checklist("Đặt phòng phố cổ", "Lưu trú", true, -18),
                                checklist("Chuẩn bị giày đi bộ", "Hành lý", true, -2),
                                checklist("Mua quà lưu niệm", "Cá nhân", false, 2)),
                        List.of(
                                booking(BookingType.FLIGHT, "Vietnam Airlines", "HN-DEMO-01", "2200000", "Chuyến bay khứ hồi đến Nội Bài."),
                                booking(BookingType.HOTEL, "Old Quarter Boutique", "HN-HOTEL-01", "2000000", "Phòng đôi hai đêm tại khu phố cổ."))),
                new TripSeed(
                        "Nghỉ dưỡng Đà Nẵng", "da-nang", -1, 3, 4, "15000000",
                        TripStatus.ONGOING,
                        "Kỳ nghỉ gia đình bên biển kết hợp Sơn Trà và một ngày tham quan Hội An.",
                        List.of(
                                itinerary(1, "15:00", "17:30", "Tắm biển Mỹ Khê", "Bãi biển Mỹ Khê", "Nhận phòng trước khi ra biển."),
                                itinerary(2, "08:00", "11:00", "Khám phá bán đảo Sơn Trà", "Bán đảo Sơn Trà", "Tham quan chùa Linh Ứng."),
                                itinerary(3, "15:30", "21:00", "Dạo phố cổ Hội An", "Hội An", "Ngắm đèn lồng và thưởng thức cao lầu.")),
                        List.of(
                                expense(ExpenseCategory.TRANSPORT, "3200000", 0, "Vé tàu và taxi Đà Nẵng"),
                                expense(ExpenseCategory.ACCOMMODATION, "4800000", 0, "Resort ven biển Đà Nẵng"),
                                expense(ExpenseCategory.FOOD, "2100000", 1, "Ăn uống gia đình tại Đà Nẵng")),
                        List.of(
                                checklist("Xác nhận phòng resort", "Lưu trú", true, -10),
                                checklist("Chuẩn bị kem chống nắng", "Hành lý", true, -2),
                                checklist("Thuê xe đi Sơn Trà", "Di chuyển", false, 1),
                                checklist("Đặt bàn hải sản", "Ẩm thực", false, 2)),
                        List.of(
                                booking(BookingType.TRAIN, "Đường sắt Việt Nam", "DN-TRAIN-02", "3200000", "Khoang giường nằm cho gia đình."),
                                booking(BookingType.HOTEL, "My Khe Beach Resort", "DN-HOTEL-02", "4800000", "Phòng gia đình hướng biển bốn đêm."))),
                new TripSeed(
                        "Săn mây Đà Lạt", "da-lat", 14, 17, 3, "8500000",
                        TripStatus.PLANNED,
                        "Chuyến đi cao nguyên săn mây, thăm vườn hoa và thưởng thức cà phê Đà Lạt.",
                        List.of(
                                itinerary(1, "05:00", "08:00", "Săn mây đồi chè", "Cầu Đất", "Khởi hành sớm và mang áo ấm."),
                                itinerary(2, "09:00", "11:00", "Thăm vườn hoa thành phố", "Vườn hoa Đà Lạt", "Dành thời gian chụp ảnh mùa hoa."),
                                itinerary(3, "08:30", "10:30", "Uống cà phê bên rừng thông", "Ngoại ô Đà Lạt", "Thưởng thức cà phê rang tại chỗ.")),
                        List.of(
                                expense(ExpenseCategory.TRANSPORT, "1800000", 0, "Vé xe giường nằm Đà Lạt"),
                                expense(ExpenseCategory.ACCOMMODATION, "2700000", 0, "Homestay rừng thông Đà Lạt"),
                                expense(ExpenseCategory.TICKET, "650000", 1, "Vé tham quan các điểm Đà Lạt")),
                        List.of(
                                checklist("Kiểm tra dự báo mây", "Lịch trình", true, -3),
                                checklist("Chuẩn bị áo ấm", "Hành lý", false, -1),
                                checklist("Sạc pin máy ảnh", "Thiết bị", false, -1),
                                checklist("Đặt xe đi Cầu Đất", "Di chuyển", true, -5)),
                        List.of(
                                booking(BookingType.BUS, "Phương Trang", "DL-BUS-03", "1800000", "Vé xe giường nằm khứ hồi."),
                                booking(BookingType.HOTEL, "Pine Hill Homestay", "DL-HOTEL-03", "2700000", "Ba đêm tại homestay gần rừng thông."))),
                new TripSeed(
                        "Khám phá Phú Quốc", "phu-quoc", 45, 49, 2, "18000000",
                        TripStatus.DRAFT,
                        "Bản nháp kỳ nghỉ đảo ngọc với tour biển, chợ đêm và thời gian thư giãn.",
                        List.of(
                                itinerary(1, "16:00", "18:30", "Ngắm hoàng hôn bãi Trường", "Bãi Trường", "Chọn vị trí ngắm hoàng hôn gần bờ biển."),
                                itinerary(2, "08:00", "16:00", "Tour cano ba đảo", "Quần đảo An Thới", "Mang đồ bơi và túi chống nước."),
                                itinerary(3, "18:30", "21:00", "Khám phá chợ đêm", "Chợ đêm Dương Đông", "Thử hải sản và đặc sản địa phương.")),
                        List.of(
                                expense(ExpenseCategory.TRANSPORT, "4600000", 0, "Vé máy bay dự kiến Phú Quốc"),
                                expense(ExpenseCategory.ACCOMMODATION, "6200000", 0, "Đặt cọc resort Phú Quốc"),
                                expense(ExpenseCategory.TICKET, "2400000", 1, "Tour cano ba đảo Phú Quốc")),
                        List.of(
                                checklist("So sánh vé máy bay Phú Quốc", "Di chuyển", true, -30),
                                checklist("Chọn resort gần biển", "Lưu trú", false, -25),
                                checklist("Chuẩn bị túi chống nước", "Hành lý", false, -2),
                                checklist("Đặt tour cano", "Lịch trình", false, -10)),
                        List.of(
                                booking(BookingType.FLIGHT, "Bamboo Airways", "PQ-FLIGHT-04", "4600000", "Giữ chỗ chuyến bay đến Phú Quốc."),
                                booking(BookingType.TOUR, "An Thới Explorer", "PQ-TOUR-04", "2400000", "Tour cano ba đảo cho hai người."))),
                new TripSeed(
                        "Tokyo mùa thu", "tokyo", 90, 96, 2, "45000000",
                        TripStatus.CANCELLED,
                        "Kế hoạch mùa thu Tokyo được lưu lại để tham khảo sau khi lịch bay thay đổi.",
                        List.of(
                                itinerary(1, "09:00", "12:00", "Tham quan đền Meiji", "Shibuya, Tokyo", "Đi bộ qua khu rừng dẫn vào đền."),
                                itinerary(2, "08:00", "11:00", "Ngắm lá đỏ ở Shinjuku Gyoen", "Shinjuku, Tokyo", "Kiểm tra lịch mở cửa trước chuyến đi."),
                                itinerary(3, "16:00", "20:00", "Khám phá Asakusa", "Asakusa, Tokyo", "Thăm chùa Sensō-ji và phố Nakamise.")),
                        List.of(
                                expense(ExpenseCategory.TRANSPORT, "15000000", 0, "Đặt cọc vé máy bay Tokyo"),
                                expense(ExpenseCategory.ACCOMMODATION, "12000000", 0, "Đặt cọc khách sạn Tokyo"),
                                expense(ExpenseCategory.TICKET, "1800000", 2, "Vé tham quan dự kiến tại Tokyo")),
                        List.of(
                                checklist("Kiểm tra hạn hộ chiếu", "Giấy tờ", true, -60),
                                checklist("Chuẩn bị hồ sơ visa", "Giấy tờ", true, -55),
                                checklist("Đổi tiền yên", "Tài chính", false, -7),
                                checklist("Mua SIM du lịch", "Kết nối", false, -3)),
                        List.of(
                                booking(BookingType.FLIGHT, "Japan Airlines", "TYO-FLIGHT-05", "15000000", "Đặt chỗ chuyến bay đã được bảo lưu."),
                                booking(BookingType.HOTEL, "Tokyo Central Hotel", "TYO-HOTEL-05", "12000000", "Đặt phòng có chính sách hoàn hủy."))));
    }

    private static ItinerarySeed itinerary(
            int dayNo, String fromTime, String toTime, String activity, String location, String note) {
        return new ItinerarySeed(
                dayNo, LocalTime.parse(fromTime), LocalTime.parse(toTime), activity, location, note);
    }

    private static ExpenseSeed expense(
            ExpenseCategory category, String amount, int dayOffset, String note) {
        return new ExpenseSeed(category, amount, dayOffset, note);
    }

    private static ChecklistSeed checklist(
            String title, String category, boolean done, int dueDateOffset) {
        return new ChecklistSeed(title, category, done, dueDateOffset);
    }

    private static BookingSeed booking(
            BookingType type, String provider, String code, String price, String note) {
        return new BookingSeed(type, provider, code, price, note);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DestinationSeed(
            String key, String name, String city, String country, String description, String imageUrl) {
    }

    private record TripSeed(
            String title,
            String destinationKey,
            int startOffsetDays,
            int endOffsetDays,
            int peopleCount,
            String budget,
            TripStatus status,
            String notes,
            List<ItinerarySeed> itinerary,
            List<ExpenseSeed> expenses,
            List<ChecklistSeed> checklist,
            List<BookingSeed> bookings) {
    }

    private record ItinerarySeed(
            int dayNo, LocalTime fromTime, LocalTime toTime, String activity, String location, String note) {
    }

    private record ExpenseSeed(ExpenseCategory category, String amount, int dayOffset, String note) {
    }

    private record ChecklistSeed(String title, String category, boolean done, int dueDateOffset) {
    }

    private record BookingSeed(
            BookingType type, String provider, String bookingCode, String price, String note) {
    }
}
