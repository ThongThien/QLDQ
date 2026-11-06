# Authentication và session

## Tài khoản demo

- Chỉ huy: `chihuy` / `Demo@123`
- Chiến sĩ: `chiensi01` đến `chiensi10` / `Demo@123`

Các mật khẩu trong database là BCrypt hash. Cần import `src/main/resources/seed.sql` nếu database chưa có các tài khoản trên.

## Cấu hình

```env
SESSION_TIMEOUT=90m
SESSION_COOKIE_SECURE=false
SESSION_COOKIE_SAME_SITE=lax
SESSION_JDBC_INITIALIZE_SCHEMA=always
FRONTEND_URL=http://localhost:3000
```

Local HTTP dùng `SESSION_COOKIE_SECURE=false`. Khi production chạy HTTPS, đặt `SESSION_COOKIE_SECURE=true`. Nếu FE và BE thực sự nằm ở hai site khác nhau, dùng `SESSION_COOKIE_SAME_SITE=none` đồng thời bắt buộc bật Secure.

Spring Session tự tạo hai bảng `SPRING_SESSION` và `SPRING_SESSION_ATTRIBUTES` trong PostgreSQL. Timeout là 90 phút **không hoạt động**; mỗi request hợp lệ tiếp tục duy trì phiên.

## Luồng gọi từ Frontend

Mọi request phải dùng `credentials: "include"`. Với POST/PUT/PATCH/DELETE, gửi CSRF token:

```javascript
const api = "http://localhost:8080/api";

const csrf = await fetch(`${api}/auth/csrf`, {
  credentials: "include",
}).then((response) => response.json());

const user = await fetch(`${api}/auth/login`, {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({ username: "chihuy", password: "Demo@123" }),
}).then((response) => response.json());
```

Sau refresh hoặc chuyển trang, gọi `GET /api/auth/me` với `credentials: "include"`. Chỉ đưa người dùng về màn hình login khi API trả `401`; không logout khi nhận `403`.

## Kiểm thử thủ công nhanh bằng PowerShell

Khởi động backend rồi chạy trong PowerShell 7:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $session
$headers = @{ $csrf.headerName = $csrf.token }

$body = @{ username = "chihuy"; password = "Demo@123" } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/auth/login -Method Post -WebSession $session -Headers $headers -ContentType "application/json" -Body $body
Invoke-RestMethod http://localhost:8080/api/auth/me -WebSession $session
Invoke-RestMethod http://localhost:8080/api/soldiers -WebSession $session

Invoke-RestMethod http://localhost:8080/api/auth/logout -Method Post -WebSession $session -Headers $headers
```

Các kiểm tra quan trọng nên làm thêm:

1. Login Chỉ huy, mở Dashboard rồi tab Chiến sĩ và refresh: `/auth/me` vẫn trả Chỉ huy.
2. Login Chiến sĩ rồi gọi `/api/soldiers`: nhận `403`, nhưng gọi lại `/auth/me` vẫn thành công.
3. Bỏ cookie rồi gọi API nghiệp vụ: nhận `401`.
4. Gửi POST không có `X-CSRF-TOKEN`: nhận `403` và session không bị xóa.
