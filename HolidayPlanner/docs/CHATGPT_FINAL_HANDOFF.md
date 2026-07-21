# ChatGPT final handoff

Gói này tiếp tục từ commit `9ab1d152fb9f4e90cc2501f2969317c7a5fcc80f` trên branch `fix/srs-compliance-and-stability`.

## Thay đổi trong handoff này

- Sửa `.gitignore` bị chèn nhầm nguyên lệnh PowerShell; giữ rule `.tools/` sạch.
- Thêm workflow GitHub Actions Java 17 Maven verify.
- Thêm real multi-size `favicon.ico` và sửa controller trả `image/x-icon`.
- Thêm MVC test cho favicon public.
- Cải thiện browser smoke:
  - kiểm tra favicon;
  - cuộn toàn trang trước full screenshot;
  - kiểm tra reveal content;
  - kiểm tra ảnh vỡ;
  - giữ console/network assertions.
- Thêm `scripts/final-verify.ps1`.
- Thêm/cập nhật tài liệu final verification, Postman status, release checklist, SRS alignment và screenshot guide.

## Cách chép vào repository local

Giải nén gói trả về vào một thư mục riêng. Chép **nội dung** của gói vào repository hiện tại, cho phép ghi đè file cùng tên, nhưng không chép/xóa thư mục `.git` của repository local.

Sau đó từ root repository:

```powershell
git status
git diff --check
git add .
git commit -m "test: finalize verification tooling and release evidence"
git push
```

Workflow `Maven Verify` sẽ chạy trên branch `fix/**`. Chờ workflow xanh trước khi mở/merge Pull Request.

## Lệnh xác minh local

```powershell
cd HolidayPlanner
.\scripts\final-verify.ps1
```

Khi app đang chạy và có Newman:

```powershell
.\scripts\final-verify.ps1 -RunPostman
```

Không commit output trong `target/verification/`; thư mục này đã được ignore bởi rule `**/target/`.
