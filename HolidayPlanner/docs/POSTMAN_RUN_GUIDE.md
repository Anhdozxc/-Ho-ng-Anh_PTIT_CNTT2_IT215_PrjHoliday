# Hướng dẫn chạy Postman

Collection: `../postman/HolidayPlanner.postman_collection.json`.

## Điều kiện trước khi chạy

1. MySQL 8 đang hoạt động và ứng dụng đã khởi động tại `http://localhost:8080`.
2. Demo seed đang bật để có tài khoản USER, ADMIN và ít nhất một destination active.
3. Không đổi thứ tự sáu folder: collection tạo dữ liệu động rồi dùng lại ID ở các bước sau.
4. Không nhập Cloudinary secret hoặc credential thật/production vào collection hay environment của Postman được commit lên Git; mật khẩu trong collection chỉ dành cho demo seed local.

Ứng dụng đọc datasource từ `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Mặc định local lần lượt là JDBC URL tới `holiday_planner`, `root`, `123456`; nên khai báo lại cả ba biến khi cấu hình MySQL khác hoặc khi chạy ngoài môi trường local.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/holiday_planner?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME="<mysql-user>"
$env:DB_PASSWORD="<mysql-password>"
```

## Import và chạy

1. Trong Postman, chọn **Import** và mở `HolidayPlanner.postman_collection.json`.
2. Mở collection, kiểm tra `baseUrl` và credential demo local nếu đã thay đổi cấu hình seed.
3. Chọn **Run collection**.
4. Giữ nguyên thứ tự folder từ `01` đến `06`; không chạy song song.
5. Kết quả đạt khi mọi request không bị skip đều xanh và không có assertion failed.

Collection tự:

- tạo email USER duy nhất theo timestamp;
- tạo ngày đi từ ngày chạy nên không phụ thuộc một mốc ngày cố định;
- lấy `destinationId` từ API thay vì giả định ID bằng `1`;
- lưu động `userId`, `tripId`, `itineraryId`, `expenseId`, `checklistId`, `bookingId` và `adminDestinationId`;
- đổi mật khẩu USER động, kiểm tra mật khẩu cũ bị từ chối;
- xóa child record và xóa mềm trip ở folder cleanup.

Collection có 61 request, 93 `pm.test` assertion và 25 collection variable. Mỗi request có ít nhất một assertion; các request media bị skip mặc định vẫn giữ assertion để dùng khi bật kiểm thử upload.

## Phạm vi folder

| Folder | Phạm vi |
| --- | --- |
| 01 | Đăng ký, email trùng, validation, API 401 JSON |
| 02 | Hồ sơ, đổi mật khẩu, avatar optional |
| 03 | Dashboard USER, trip CRUD, bốn nhóm child CRUD, lọc expense, checklist desired-state |
| 04 | 403 cross-owner/admin, ngày sai 400, overlap 409, amount âm 400, transition sai 409 |
| 05 | Dashboard ADMIN, destination create/update/hide/show/media, user search/lock/unlock, 404 |
| 06 | Xóa child, xóa mềm trip và xác nhận 404 |

## Chạy media optional

Mặc định `runMediaUploads=false`; các request multipart được skip có chủ đích để collection chạy được khi chưa có Cloudinary hoặc file local.

Để kiểm thử upload thật:

1. Cấu hình đủ ba credential `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong process chạy Spring Boot; `CLOUDINARY_FOLDER` là tùy chọn.
2. Đặt `runMediaUploads=true` trong collection variables.
3. Đặt `avatarImagePath` và `destinationImagePath` thành đường dẫn tuyệt đối đến ảnh JPEG/PNG/WEBP hợp lệ, không quá 5 MB.
4. Đặt `invalidImagePath` thành đường dẫn tuyệt đối đến một file không phải ảnh.
5. Chạy lại toàn collection hoặc riêng các request media sau khi dữ liệu tiền điều kiện đã tồn tại.

Nếu Cloudinary chưa cấu hình, gọi upload trực tiếp phải trả `503` có JSON error; đây là fallback mong đợi, không phải lỗi khởi động ứng dụng.

## Newman

Nếu đã cài Newman:

```bash
newman run postman/HolidayPlanner.postman_collection.json --reporters cli
```

Collection dùng collection variables nên không cần file environment riêng. Có thể truyền URL khác bằng `--env-var baseUrl=http://localhost:8081`.

Không truyền Cloudinary secret bằng tham số dòng lệnh Newman vì có thể bị lưu trong shell history; hãy đặt secret ở environment của process chạy ứng dụng hoặc secret manager.

## Đọc lỗi

- `401`: kiểm tra ứng dụng đã seed tài khoản và password collection khớp.
- `403`: đúng với USER gọi admin hoặc cross-owner; nếu xuất hiện ở happy path, kiểm tra credential/ID động.
- `404`: kiểm tra có chạy đủ folder trước đó và không chạy cleanup sớm.
- `409`: đúng cho email trùng, lịch trình chồng giờ và transition trạng thái sai.
- `503` ở upload: thiếu Cloudinary; giữ `runMediaUploads=false` nếu chỉ chạy regression core.

Sau khi collection chạy, database còn USER động và destination audit (đã ẩn ở bước cleanup); dùng database sạch hoặc tắt seed tùy mục đích kiểm thử tiếp theo.
