# QLDQ API - Supabase và dữ liệu demo

## 1. Lấy thông tin kết nối Supabase

1. Mở dự án trên Supabase Dashboard.
2. Vào **Connect** hoặc **Project Settings > Database**.
3. Chọn thông tin kết nối PostgreSQL phù hợp:
   - Dùng **Direct connection** khi môi trường hỗ trợ IPv6.
   - Dùng **Session pooler** nếu máy local không kết nối được Direct connection.
4. Sao chép host, port, database, user và dùng mật khẩu database của dự án.
5. Mở file `.env` trong thư mục `qldqapi` và thay các placeholder. Không commit file này.

Ví dụ cấu hình (không phải credentials thật):

```env
DB_HOST=your_supabase_host
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
DB_SCHEMA=public
DB_SSL_MODE=require
```

Nếu dùng Supabase pooler, `DB_USERNAME` có thể có dạng `postgres.<project-ref>` và port phải lấy đúng từ bảng **Connect**, không tự mặc định theo ví dụ.

## 2. Tạo schema bằng Code First

Từ thư mục `qldqapi`, chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Ứng dụng tự đọc `.env`. Hibernate dùng `ddl-auto=update`, vì vậy chỉ tạo/cập nhật schema và không xóa dữ liệu đã có. Khi backend khởi động thành công lần đầu, các bảng trong schema `public` đã sẵn sàng để nhận dữ liệu mẫu.

## 3. Import dữ liệu mẫu

> **Cảnh báo:** `seed.sql` xóa toàn bộ dữ liệu nghiệp vụ cũ bằng `TRUNCATE` trước khi nhập lại. Không chạy trên database có dữ liệu cần giữ.

1. Trong Supabase Dashboard, mở **SQL Editor**.
2. Tạo query mới và dán toàn bộ nội dung file `src/main/resources/seed.sql`.
3. Nhấn **Run**.
4. Có thể chạy lại script để đưa database về đúng bộ dữ liệu demo ban đầu; dữ liệu nghiệp vụ hiện tại sẽ bị thay thế.

Script bám theo ba biểu mẫu PDF tuần **02/11-08/11/2026**, dùng múi giờ `Asia/Ho_Chi_Minh` và tạo 84 ca gác (12 ca/ngày), 29 công việc tuần cùng 14 phân công theo dõi địa bàn. Trạng thái thời gian không được lưu trùng trong database mà được suy ra từ `start_at` và `end_at`.

Khi thật sự muốn seed tự động lúc Backend khởi động, đặt tạm `SQL_INIT_MODE=always`, khởi động đúng một lần rồi trả về `SQL_INIT_MODE=never` để tránh xóa dữ liệu ngoài ý muốn.

Nếu đổi `DB_SCHEMA` khỏi `public`, cần đổi dòng `SET LOCAL search_path` trong `seed.sql` sang schema tương ứng trước khi import.

## 4. Tài khoản demo

Tất cả tài khoản dưới đây dùng mật khẩu `Demo@123`. Database chỉ lưu BCrypt hash được tạo bởi `pgcrypto`.

| Vai trò | Username | Họ tên | Mật khẩu |
|---|---|---|---|
| Chỉ huy | `chihuy` | Huỳnh Hiếu Hân | `Demo@123` |
| Chiến sĩ | `chiensi01` | Lê Hữu Đạt | `Demo@123` |
| Chiến sĩ | `chiensi02` | Võ Ngọc Sơn | `Demo@123` |
| Chiến sĩ | `chiensi03` | Võ Thành Tâm | `Demo@123` |
| Chiến sĩ | `chiensi04` | Phạm Hoàng Châu | `Demo@123` |
| Chiến sĩ | `chiensi05` | Nguyễn Hải Đăng | `Demo@123` |
| Chiến sĩ | `chiensi06` | Trần Đình Hoàng Nhật | `Demo@123` |
| Chiến sĩ | `chiensi07` | Trần Ngọc Sang | `Demo@123` |
| Chiến sĩ | `chiensi08` | Phan Văn Phước | `Demo@123` |
| Chiến sĩ | `chiensi09` | Trịnh Ngọc Duy | `Demo@123` |
| Chiến sĩ | `chiensi10` | Thái Minh Trọng | `Demo@123` |
| Chiến sĩ | `chiensi11` | Dương Đại Phát | `Demo@123` |
| Chiến sĩ | `chiensi12` | Nguyễn Quốc Anh | `Demo@123` |
| Chiến sĩ | `chiensi13` | Thái Tấn Toàn | `Demo@123` |
| Chiến sĩ | `chiensi14` | Nguyễn Đức Huy | `Demo@123` |

Các tài khoản đã tồn tại sẽ được cập nhật lại mật khẩu demo mỗi lần chạy seed. Đây chỉ là dữ liệu phục vụ demo; cần đổi hoặc vô hiệu hóa trước khi dùng ở môi trường thật.

## 5. Khởi động lại backend

Sau khi import seed, chạy lại:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend mặc định chạy tại `http://localhost:8080`. `FRONTEND_URL`, `SERVER_PORT`, timeout phiên và timezone đều có thể thay đổi trong `.env`.

Đăng nhập session đã được kết nối trực tiếp với bảng `app_users`. Xem [AUTHENTICATION.md](AUTHENTICATION.md) để lấy CSRF token, đăng nhập, kiểm tra session và đăng xuất.

## 6. Chạy chế độ mô phỏng thời gian

Chỉ dùng ở local/development:

```env
SPRING_PROFILES_ACTIVE=dev
```

Khởi động lại Backend và chạy Frontend bằng `npm run dev`. Đăng nhập tài khoản Chỉ huy để thấy điều khiển mô phỏng trên Header. Production không được cấu hình profile `dev`; khi đó các API `/api/dev/time` không tồn tại và Frontend production không hiển thị điều khiển.

## 7. Xuất lịch công tác tuần

Mở **Lịch công tác tuần**, chọn **Xuất PDF** tại tuần cần in. Trong hộp thoại của trình duyệt, chọn **Save as PDF / Lưu dưới dạng PDF**. Bản in dùng trực tiếp dữ liệu lịch đã tải từ Backend và được định dạng A4 ngang.
