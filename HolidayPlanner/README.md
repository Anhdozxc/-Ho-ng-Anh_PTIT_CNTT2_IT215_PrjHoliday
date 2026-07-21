# Holiday Planner

Holiday Planner là ứng dụng Spring Boot quản lý kế hoạch du lịch cá nhân theo SRS `HolidayPlanner_SRS_FINAL`. Ứng dụng có giao diện Thymeleaf responsive, REST API dùng được với Postman, phân quyền `USER`/`ADMIN`, quản lý đầy đủ TripPlan và dữ liệu con, dashboard, ảnh đại diện và ảnh điểm đến qua Cloudinary.

SRS là nguồn yêu cầu nghiệp vụ cao nhất. Các phần media, demo data và giao diện mở rộng không thay đổi phạm vi UC01–UC10.

## Công nghệ

- Java 17+, Maven 3.9+.
- Spring Boot 3.4.5, Spring MVC, Thymeleaf, Spring Security.
- Spring Data JPA/Hibernate, MySQL 8.x.
- H2 `MODE=MySQL` cho automated tests.
- Cloudinary Java SDK cho lưu trữ ảnh có quản lý.
- Bootstrap WebJar, CSS và JavaScript local; không phụ thuộc CDN khi trình chiếu.

## Chức năng chính

- Đăng ký, đăng nhập, đăng xuất, remember-me, hồ sơ, đổi mật khẩu và avatar.
- BCrypt, email duy nhất không phân biệt hoa thường, validation phía server và inline validation phía UI.
- RBAC `USER`/`ADMIN`; kiểm tra ownership tại service cho trip và mọi bản ghi con.
- Tìm kiếm, lọc, sắp xếp, tạo, sửa và xóa mềm TripPlan.
- CRUD lịch trình, chi phí, checklist và ghi chú đặt dịch vụ.
- Checklist dùng desired state `{ "done": true|false }`, nên gửi lặp lại cùng trạng thái không làm đảo trạng thái ngoài ý muốn.
- Dashboard cá nhân cho USER và dashboard toàn hệ thống cho ADMIN.
- ADMIN tìm kiếm/phân trang điểm đến, quản lý ảnh điểm đến và khóa/mở tài khoản bằng desired state.
- Giao diện luxury light-only, toast, confirmation modal, responsive mobile và ảnh fallback local.
- REST error contract thống nhất cho các lỗi nghiệp vụ và bảo mật.

## Cấu trúc

```text
HolidayPlanner/
├── pom.xml
├── README.md
├── database/
│   ├── holiday_planner.sql
│   └── 20260716_add_media_columns.sql
├── docs/                  Alignment, test, release và browser evidence
├── postman/HolidayPlanner.postman_collection.json
├── scripts/
│   ├── browser-smoke.mjs
│   └── final-verify.ps1
├── src/main/java/vn/edu/ptit/holidayplanner/
│   ├── api/          REST controllers, mapper và error handler
│   ├── config/       Security, Cloudinary và demo seed
│   ├── domain/       JPA entities/enums
│   ├── dto/          Request/response DTO và Bean Validation
│   ├── media/        Image storage abstraction và Cloudinary implementation
│   ├── repository/   Spring Data repositories
│   ├── service/      Nghiệp vụ, transaction và ownership
│   └── web/          MVC controllers và web error handler
├── src/main/resources/
│   ├── application.properties
│   ├── application-prod.properties
│   ├── static/
│   └── templates/
└── src/test/
```

## Yêu cầu môi trường

- JDK 17 trở lên.
- Maven 3.9 trở lên hoặc Maven tích hợp trong IntelliJ IDEA.
- MySQL 8.x đang chạy.
- Cổng `8080` chưa được tiến trình khác sử dụng.
- Tài khoản Cloudinary chỉ bắt buộc khi cần upload ảnh; ứng dụng vẫn khởi động nếu chưa cấu hình.

## Biến môi trường

| Biến | Bắt buộc | Mục đích / mặc định local |
|---|---:|---|
| `DB_URL` | Có ở môi trường thật | JDBC URL; local mặc định dùng database `holiday_planner` tại `localhost:3306` |
| `DB_USERNAME` | Có ở môi trường thật | Tài khoản MySQL; local mặc định `root` |
| `DB_PASSWORD` | Có ở môi trường thật | Mật khẩu MySQL; local theo SRS mặc định `123456`, phải ghi đè ở môi trường thật |
| `COOKIE_SECURE` | Không | Đặt `true` khi ứng dụng được phục vụ qua HTTPS; local mặc định `false` |
| `REMEMBER_ME_KEY` | Có ở môi trường thật | Khóa remember-me dài, ngẫu nhiên; local sẽ sinh khóa tạm theo từng process nếu bỏ trống |
| `CLOUDINARY_CLOUD_NAME` | Khi upload ảnh | Cloud name của tài khoản Cloudinary |
| `CLOUDINARY_API_KEY` | Khi upload ảnh | API key của Cloudinary |
| `CLOUDINARY_API_SECRET` | Khi upload ảnh | API secret của Cloudinary; không commit/log |
| `CLOUDINARY_FOLDER` | Không | Root folder; mặc định `holiday-planner` |
| `APP_SEED_DEMO_DATA` | Không | Bật/tắt seed; local mặc định `true`, profile `prod` mặc định `false` |
| `SPRING_PROFILES_ACTIVE` | Không | Đặt `prod` khi chạy cấu hình production |

Ví dụ PowerShell chỉ dùng placeholder:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/holiday_planner?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME="<mysql-user>"
$env:DB_PASSWORD="<mysql-password>"
$env:COOKIE_SECURE="false"
$env:REMEMBER_ME_KEY="<long-random-secret>"

$env:CLOUDINARY_CLOUD_NAME="<your-cloud-name>"
$env:CLOUDINARY_API_KEY="<your-api-key>"
$env:CLOUDINARY_API_SECRET="<your-api-secret>"
$env:CLOUDINARY_FOLDER="holiday-planner"
$env:APP_SEED_DEMO_DATA="true"
```

Linux/macOS dùng `export` với cùng tên biến. File `.env` và thư mục/file secrets đã được `.gitignore`, nhưng Spring Boot không tự nạp `.env`; hãy khai báo biến trong shell, IDE Run Configuration hoặc secret manager của nền tảng triển khai.

Không đưa credential thật vào `application.properties`, README, Postman collection, command history hoặc Git.

## Chạy local

Nếu tài khoản MySQL có quyền tạo database, JDBC URL mặc định sẽ tự tạo `holiday_planner`. Nếu không, chạy trước:

```sql
SOURCE database/holiday_planner.sql;
```

Sau đó:

```bash
mvn clean test
mvn spring-boot:run
```

Mở `http://localhost:8080`.

Đóng gói executable JAR:

```bash
mvn clean package
java -jar target/holiday-planner-1.0.0.jar
```

Trong IntelliJ IDEA, mở đúng thư mục `HolidayPlanner` chứa `pom.xml`, chọn JDK 17+, reload Maven, thêm các biến môi trường vào Run Configuration rồi chạy `HolidayPlannerApplication`.

## Local và production

Local mặc định:

- `spring.jpa.hibernate.ddl-auto=update` để tạo/cập nhật schema phát triển.
- `APP_SEED_DEMO_DATA=true` để có dữ liệu trình diễn.
- Thiếu Cloudinary không làm ứng dụng dừng khởi động.

Production khuyến nghị:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:APP_SEED_DEMO_DATA="false"
```

Đồng thời phải cấp datasource, `REMEMBER_ME_KEY` và Cloudinary qua secret manager. Sau khi áp dụng migration có kiểm soát, nên chạy production với `--spring.jpa.hibernate.ddl-auto=validate` thay vì dựa vào auto-update.

## Cloudinary, avatar và ảnh điểm đến

`ImageStorageService` tách nghiệp vụ khỏi nhà cung cấp lưu trữ. Khi đủ ba biến Cloudinary, ứng dụng dùng Cloudinary; khi thiếu cấu hình, fallback implementation cho phép ứng dụng khởi động và các chức năng không upload vẫn hoạt động.

Quy tắc upload:

- Chấp nhận `.jpg`, `.jpeg`, `.png`, `.webp`.
- MIME type, phần mở rộng và file signature phải khớp.
- Kích thước tối đa 5 MB.
- Avatar được crop theo khuôn mặt; ảnh điểm đến được giới hạn kích thước phù hợp hiển thị.
- Upload ảnh mới hoàn tất trước khi thay reference cũ.
- Nếu upload hoặc lưu database thất bại, reference ảnh cũ được giữ lại; ảnh mới dở dang được cleanup.
- `publicId` chỉ lưu nội bộ để xóa đúng tài nguyên thuộc folder ứng dụng và không xuất hiện trong API response.

Nếu Cloudinary chưa cấu hình:

- Avatar/ảnh điểm đến hiện có và ảnh fallback local vẫn hiển thị.
- ADMIN vẫn có thể nhập URL HTTP(S) hoặc đường dẫn local bắt đầu bằng `/` cho ảnh điểm đến.
- API upload trả `503 Service Unavailable` với JSON error rõ ràng; dữ liệu ảnh cũ không bị mất.
- Xóa avatar đưa giao diện về avatar mặc định; xóa ảnh điểm đến đưa về `/images/destination-fallback.svg`.

Endpoint media:

- `POST /api/profile/avatar` — multipart field `image`.
- `DELETE /api/profile/avatar`.
- `POST /api/admin/destinations/{id}/image` — multipart field `image`.
- `DELETE /api/admin/destinations/{id}/image`.

## Demo data

Seed dùng natural key và kiểm tra dữ liệu đã tồn tại, vì vậy restart không tạo trùng. `APP_SEED_DEMO_DATA=true` tạo:

- 3 tài khoản demo.
- 5 điểm đến: Hà Nội, Đà Nẵng, Đà Lạt, Phú Quốc và Tokyo.
- 5 chuyến đi hoàn chỉnh của `user@holidayplanner.vn`, có trạng thái `COMPLETED`, `ONGOING`, `PLANNED`, `DRAFT`, `CANCELLED`, ngày tương đối với ngày chạy và dữ liệu vượt ngân sách để trình diễn cảnh báo.
- Mỗi chuyến có ít nhất 3 itinerary, 3 expense, 4 checklist và 2 booking.

Tài khoản local:

| Vai trò | Email | Mật khẩu |
|---|---|---|
| ADMIN | `admin@holidayplanner.vn` | `admin123` |
| USER demo đầy đủ | `user@holidayplanner.vn` | `user1234` |
| USER kiểm tra ownership | `traveler@holidayplanner.vn` | `traveler123` |

Đây chỉ là credential demo local. Luôn tắt seed và không dùng các mật khẩu này ở production.

## Schema và migration

Các bảng chính: `users`, `destinations`, `trip_plans`, `itinerary_items`, `expenses`, `checklist_items`, `booking_notes`.

Media bổ sung các cột:

- `users.avatar_url VARCHAR(700)`.
- `users.avatar_public_id VARCHAR(255)`.
- `destinations.image_public_id VARCHAR(255)`.

Database mới ở local có thể để Hibernate `ddl-auto=update` tạo cột. Khi nâng cấp database MySQL đã có dữ liệu:

1. Sao lưu database.
2. Kiểm tra ba cột trên chưa tồn tại.
3. Chạy đúng một lần `database/20260716_add_media_columns.sql`.
4. Khởi động với `ddl-auto=validate` để xác nhận schema.

Migration này không xóa dữ liệu, nhưng không được chạy lặp lại trên schema đã có cột.

## REST API

REST API dùng HTTP Basic Auth, ngoại trừ `POST /api/auth/register`. CSRF chỉ được bỏ qua cho `/api/**`; form web vẫn bật CSRF.

Nhóm endpoint:

- `POST /api/auth/register`.
- `GET|PUT /api/profile`, `PUT /api/profile/password` (có alias `POST`) và avatar endpoints.
- `GET|POST /api/trips`, `GET|PUT|DELETE /api/trips/{id}`.
- CRUD `/api/trips/{id}/itinerary`.
- CRUD `/api/trips/{id}/expenses`.
- CRUD checklist và desired state `PATCH /api/trips/{id}/checklist/{itemId}/state`.
- CRUD `/api/trips/{id}/bookings`.
- `GET /api/dashboard`.
- Destination CRUD/search/filter/page/upload và desired state `PATCH /api/admin/destinations/{id}/status`.
- User search/filter/page và desired state `PATCH /api/admin/users/{id}/status`.

Ví dụ error contract:

```json
{
  "timestamp": "2026-07-16T09:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Dữ liệu không hợp lệ",
  "fieldErrors": {
    "email": "Email không hợp lệ"
  }
}
```

Các status thường dùng: `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `413`, `502`, `503`.

## Traceability SRS UC01–UC10

| Use Case | Chức năng | Thành phần chính | Web/API |
|---|---|---|---|
| UC01 | Đăng ký tài khoản | `UserAccount`, `RegisterRequest`, `UserService` | `/register`, `POST /api/auth/register` |
| UC02 | Đăng nhập/đăng xuất | `SecurityConfig`, `CustomUserDetailsService` | `/login`, `/logout`, HTTP Basic `/api/**` |
| UC03 | Quản lý kế hoạch | `TripPlan`, `TripPlanService` | `/trips`, `/api/trips` |
| UC04 | Quản lý lịch trình | `ItineraryItem`, `TripDetailService` | Trip detail, `/api/trips/{id}/itinerary` |
| UC05 | Ngân sách/chi phí | `Expense`, `TripDetailService` | Trip detail, `/api/trips/{id}/expenses` |
| UC06 | Checklist chuẩn bị | `ChecklistItem`, `TripDetailService` | Trip detail, `/api/trips/{id}/checklist` |
| UC07 | Dashboard tổng quan | `DashboardService` | `/dashboard`, `GET /api/dashboard` |
| UC08 | Quản lý điểm đến | `Destination`, `DestinationService` | `/admin/destinations`, `/api/admin/destinations` |
| UC09 | Ghi chú đặt dịch vụ | `BookingNote`, `TripDetailService` | Trip detail, `/api/trips/{id}/bookings` |
| UC10 | Quản lý người dùng | `UserAccount`, `UserService` | `/admin/users`, `/api/admin/users` |

## Postman

Import `postman/HolidayPlanner.postman_collection.json` và chạy sáu folder theo thứ tự; xem hướng dẫn chi tiết tại `docs/POSTMAN_RUN_GUIDE.md`. Collection hiện có:

- 61 requests.
- 93 `pm.test` assertions; mọi request đều có ít nhất một assertion.
- 25 collection variables.
- Coverage: đăng ký, profile, đổi mật khẩu, avatar, trip và toàn bộ child CRUD, desired-state idempotency, USER/ADMIN dashboard, destination CRUD/search/page/media, user management, cùng lỗi 400/401/403/404/cross-owner/locked.

Mặc định:

- `baseUrl=http://localhost:8080`.
- `runMediaUploads=false`; bốn request multipart được skip để collection vẫn chạy khi Cloudinary hoặc file local chưa sẵn sàng.
- Collection tự tạo email USER duy nhất, lưu các ID phát sinh và đổi mật khẩu USER đó trong quá trình test.

Để chạy media live:

1. Cấu hình Cloudinary bằng environment variables trước khi khởi động ứng dụng.
2. Đặt `runMediaUploads=true`.
3. Đặt `avatarImagePath` và `destinationImagePath` thành đường dẫn tuyệt đối tới ảnh JPG/JPEG/PNG/WEBP hợp lệ, không quá 5 MB.
4. Đặt `invalidImagePath` thành đường dẫn tuyệt đối tới file không phải ảnh để kiểm tra validation 400.

Không lưu file ảnh nhạy cảm hoặc credential thật/production vào collection; các mật khẩu có sẵn chỉ khớp demo seed local. Chạy bằng Newman (nếu đã cài):

```bash
newman run postman/HolidayPlanner.postman_collection.json
```

Collection dọn các child record và xóa mềm trip vừa tạo. USER động và destination test đã deactivate được giữ lại làm audit data; dùng database sạch nếu cần trạng thái demo ban đầu hoàn toàn nguyên vẹn.

## Automated tests và smoke test

Test dùng H2 với chế độ tương thích MySQL:

```bash
mvn clean test
mvn clean package
```

Phạm vi cần giữ xanh gồm:

- Validation boundary và password confirmation.
- Email normalization/duplicate, BCrypt, profile và đổi mật khẩu.
- Ownership/cross-trip/cross-owner và tài khoản bị khóa.
- CRUD TripPlan và bốn loại child record; desired-state idempotency.
- Dashboard USER/ADMIN và phân trang/tìm kiếm.
- Image validation, storage fallback, replacement/cleanup bằng mock.
- Demo seed idempotency.
- MockMvc security và API error contract.

MySQL runtime smoke:

1. Khởi động MySQL 8.x và tạo database hoặc cấp quyền `createDatabaseIfNotExist`.
2. Export `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
3. Chạy `mvn spring-boot:run`.
4. Kiểm tra log có `Started HolidayPlannerApplication`, mở `/login`, đăng nhập USER/ADMIN và chạy Postman.
5. Restart ứng dụng và xác nhận số bản ghi demo không tăng.

Cloudinary smoke nên xác minh upload, replace, delete cho cả avatar và destination, đồng thời thử file sai định dạng và file lớn hơn 5 MB. Không bật request media live nếu chưa có file test phù hợp.

## Kiểm chứng cuối và CI

Chạy build/test chuẩn trên Windows từ thư mục này:

```powershell
.\scripts\final-verify.ps1
```

Script lưu raw log tạm trong `target/verification/` nên không bị commit. Có thể thêm Postman khi ứng dụng đang chạy và Newman đã cài:

```powershell
.\scripts\final-verify.ps1 -RunPostman
```

Browser smoke dùng Chrome DevTools Protocol; xem `docs/screenshots/README.md`. Các tài liệu bằng chứng:

- `docs/FINAL_VERIFICATION_REPORT.md`.
- `docs/POSTMAN_EXECUTION_REPORT.md`.
- `docs/RELEASE_CHECKLIST.md`.
- `docs/screenshots/browser-smoke.json`.

Repository có workflow `.github/workflows/maven-verify.yml` chạy `mvn clean test` và `mvn package` bằng Temurin Java 17 khi push branch `main`, `fix/**` hoặc mở pull request. Chỉ coi bản nộp sẵn sàng khi local build hoặc workflow này xanh và Postman core không có assertion failed.

## Bảo mật và phân quyền

- `/login`, `/register`, static assets và `POST /api/auth/register` là public.
- `/dashboard`, `/trips`, `/profile`, `/api/profile`, `/api/trips`, `/api/dashboard` yêu cầu đăng nhập.
- `/admin/**` và `/api/admin/**` chỉ dành cho `ADMIN`.
- USER chỉ đọc/sửa/xóa trip và child record của chính mình.
- ADMIN không thể tự khóa tài khoản đang đăng nhập.
- Password, Cloudinary secret và internal media `publicId` không được trả về API.

## Troubleshooting

### MySQL không kết nối

- Kiểm tra service MySQL, host/port và `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.
- Nếu user không có quyền tạo database, chạy `database/holiday_planner.sql` trước.
- Kiểm tra timezone JDBC là `Asia/Ho_Chi_Minh`.

### Upload trả 503

Ứng dụng chưa nhận đủ `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Đặt biến trong đúng process chạy Java rồi restart; không chép secret vào source.

### Upload trả 400 hoặc 413

- `400`: kiểm tra extension, MIME type, signature và multipart field phải là `image`.
- `413`: ảnh vượt quá 5 MB.

### Cổng 8080 đang bận

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

Dừng tiến trình cũ hoặc chạy với `--server.port=8081`, đồng thời đổi `baseUrl` trong Postman.

## Giới hạn đã biết

- Upload avatar/ảnh điểm đến cần Cloudinary và file local hợp lệ; khi thiếu cấu hình, phần còn lại của ứng dụng vẫn chạy nhưng upload trả JSON `503`.
- Collection Postman là luồng có trạng thái và phải chạy tuần tự từ folder `01` đến `06`; bốn request multipart được skip mặc định.
- `ddl-auto=update`, demo seed và mật khẩu database mặc định chỉ dành cho local. Production cần migration có kiểm soát, secret manager, HTTPS và `COOKIE_SECURE=true`.
- README mô tả lệnh và phạm vi kiểm thử, không thay thế bằng chứng chạy trên máy đích; ghi nhận kết quả thực tế trong `docs/RELEASE_CHECKLIST.md`.

## Tài liệu nguồn được bảo toàn

Ba tài liệu yêu cầu nằm tại `../documents` và không được ứng dụng chỉnh sửa. SHA-256 đã xác minh:

- `HolidayPlanner_SRS_FINAL.docx`: `A9559BF25B91BB286C6E96FB490A96039B33F4BC2D575C0AA654CBF59DB6A90F`.
- `HolidayPlanner_Technical_FINAL.xlsx`: `302BF7C8CD3C753ACC58CFFE0B1F094A25CAA37F8C77303D249D5191DBF97D5C`.
- `HolidayPlanner_Calendar_FINAL.xlsx`: `D3C8B4DEB9E1A9E244EAEBE3DD3435603DBD78C0DD93B334E8D5582055CC0C43`.
