# Release checklist

## Trước khi nộp/bảo vệ

- [ ] MySQL đang chạy; database `holiday_planner` tạo được.
- [ ] Kiểm tra `root / 123456` hoặc sửa `application.properties` theo máy.
- [ ] IntelliJ đã sync Maven thành công với Java 17.
- [ ] Chạy `mvn test` hoặc Run All Tests trong IntelliJ.
- [ ] Chạy ứng dụng và smoke test UC01-UC10.
- [ ] Kiểm tra đăng nhập USER/ADMIN và phân quyền URL/API.
- [ ] Import Postman collection, chạy các request chính.
- [ ] Tạo sẵn một chuyến đi demo có itinerary, expense, checklist, booking.
- [ ] Đóng các ứng dụng dùng cổng 8080 trước khi demo.

## Lưu ý môi trường đóng gói

Mã nguồn đã được rà soát cấu trúc, XML/JSON, template HTML và tính nhất quán với SRS/Technical. Môi trường tạo gói không có executable Maven, nên bước compile/test cuối phải được chạy trên IntelliJ/Maven của máy bảo vệ.
