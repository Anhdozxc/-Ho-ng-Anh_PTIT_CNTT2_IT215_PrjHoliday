# Kịch bản kiểm thử Holiday Planner

Tài liệu này bám theo SRS FINAL, AC-01..AC-07 và contract REST hiện tại. Automated tests dùng profile `test` (H2 `MODE=MySQL`, Cloudinary tắt); Postman dùng MySQL và HTTP Basic.

## Authentication, profile và security

| ID | Mức | Thao tác | Kết quả mong đợi |
| --- | --- | --- | --- |
| AUTH-01 | Auto/API | `POST /api/auth/register` với email mới, password và confirmation hợp lệ | `201`; email chuẩn hóa lowercase; role `USER`, status `ACTIVE`; response không có password/hash |
| AUTH-02 | Auto/API | Đăng ký email đã tồn tại, khác hoa thường | `409` và JSON error contract |
| AUTH-03 | Auto/API | Email sai, password ngắn hoặc confirmation lệch | `400`, `fieldErrors` đúng field; không tạo user |
| AUTH-04 | Auto/Web | Login đúng/sai và user `LOCKED` | Đúng thì vào dashboard; sai/locked thì login thất bại; password lưu BCrypt |
| AUTH-05 | Auto/API | Gọi `/api/trips` không auth | `401` JSON, không trả HTML login |
| AUTH-06 | Auto/API | USER gọi `/api/admin/users` | `403` JSON |
| AUTH-07 | Auto/Web | POST form web không có CSRF | Bị từ chối `403`; form có token hoạt động |
| AUTH-08 | Manual/Web | Chọn “Ghi nhớ đăng nhập”, đóng/mở browser | Cookie remember-me `HttpOnly`, `SameSite=Lax`; đăng nhập được duy trì trong thời hạn |
| PROF-01 | Auto/API | `GET /api/profile`, `PUT /api/profile` | `200`; xem/cập nhật họ tên; không lộ hash/publicId media |
| PROF-02 | Auto/API | `PUT /api/profile/password` với current password đúng/sai | Đúng: `204`, password mới BCrypt và đăng nhập được; sai: `400` |
| PROF-03 | Auto/API | Avatar rỗng/sai MIME/sai magic bytes/quá 5 MB | `400` hoặc `413`; avatar cũ không mất |
| PROF-04 | Auto/API | Cloudinary unavailable; upload/replace/delete avatar | App vẫn start; upload trả `503`; replacement rollback giữ ảnh cũ; delete đưa về fallback |

## TripPlan và ownership

| ID | Mức | Thao tác | Kết quả mong đợi |
| --- | --- | --- | --- |
| TRIP-01 | Auto/API | `POST /api/trips` dữ liệu hợp lệ | `201`; owner là user hiện tại |
| TRIP-02 | Auto/API | Ngày kết thúc trước ngày bắt đầu, people `< 1`, budget âm | `400` có `fieldErrors`/message kiểm soát |
| TRIP-03 | Auto/API | Search title/destination, filter status, sort, page | Chỉ dữ liệu thuộc scope; thứ tự và page metadata đúng |
| TRIP-04 | Auto/API | `PUT /api/trips/{id}` và `DELETE /api/trips/{id}` | `200` khi sửa; `204` khi xóa mềm; sau xóa trả `404` |
| TRIP-05 | Auto/API | User B đọc/sửa/xóa trip hiện hữu của User A | `403`; ID không tồn tại trả `404` |
| TRIP-06 | Auto/Service | Ma trận 5×5 trạng thái | Cho phép cùng trạng thái và 6 cạnh SRS; mọi cạnh khác ném conflict/HTTP `409` |
| TRIP-07 | Auto/Web | Sửa trip đang dùng destination inactive | Option hiện tại vẫn có nhãn “Đã ẩn”; giữ nguyên được; không chọn được inactive khác |

## Itinerary, expense, checklist và booking

| ID | Mức | Thao tác | Kết quả mong đợi |
| --- | --- | --- | --- |
| ITIN-01 | Auto/API | POST/GET/PUT/DELETE itinerary | `201/200/200/204`; sort `dayNo`, `fromTime` |
| ITIN-02 | Auto/API | `dayNo` ngoài trip, thiếu một đầu giờ, `toTime <= fromTime` | `400` |
| ITIN-03 | Auto/API | Create/update chồng giờ cùng ngày | `409`; chạm biên giờ không bị coi là overlap |
| ITIN-04 | Auto/API | Child ID thuộc trip khác/cross-owner và ID thiếu | Cross-owner `403`; ID thiếu `404` |
| EXP-01 | Auto/API | POST/GET/PUT/DELETE expense với `0.00` và giá trị hợp lệ | `201/200/200/204`; dùng `BigDecimal` scale 2 |
| EXP-02 | Auto/API | Amount âm hoặc quá 2 số thập phân | `400`, không phát sinh `ArithmeticException` |
| EXP-03 | Auto/API | `spentDate` null hoặc trong khoảng trip; ngoài khoảng | Null/inclusive range được chấp nhận; ngoài range trả `400` |
| EXP-04 | Auto/API | `GET /api/trips/{id}?expenseCategory=FOOD` | Mảng `expenses` chỉ chứa category yêu cầu; total vẫn là tổng trip |
| CHECK-01 | Auto/API | POST checklist có title/category/dueDate | `201`; `done=false` bất kể payload thừa |
| CHECK-02 | Auto/API | PUT metadata checklist | `200`; không làm đổi `done` |
| CHECK-03 | Auto/API | PATCH `/checklist/{itemId}/state` hai lần với `done=true` | Cả hai `200`; trạng thái idempotent và vẫn `true` |
| CHECK-04 | Auto/API | DELETE checklist/cross-owner/missing ID | Owner `204`; cross-owner `403`; missing `404` |
| BOOK-01 | Auto/API | POST/GET/PUT/DELETE booking; bookingCode null | `201/200/200/204`; provider bắt buộc, code optional |
| BOOK-02 | Auto/API | Price âm/quá scale; cross-owner | Validation `400`; cross-owner `403` |

## Dashboard và Admin

| ID | Mức | Thao tác | Kết quả mong đợi |
| --- | --- | --- | --- |
| DASH-01 | Auto/API | USER gọi `GET /api/dashboard` | Chỉ số và tổng chi/checklist chỉ thuộc owner, không tính trip deleted |
| DASH-02 | Auto/API | ADMIN gọi dashboard | Tổng hợp toàn hệ thống, `adminView=true` |
| DASH-03 | Auto/Service | Upcoming có DRAFT/PLANNED/ONGOING/CANCELLED/COMPLETED/deleted | Chỉ trạng thái hợp lệ; tăng dần startDate; tối đa 5 |
| DEST-01 | Auto/API | Admin create/update/search/filter/page destination | `201/200/200`; metadata và page đúng |
| DEST-02 | Auto/API | PATCH `/api/admin/destinations/{id}/status` false/true | Hide/show idempotent; trip cũ không mất dữ liệu |
| DEST-03 | Auto/API | Upload/replace/delete image; URL sai | Validate đúng; không lộ `publicId`; rollback giữ ảnh cũ |
| USER-01 | Auto/API | Search user theo tên/email, filter role/status, page | Kết quả và metadata đúng; không có password |
| USER-02 | Auto/API | PATCH `/api/admin/users/{id}/status` LOCKED/ACTIVE | Desired state idempotent; locked user không login được |
| USER-03 | Auto/Service | Admin tự khóa hoặc khóa active admin cuối cùng | Bị từ chối; trạng thái DB không đổi |

## Frontend và manual smoke

| ID | Viewport | Kiểm tra | Kết quả mong đợi |
| --- | ---: | --- | --- |
| UI-01 | 375/768 | Login/register/navbar/forms | Không overflow; menu/label/error/focus hoạt động; không lộ demo password trong HTML |
| UI-02 | 1024/1440/1920 | Dashboard, trip list/detail | Design system light nhất quán; ảnh không méo/vỡ; empty state và cards cân đối |
| UI-03 | Tất cả | Trip detail 4 tab và CRUD | Giữ đúng tab sau thao tác; modal confirm có focus trap/Escape; không dùng `window.confirm()` |
| UI-04 | Tất cả | Admin tables/mobile cards | Search/filter/action đúng role; mobile không tràn ngang |
| UI-05 | Tất cả | Keyboard/reduced motion | Focus rõ, icon có accessible name, heading hợp lý; animation giảm khi `prefers-reduced-motion` |
| UI-06 | Browser | Console/network | Không JavaScript error, không request asset 404, CSRF web không bị phá |
| ERR-01 | Browser/API | 403/404/500 | Web có trang thân thiện, không stack trace; API có JSON error contract |

## Lệnh regression

```bash
mvn clean test
mvn clean package
newman run postman/HolidayPlanner.postman_collection.json
```

Tiêu chí kết thúc: không disable/skip automated test; failures/errors bằng 0; Postman core không có assertion failed; mọi gap manual hoặc môi trường phải được ghi trung thực trong `RELEASE_CHECKLIST.md`.
