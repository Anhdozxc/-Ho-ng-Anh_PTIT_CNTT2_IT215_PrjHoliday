# Browser smoke và ảnh kiểm chứng giao diện

Thư mục này chứa ảnh do `scripts/browser-smoke.mjs` chụp từ ứng dụng chạy thật bằng Chrome DevTools Protocol. Script không dùng dữ liệu production; tài khoản được truyền qua biến môi trường.

## Lần chạy đang được lưu

`browser-smoke.json` ghi nhận lần chạy ngày **17/07/2026** với:

- Base URL: `http://127.0.0.1:8080`.
- 45 kiểm tra đạt.
- 0 kiểm tra thất bại.
- 0 JavaScript exception.
- 0 resource ngoài document bị lỗi.
- Responsive đã kiểm tra tại 375, 768, 1024, 1440 và 1920 px.

Ảnh hiện có bao phủ Login, Dashboard, Trip list, năm trạng thái tab của Trip detail, Profile, Admin Destination, Admin User và trang 404. Tên ảnh cũ được giữ để bảo toàn bằng chứng của lần chạy trước.

## Script smoke phiên bản hiện tại

Script hiện tại đã được tăng cường để:

- Chụp cả Login và Register ở desktop/mobile.
- Chụp Dashboard USER và ADMIN riêng.
- Chụp Trip list desktop/mobile.
- Cuộn qua toàn bộ trang trước khi chụp để kích hoạt `IntersectionObserver`, lazy image và tránh phần nội dung dưới fold bị trắng trong full-page screenshot.
- Kiểm tra nội dung reveal đã hiển thị hết.
- Kiểm tra ảnh không vỡ.
- Kiểm tra `/favicon.ico` public và trả kiểu nội dung ảnh.
- Chụp 403 và 404.
- Ghi lỗi console và network vào JSON.

Vì script đã thay đổi sau lần chụp đang lưu, hãy chạy lại trước buổi bảo vệ để sinh bộ ảnh tên mới và cập nhật `browser-smoke.json`.

## Cách chạy lại

1. Khởi động MySQL và ứng dụng tại `http://127.0.0.1:8080`.
2. Mở Chrome với remote debugging, ví dụ trên Windows:

```powershell
& "$env:ProgramFiles\Google\Chrome\Application\chrome.exe" `
  --remote-debugging-port=9222 `
  --user-data-dir="$env:TEMP\holiday-planner-chrome"
```

3. Khai báo tài khoản demo trong đúng terminal chạy Node:

```powershell
$env:HP_USER_EMAIL="user@holidayplanner.vn"
$env:HP_USER_PASSWORD="user1234"
$env:HP_ADMIN_EMAIL="admin@holidayplanner.vn"
$env:HP_ADMIN_PASSWORD="admin123"
$env:HP_BASE_URL="http://127.0.0.1:8080"
$env:HP_CDP_URL="http://127.0.0.1:9222"
node scripts/browser-smoke.mjs
```

Không commit credential khác với tài khoản demo local. Không chụp màn hình chứa secret, password hoặc dữ liệu cá nhân thật.
