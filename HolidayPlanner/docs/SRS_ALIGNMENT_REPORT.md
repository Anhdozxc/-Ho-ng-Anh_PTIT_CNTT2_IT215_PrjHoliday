# SRS Alignment Report

## 1. Phạm vi và nguồn sự thật

- Audit ngày 17/07/2026 trên branch `fix/srs-compliance-and-stability`.
- Thứ tự ưu tiên: `HolidayPlanner_SRS_FINAL.docx` -> `HolidayPlanner_Technical_FINAL.xlsx` -> AC-01..AC-07 -> source trong `src/main`, `src/test`, `pom.xml` và `resources`.
- Không sử dụng `.class`, `target/classes`, `target/test-classes` hoặc `target/maven-status` làm nguồn khôi phục source.
- Baseline tái lập: 46 source main, 3 source test, 6 test chạy thành công; 0 failure, 0 error, 0 skipped. Máy audit chỉ có JDK 21, Maven compile với `--release 17`; chạy đúng runtime JDK 17 cần được xác minh riêng.

## 2. Quyết định khi yêu cầu chưa hoàn toàn thống nhất

| Chủ đề | Mâu thuẫn/điểm chưa rõ | Quyết định an toàn |
| --- | --- | --- |
| Expense amount | SRS ghi số tiền `>= 0`; DTO hiện tại yêu cầu `> 0` | Cho phép `0.00`, cấm số âm, dùng `BigDecimal` scale 2 với `HALF_UP` để service không ném `ArithmeticException`. |
| Checklist create | Bảng dữ liệu SRS đánh dấu `isDone` bắt buộc; luồng nghiệp vụ nói tạo rồi đánh dấu hoàn thành | Create mặc định `false`, không nhận `done`; trạng thái được cập nhật idempotent bằng `PATCH`. |
| Destination delete | Bảng chức năng nói thêm/sửa/xóa; UC08 nói điểm đến đã dùng không nên xóa cứng và cho phép ẩn | Dùng soft hide/show; không xóa cứng dữ liệu đang/đã được tham chiếu. |
| Cross-owner | SRS/AC nói truy cập chéo bị từ chối nhưng không chỉ rõ 403 hay 404 | Theo AC và lệnh triển khai: tồn tại nhưng khác owner trả JSON/HTML 403; ID không tồn tại trả 404. |
| Trip status | Sơ đồ thể hiện các cạnh trạng thái; source cho phép gán tùy ý | Chỉ cho DRAFT -> PLANNED/CANCELLED; PLANNED -> ONGOING/CANCELLED; ONGOING -> COMPLETED/CANCELLED; terminal state không đổi tiếp. |
| Profile/avatar | SRS có quản lý hồ sơ, avatar/Cloudinary nhưng không có đặc tả field/API chi tiết | Triển khai tối thiểu họ tên, đổi mật khẩu và avatar; Cloudinary chỉ bật khi đủ credential, không hardcode secret, thiếu cấu hình không làm app dừng. |
| Login lockout | UC02 nói hệ thống "có thể" tạm khóa sau nhiều lần sai | Không thêm cơ chế lockout tự động ngoài scope; tài khoản do Admin đặt `LOCKED` phải bị từ chối đăng nhập. |

## 3. Endpoint audit trước khi sửa

### 3.1 Endpoint hiện có

| Nhóm | Method và path |
| --- | --- |
| Auth REST | `POST /api/auth/register` |
| Dashboard REST | `GET /api/dashboard` |
| Trip REST | `GET /api/trips`, `POST /api/trips`, `GET /api/trips/{id}`, `PUT /api/trips/{id}`, `DELETE /api/trips/{id}` |
| Itinerary REST | `POST /api/trips/{id}/itinerary`, `DELETE /api/trips/{id}/itinerary/{itemId}` |
| Expense REST | `POST /api/trips/{id}/expenses`, `DELETE /api/trips/{id}/expenses/{expenseId}` |
| Checklist REST | `POST /api/trips/{id}/checklist`, `PATCH /api/trips/{id}/checklist/{itemId}/toggle`, `DELETE /api/trips/{id}/checklist/{itemId}` |
| Booking REST | `POST /api/trips/{id}/bookings`, `DELETE /api/trips/{id}/bookings/{bookingId}` |
| Admin Destination REST | `GET /api/admin/destinations`, `POST /api/admin/destinations`, `PUT /api/admin/destinations/{id}`, `DELETE /api/admin/destinations/{id}` |
| Admin User REST | `GET /api/admin/users`, `PATCH /api/admin/users/{id}/toggle` |
| Auth web | `GET /login`, `POST /login` (Spring Security), `GET /register`, `POST /register`, `POST /logout` |
| Trip web | `GET /trips`, `GET /trips/new`, `POST /trips`, `GET /trips/{id}`, `GET /trips/{id}/edit`, `POST /trips/{id}`, `POST /trips/{id}/delete` |
| Trip detail web | create/delete itinerary, expense, booking; create/toggle/delete checklist |
| Admin web | list/create/hide-show destination; list/lock-unlock user |

### 3.2 Endpoint còn thiếu hoặc cần sửa semantics

| Mức | Endpoint cần có/sửa | Gap baseline |
| --- | --- | --- |
| P0 | `GET /api/profile` | Chưa có profile module. |
| P0 | `PUT /api/profile` | Chưa cập nhật họ tên. |
| P0 | `PUT /api/profile/password` | Chưa kiểm tra mật khẩu hiện tại/BCrypt/xác nhận. |
| P0 | `POST /api/profile/avatar`, `DELETE /api/profile/avatar` | Chưa có media/Cloudinary/fallback/validation. |
| P0 | `PUT /api/trips/{id}/itinerary/{itemId}` | Itinerary mới có create/delete. |
| P0 | `PUT /api/trips/{id}/expenses/{expenseId}` | Expense mới có create/delete. |
| P0 | `PUT /api/trips/{id}/checklist/{itemId}` | Checklist chưa sửa title/category/dueDate. |
| P0 | `PATCH /api/trips/{id}/checklist/{itemId}/state` | Toggle hiện tại không idempotent. |
| P0 | `PUT /api/trips/{id}/bookings/{bookingId}` | Booking mới có create/delete. |
| P0 | `PATCH /api/trips/{id}/status` hoặc validate status trong `PUT` | Source cho phép mọi transition. |
| P0 | API 401/403 handlers | Chưa bảo đảm JSON có cấu trúc; có thể redirect/HTML. |
| P1 | `PATCH /api/admin/destinations/{id}/status` | Baseline dùng `DELETE` cho toggle, không idempotent. |
| P1 | `PATCH /api/admin/users/{id}/status` | Baseline dùng toggle mơ hồ. |
| P1 | Search/filter/sort trip/admin lists | Chưa có query params/service/repository tương ứng. |

## 4. Màn hình và frontend audit trước khi sửa

Màn hình hiện có: Login, Register, Dashboard, Trip List, Trip Create/Edit, Trip Detail (4 tab), Admin Destination, Admin User.

Màn hình/luồng còn thiếu:

- `/profile` cho họ tên, mật khẩu, avatar.
- Trang lỗi thân thiện 403, 404, 500.
- UI edit cho itinerary, expense, checklist, booking.
- UI edit destination và search/filter admin.
- Search/filter/sort trip list.
- Remember-me, profile menu, enum tiếng Việt, modal xác nhận dùng chung.
- Giữ đúng tab sau thao tác, giữ field errors và giá trị nhập khi validation fail.
- Form edit trip phải giữ destination hiện tại nếu đã inactive và không cho chọn inactive khác.

Baseline frontend chỉ có 8 template, một `fragments.html`, một `app.css` 137 dòng; không có JS hoặc image asset. Form Trip không tải Bootstrap bundle nên navbar mobile không hoạt động. Các trang chưa có design tokens đầy đủ, reduced motion, image fallback, modal focus management, loading state và responsive coverage 375/768/1024/1440/1920.

## 5. Test source và artifact audit trước khi sửa

Source test thật:

- `HolidayPlannerApplicationTests`: context load.
- `TripPlanServiceTest`: create/soft-delete, invalid date, cross-owner.
- `UserServiceTest`: normalize email + BCrypt, duplicate email.

Thiếu toàn bộ test MVC/API/MockMvc, security 401/403/CSRF, Profile/Media, TripDetail CRUD/409, Dashboard, Destination, admin status, state transition và inactive destination.

Artifact stale phải loại khỏi Git:

- 83 file dưới `HolidayPlanner/target/`, bao gồm 19 compiled test class nhưng chỉ có 3 test source thật.
- 11 file IntelliJ dưới `.idea/`.
- Baseline chưa có `.gitignore`; Maven clean tái tạo `target/` dưới dạng untracked.

## 6. Ma trận alignment

| Requirement | SRS yêu cầu | Source hiện tại (baseline) | Gap | File dự kiến sửa | Test tương ứng | Trạng thái audit |
| --- | --- | --- | --- | --- | --- | --- |
| UC01 Đăng ký | Họ tên/email/password; email unique; password >= 8; BCrypt | Có web + REST; normalize email và BCrypt | Thiếu MVC/API validation đầy đủ | `UserService`, auth controllers/DTO | UserService + MockMvc register | PARTIAL |
| UC02 Login/logout | Session, locked user bị từ chối, rememberMe | Form login/logout; locked mapped disabled | Thiếu remember-me, JSON 401/403 test | `SecurityConfig`, login template | Security MockMvc + locked login | PARTIAL |
| UC03 TripPlan | CRUD, soft delete, owner, date/budget/people/status | Có CRUD/soft delete/owner query | Cross-owner thành 404; không state machine; thiếu search/filter/sort | `TripPlanService/Repository`, controllers | CRUD/ownership/validation/transitions/inactive destination | PARTIAL |
| UC04 Itinerary | CRUD, day/time, sort, overlap | Create/read/delete; sort và một phần validation | Thiếu update + overlap 409 + UI edit | `TripDetailService`, repository, REST/web | CRUD/day/time/overlap/cross-owner | FAIL |
| UC05 Expense | CRUD, category filter, amount >= 0, total | Create/read/delete | Thiếu update/filter; 0 bị cấm; scale unsafe; thiếu spentDate rule | `TripDetailService`, DTO/repository/controllers | CRUD/filter/amount/scale/date | FAIL |
| UC06 Checklist | CRUD, edit fields, done state | Create/read/toggle/delete | Thiếu update; toggle không idempotent | `TripDetailService`, REST/web | CRUD/state/ownership | FAIL |
| UC07 Dashboard | USER personal, ADMIN aggregate, upcoming, empty state | Có scope và stats cơ bản | Upcoming còn cancelled/completed; limit in-memory; nguy cơ lazy/N+1 | `TripPlanRepository`, `DashboardService`, UI | owner/admin/exclusion/order/limit | PARTIAL |
| UC08 Destination Admin | list/search/create/update/hide/show, image validation | REST update + soft hide; web create/hide/show | Thiếu web edit/search, status explicit, image URL/file validation | `DestinationService`, admin controllers/templates | CRUD/search/hide/show/in-use/image | PARTIAL |
| UC09 BookingNote | CRUD; provider required; code optional; price non-negative | Create/read/delete | Thiếu update; scale unsafe; UI edit | `TripDetailService`, DTO/controllers | CRUD/negative/ownership | FAIL |
| UC10 User Admin | list/search, role/status filter, lock/unlock, self-lock deny | list/toggle; self-lock có | Thiếu search/filter/idempotent DTO/last-admin test | `UserService`, admin controllers/templates | locked/self-lock/last-admin/search/filter | PARTIAL |
| Profile | User/Admin quản lý hồ sơ cá nhân | Chưa có | Thiếu toàn bộ | new profile DTO/controller/template + `UserService` | profile get/update/password | FAIL |
| Avatar/Cloudinary | Avatar/media qua Cloudinary | Chưa có | Thiếu toàn bộ; phải optional/fallback | `UserAccount`, new `media/*`, profile controllers/config | validator/storage unavailable/replacement rollback | FAIL |
| NFR-01 | BCrypt, RBAC, service ownership, API 401/403, web CSRF | BCrypt/RBAC/CSRF một phần | 401/403 JSON và 403 cross-owner sai; thiếu handler web/API | `SecurityConfig`, handlers/services | MockMvc 401/403/CSRF/owner | PARTIAL |
| NFR-02 | Date, people, non-negative money, itinerary range | Có một phần DTO/service validation | Scale, overlap, spentDate, state chưa đồng bộ | DTO + services | validation matrix | PARTIAL |
| NFR-03 | Dashboard/list < 3s, owner queries | Owner queries có | Upcoming fetch/limit/N+1 cần sửa | repositories/services | query behavior + limit | PARTIAL |
| NFR-04 | Responsive, empty/error/success, delete confirm | Có Bootstrap cơ bản | Thiếu premium responsive, modal, error pages, accessibility | templates/CSS/JS | MVC render + manual viewport | FAIL |
| NFR-05 | 3 layer, DTO/validation/enum, README/Postman | Kiến trúc 3 layer có | DTO/API/docs/test chưa đủ | toàn bộ module + docs/postman | build + contract tests | PARTIAL |
| NFR-06 | Java 17+, Maven, MySQL 8, browser hiện đại | `release 17`, MySQL config | Chưa chạy runtime JDK 17/MySQL sạch | `pom.xml`, properties, README | package + MySQL smoke | PARTIAL |
| NFR-07 | Test module; không còn Critical/High | 6 test pass | Coverage chức năng/security rất thấp | `src/test` | full suite | FAIL |
| AC-01 | Register/login/BCrypt/locked | Core có | Thiếu integration evidence | auth/security | API + web integration | PARTIAL |
| AC-02 | Owner only; cross-owner denied | Owner query có | Sai status 404 thay vì 403 | trip services | cross-owner 403 | FAIL |
| AC-03 | Full CRUD 5 aggregate types | Trip có; child resources partial | Thiếu update nhiều module | services/controllers/UI | CRUD matrix | FAIL |
| AC-04 | Dashboard 3 stats + upcoming | Có stats | Upcoming filter/limit sai | dashboard repository/service | dashboard suite | PARTIAL |
| AC-05 | Admin Destination/User; USER denied | Role guard có | Admin CRUD/search/status chưa đủ; thiếu JSON evidence | admin modules | ADMIN 200/USER 403 | PARTIAL |
| AC-06 | Postman/manual smoke; no Critical/High | Collection có baseline | Thiếu assertions/dynamic IDs/negative cases và bằng chứng chạy | postman + guide | Newman/manual log | FAIL |
| AC-07 | IntelliJ/MySQL/README/seed/run guide | Seed/MySQL/README cơ bản | Config secret/default/error handling và docs chưa phản ánh thực tế | properties/README/docs | clean clone/run smoke | PARTIAL |

## 7. Kế hoạch file và test ưu tiên

1. Repository hygiene + safe config.
2. API security JSON + ownership 403 + exception contract.
3. Profile/password/avatar và optional media storage.
4. Full CRUD/validation cho itinerary, expense, checklist, booking.
5. Trip state machine, inactive destination và dashboard query.
6. Admin search/filter/idempotent status và trip search/filter/sort.
7. Luxury light design system, responsive/accessibility, modal/tab state.
8. Unit/service/MockMvc regression tests, Postman và tài liệu bằng chứng.

Trạng thái trong bảng sẽ được cập nhật theo bằng chứng test/build/smoke thực tế; không chuyển thành PASS chỉ dựa trên việc có code.
