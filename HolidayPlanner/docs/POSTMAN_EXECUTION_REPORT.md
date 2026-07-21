# Postman execution report

## 1. Collection audit

File được kiểm tra: `postman/HolidayPlanner.postman_collection.json`.

| Chỉ số | Kết quả |
|---|---:|
| Folder cấp cao | 6 |
| Request | 61 |
| `pm.test(...)` assertions | 93 |
| Collection variables | 25 |
| Request không có test script | 0 |
| JSON syntax | PASS |

Collection sử dụng ngày và ID động, không giả định `destinationId=1`, đồng thời có positive/negative cases cho UC01–UC10, security, ownership, validation, conflict và admin.

## 2. Trạng thái chạy live

Bản source đóng gói không chứa raw Newman/Postman Runner output. Vì vậy báo cáo này **không giả định** 61 request đã được chạy live chỉ dựa trên việc collection hợp lệ.

Trạng thái hiện tại:

- Static structure/schema audit: **PASS**.
- Assertions coverage: **PASS**.
- Secret scan: **PASS**; không có Cloudinary credential thật.
- Live core collection against MySQL: **CẦN CHẠY LẠI TRÊN MÁY NỘP/BẢO VỆ**.
- Live media upload: optional; mặc định `runMediaUploads=false`.

## 3. Lệnh chạy cuối

Khi ứng dụng đang chạy tại `http://localhost:8080`:

```powershell
newman run postman/HolidayPlanner.postman_collection.json --reporters cli
```

Hoặc dùng script tổng hợp:

```powershell
.\scripts\final-verify.ps1 -RunPostman
```

Kết quả đạt:

- Không có request failure ngoài request negative vốn đã assert status mong đợi.
- Không có assertion failed.
- Bốn request multipart có thể bị skip khi `runMediaUploads=false`; đây là skip có chủ đích.

## 4. Media live

Chỉ bật `runMediaUploads=true` khi ứng dụng đã nhận đủ Cloudinary environment variables và Postman có đường dẫn file test hợp lệ. Không đưa secret vào collection, command history hoặc Git.
