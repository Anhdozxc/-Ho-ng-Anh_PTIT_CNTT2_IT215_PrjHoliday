# SRS Alignment Report

## 1. Phạm vi và nguồn sự thật

- Branch triển khai: `fix/srs-compliance-and-stability`.
- Base commit của gói nhận: `9ab1d152fb9f4e90cc2501f2969317c7a5fcc80f`.
- Thứ tự ưu tiên: SRS FINAL → Technical FINAL → AC-01..AC-07 → source thật.
- Source thật chỉ gồm `src/main`, `src/test`, `pom.xml`, `resources`, `database`, `postman` và tài liệu; không dùng `.class` hoặc `target/maven-status`.
- Tài liệu yêu cầu trong `../documents` được giữ nguyên.

## 2. Baseline trước khi sửa

Baseline audit ban đầu có:

- 46 main Java files.
- 3 test source files.
- 6 tests pass.
- Frontend 8 template, một CSS nhỏ, không JavaScript/design system/media/profile hoàn chỉnh.
- Nhiều class chỉ tồn tại trong `target/` cũ nhưng thiếu source.
- Child resources chỉ create/delete ở nhiều module.
- Cross-owner trả 404 thay vì 403.
- Dashboard upcoming chưa lọc đầy đủ.
- Chưa có Profile/Avatar/Cloudinary source thật.
- Postman ít assertion và ID còn phụ thuộc dữ liệu.

Baseline này được lưu để truy vết, không phản ánh trạng thái source cuối.

## 3. Quyết định nghiệp vụ khi tài liệu chưa hoàn toàn thống nhất

| Chủ đề | Quyết định triển khai |
|---|---|
| Expense amount | Cho phép `0.00`, cấm số âm, tối đa 13 chữ số nguyên và 2 chữ số thập phân; reject scale sai thay vì làm tròn âm thầm. |
| Checklist create | Create mặc định `done=false`; thay đổi trạng thái bằng desired-state `PATCH`, idempotent. |
| Destination delete | Dùng hide/show; không hard-delete destination đã/đang được trip tham chiếu. |
| Cross-owner | Resource tồn tại nhưng khác owner trả 403; ID không tồn tại trả 404. |
| Trip state | DRAFT → PLANNED/CANCELLED; PLANNED → ONGOING/CANCELLED; ONGOING → COMPLETED/CANCELLED; terminal state không chuyển tiếp; sửa cùng trạng thái được phép. |
| Profile/avatar | Triển khai họ tên, đổi mật khẩu, avatar; Cloudinary chỉ bật khi đủ credential và thiếu cấu hình không làm app dừng. |
| Login lockout | Không thêm lockout tự động ngoài SRS; tài khoản do ADMIN đặt `LOCKED` bị từ chối đăng nhập. |

## 4. Inventory sau triển khai

| Thành phần | Trạng thái cuối |
|---|---|
| Main Java source | 77 files |
| Test source | 18 files |
| Thymeleaf templates | 13 |
| CSS/JS frontend | 5 CSS + 4 JS |
| Local destination/fallback assets | Có |
| Real favicon ICO + SVG | Có |
| Postman | 61 requests, 93 assertions, 25 variables |
| Browser evidence | 45 pass, 0 fail trong lần chạy đang lưu |
| Java 17 CI | `.github/workflows/maven-verify.yml` |

## 5. Endpoint cuối

### Public/Auth

- `GET /login`, `POST /login`.
- `GET /register`, `POST /register`.
- `POST /logout`.
- `POST /api/auth/register`.
- `GET /favicon.ico`.

### Profile

- `GET /profile`, `POST /profile`.
- `POST /profile/password`.
- `POST /profile/avatar`, `POST /profile/avatar/delete`.
- `GET /api/profile`, `PUT /api/profile`.
- `PUT|POST /api/profile/password`.
- `POST /api/profile/avatar`, `DELETE /api/profile/avatar`.

### TripPlan

- Web list/search/filter/sort/page, create, detail, edit, soft delete.
- `GET|POST /api/trips`.
- `GET|PUT|DELETE /api/trips/{id}`.
- Status được validate trong create/update theo state machine.

### Itinerary

- Web create/update/delete trong Trip detail.
- `POST /api/trips/{id}/itinerary`.
- `PUT /api/trips/{id}/itinerary/{itemId}`.
- `DELETE /api/trips/{id}/itinerary/{itemId}`.

### Expense

- Web create/update/delete/filter category.
- `POST /api/trips/{id}/expenses`.
- `PUT /api/trips/{id}/expenses/{expenseId}`.
- `DELETE /api/trips/{id}/expenses/{expenseId}`.

### Checklist

- Web create/update/desired-state/delete.
- `POST /api/trips/{id}/checklist`.
- `PUT /api/trips/{id}/checklist/{itemId}`.
- `PATCH /api/trips/{id}/checklist/{itemId}/state`.
- Alias toggle cũ được giữ để tương thích.
- `DELETE /api/trips/{id}/checklist/{itemId}`.

### BookingNote

- Web create/update/delete.
- `POST /api/trips/{id}/bookings`.
- `PUT /api/trips/{id}/bookings/{bookingId}`.
- `DELETE /api/trips/{id}/bookings/{bookingId}`.

### Dashboard

- `GET /dashboard`.
- `GET /api/dashboard`.

### Admin Destination

- Web list/search/filter/page/create/update/hide/show/image.
- `GET|POST /api/admin/destinations`.
- `PUT|DELETE /api/admin/destinations/{id}`.
- `PATCH /api/admin/destinations/{id}/status`.
- `POST|DELETE /api/admin/destinations/{id}/image`.

### Admin User

- Web list/search/filter/page/lock/unlock.
- `GET /api/admin/users`.
- `PATCH /api/admin/users/{id}/status`.
- Alias `/toggle` được giữ để tương thích.

## 6. Final UC matrix

| Requirement | SRS yêu cầu | Implementation | Test/evidence | Final status |
|---|---|---|---|---|
| UC01 Register | Name/email/password, unique email, BCrypt | DTO + service + web/API | DTO/service/MockMvc | PASS |
| UC02 Login/logout | Session, locked, remember-me | Spring Security + custom user details | Security/web tests | PASS |
| UC03 TripPlan | CRUD, owner, validation, status | Full web/API, soft delete, state machine, paging | Service/API/web tests | PASS |
| UC04 Itinerary | CRUD, day/time/sort/overlap | Full CRUD; overlap conflict | Service/API tests | PASS |
| UC05 Expense | CRUD, category, amount/total | Full CRUD/filter/BigDecimal | Service/API tests | PASS |
| UC06 Checklist | CRUD and done state | Full CRUD + idempotent state | Service/API tests | PASS |
| UC07 Dashboard | Personal/admin totals/upcoming | Scoped repository queries, top 5, excluded statuses | Service/API + browser | PASS |
| UC08 Destination Admin | Search/create/update/hide/show/image | Web/API + URL/upload/fallback | Service/API/web tests | PASS |
| UC09 BookingNote | CRUD, provider/code/price | Full web/API CRUD | Service/API tests | PASS |
| UC10 User Admin | Search/filter/lock/unlock | Web/API desired state, self/last-admin guard | Service/API tests | PASS |
| Profile | Personal profile/password | Web/API | Service/API/web tests | PASS |
| Avatar/Cloudinary | Managed media | Optional provider, validation, rollback/fallback | Media tests | PASS |

## 7. NFR và AC matrix

| ID | Kết quả | Bằng chứng/ghi chú |
|---|---|---|
| NFR-01 Security | PASS | BCrypt, RBAC, 401/403 JSON, service ownership, CSRF web. |
| NFR-02 Validation | PASS | DTO + service defensive validation, money/date/time/media. |
| NFR-03 Response/query scope | PASS cho phạm vi môn học | Repository paging/top-five, `open-in-view=false`, batch fetch. Không có load benchmark production. |
| NFR-04 UX/responsive | PASS với evidence hiện có | Luxury light design system; browser JSON 45/45. Final enhanced script nên chạy lại. |
| NFR-05 Maintainability | PASS | Controller/service/repository, DTO, reusable fragments/components, docs/Postman. |
| NFR-06 Java/Maven/MySQL | CONDITIONAL FINAL VERIFY | Java target 17, previous MySQL smoke reported; Java 17 CI added. |
| NFR-07 Testing | CONDITIONAL FINAL VERIFY | Previous run reported 125/125; final rerun needed after favicon test. |
| AC-01 | PASS | Register/login/BCrypt/locked implemented/tested. |
| AC-02 | PASS | Ownership and cross-owner 403. |
| AC-03 | PASS | Full CRUD Trip + four child aggregates. |
| AC-04 | PASS | Dashboard stats and upcoming. |
| AC-05 | PASS | ADMIN destination/user, USER denied. |
| AC-06 | CONDITIONAL FINAL VERIFY | Postman collection complete; live Runner output not included. |
| AC-07 | PASS implementation / final environment check | README, seed, MySQL config, scripts and CI present. |

## 8. Frontend alignment

- Light theme only.
- Shared design tokens, components, page styles and responsive rules.
- Cinematic hero, premium cards, timeline, budget/checklist progress, admin tables/mobile cards.
- Scroll reveal, stagger, counters, parallax and sticky navbar.
- `prefers-reduced-motion` support.
- Accessible confirmation dialog with focus trap and Escape.
- Inline validation, loading state, double-submit prevention, dirty-form guard and image preview.
- Local destination/fallback images; legacy Unsplash URL is only recognized for seed migration.
- Real ICO favicon plus SVG modern icon.
- Browser script verifies overflow, reveal visibility, broken images, favicon, console and network.

## 9. Verification status

Evidence and limitations are recorded in:

- `docs/FINAL_VERIFICATION_REPORT.md`.
- `docs/POSTMAN_EXECUTION_REPORT.md`.
- `docs/RELEASE_CHECKLIST.md`.
- `docs/screenshots/browser-smoke.json`.

Implementation alignment is complete. Submission readiness remains conditional on one final clean Maven/Java 17 run and a live core Postman run; those results must not be fabricated.
