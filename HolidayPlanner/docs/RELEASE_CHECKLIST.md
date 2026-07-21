# Release checklist

## Trạng thái source hiện tại

- [x] Repository có `.gitignore` cho `target/`, `.class`, `.idea/`, `.tools/`, `.env` và file secret local.
- [x] Source thật nằm trong `src/main`, `src/test`, `pom.xml` và `resources`; không phụ thuộc class stale trong `target/`.
- [x] Backend triển khai UC01–UC10, Profile/Avatar, optional Cloudinary, RBAC, ownership và REST error contract.
- [x] Frontend light luxury, responsive, animation, modal, toast, field validation và error pages đã được triển khai.
- [x] Postman collection có 61 request, 93 assertion, 25 variable và không có request thiếu test.
- [x] Browser smoke evidence đang lưu: 45 pass, 0 fail, 0 console error, 0 failed non-document resource.
- [x] Real `favicon.ico` đã được thêm và `/favicon.ico` public với `image/x-icon`.
- [x] GitHub Actions Java 17 Maven verify đã được thêm tại `.github/workflows/maven-verify.yml`.

## Bằng chứng build đã có

Phiên Codex/Cursor trước báo cáo:

- `mvn clean test`: 125 tests, 0 failure, 0 error, 0 skipped.
- `mvn clean package`: BUILD SUCCESS, executable JAR được tạo.
- MySQL 8.0.45 runtime smoke: app khởi động, login/web/API và dashboard chính hoạt động.

Raw Maven output không được giữ trong clean ZIP. Sau bằng chứng trên, source có thêm một test favicon và cải thiện browser smoke, nên phải chạy lại một lần để lấy summary cuối chính xác.

## Việc bắt buộc trước khi nộp

- [ ] Mở đúng thư mục `HolidayPlanner` trong IntelliJ/Cursor và chọn JDK 17 hoặc xác nhận GitHub Actions Java 17 xanh.
- [ ] Chạy `mvn clean test`; ghi Tests/Failures/Errors/Skipped thực tế vào `FINAL_VERIFICATION_REPORT.md`.
- [ ] Chạy `mvn clean package`; xác nhận BUILD SUCCESS.
- [ ] Khởi động MySQL 8.x và chạy JAR/application với datasource của máy.
- [ ] Restart ứng dụng một lần để xác nhận demo seed không tạo bản ghi trùng.
- [ ] Chạy core Postman collection theo thứ tự folder; xác nhận 0 assertion failed.
- [ ] Chạy lại `scripts/browser-smoke.mjs` sau thay đổi favicon/capture; xác nhận JSON cuối có `failed: 0`.
- [ ] Kiểm tra `git status` sạch và branch đã push.

## Lệnh nhanh trên Windows

Từ thư mục `HolidayPlanner`:

```powershell
.\scripts\final-verify.ps1
```

Khi ứng dụng đang chạy và Newman đã cài:

```powershell
.\scripts\final-verify.ps1 -RunPostman
```

Browser smoke yêu cầu Chrome remote debugging và bốn biến credential demo; xem `docs/screenshots/README.md`.

## Kiểm tra trước buổi bảo vệ

- [ ] Login USER: `user@holidayplanner.vn / user1234`.
- [ ] Login ADMIN: `admin@holidayplanner.vn / admin123`.
- [ ] USER tạo/sửa/xóa trip và CRUD đủ bốn tab.
- [ ] USER không mở được trang/API admin.
- [ ] ADMIN search/filter destination và user, hide/show, lock/unlock.
- [ ] Profile cập nhật họ tên, đổi mật khẩu, avatar fallback khi Cloudinary chưa cấu hình.
- [ ] Không có asset 404, JavaScript error hoặc overflow ngang ở 375/768/1024/1440/1920.
- [ ] Không có `.env`, Cloudinary secret, raw Authorization hoặc password thật trong Git.
- [ ] Đóng process đang chiếm cổng 8080 sau khi kiểm thử.

## Quy tắc kết luận

Chỉ ghi `READY TO SUBMIT` khi các checkbox bắt buộc phía trên đã hoàn tất bằng kết quả chạy thật. README và tài liệu không thay thế build/Postman evidence.
