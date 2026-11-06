# Hướng dẫn kiểm thử hệ thống từ A–Z

Tài liệu này dùng cho bộ dữ liệu tuần **02/11/2026 – 08/11/2026** được nhập từ ba biểu mẫu PDF: lịch gác cổng, lịch công tác tuần và lịch theo dõi tình hình địa bàn.

## 1. Tài khoản và phân quyền mong đợi

| Vai trò | Tài khoản | Mật khẩu | Menu phải thấy |
|---|---|---|---|
| Chỉ huy | `chihuy` | `Demo@123` | Tổng quan, Lịch gác, Chiến sĩ, Lịch công tác tuần, Theo dõi địa bàn |
| Chiến sĩ mẫu | `chiensi01` | `Demo@123` | Tổng quan, Lịch gác, Lịch công tác tuần, Theo dõi địa bàn |
| Chiến sĩ còn lại | `chiensi02` đến `chiensi14` | `Demo@123` | Giống Chiến sĩ mẫu |

Chiến sĩ không được thấy menu **Chiến sĩ** và không được thực hiện API tạo/sửa/xóa dữ liệu quản lý. Nếu một API trả `403`, phiên đăng nhập vẫn phải còn nguyên. Chỉ `401` mới thể hiện chưa đăng nhập hoặc session đã hết hạn.

## 2. Chuẩn bị môi trường

1. Mở `qldqapi/.env`, kiểm tra `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SCHEMA=public`.
2. Backend dùng cổng `8080`; Frontend dùng cổng `3000`.
3. Frontend cần file `.env.local` với:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

4. Không commit `.env` hoặc mật khẩu Supabase.

## 3. Nạp lại dữ liệu mẫu

File `qldqapi/src/main/resources/seed.sql` có lệnh `TRUNCATE`, vì vậy mỗi lần chạy sẽ xóa dữ liệu nghiệp vụ cũ rồi nạp lại từ đầu. Schema và bảng session không bị xóa.

Cách thủ công dễ theo dõi nhất:

1. Mở Supabase → SQL Editor → New query.
2. Dán toàn bộ nội dung `seed.sql`.
3. Chọn đúng project/database đang cấu hình trong `.env`.
4. Nhấn Run một lần và kiểm tra kết quả cuối là `COMMIT` thành công.

Các số liệu chuẩn sau khi import:

| Dữ liệu | Số lượng mong đợi |
|---|---:|
| Tài khoản Chỉ huy | 1 |
| Tài khoản Chiến sĩ | 14 |
| Ca gác | 84 |
| Phân công gác | 168 |
| Lịch công tác tuần | 1 |
| Nhiệm vụ công tác | 29 |
| Lịch theo dõi địa bàn | 1 |
| Lượt theo dõi địa bàn | 14 |
| Thông báo seed | 14 |

Có thể kiểm tra nhanh trong SQL Editor:

```sql
select role, count(*) from app_users group by role order by role;
select count(*) as guard_shifts from guard_shifts;
select count(*) as guard_assignments from guard_assignments;
select schedule_type, count(*) from work_schedules group by schedule_type;
select ws.schedule_type, count(wi.id)
from work_schedules ws left join work_items wi on wi.work_schedule_id = ws.id
group by ws.schedule_type order by ws.schedule_type;
```

## 4. Khởi động hệ thống

Terminal 1:

```powershell
cd qldqapi
.\mvnw.cmd spring-boot:run
```

Đạt khi log có `Started QldqapiApplication` và không có lỗi Hikari/PostgreSQL.

Terminal 2:

```powershell
cd QLDQ_WEB\frontend
npm.cmd run dev
```

Mở `http://localhost:3000`.

## 5. Kiểm thử Chỉ huy

### 5.1 Đăng nhập và session

- Đăng nhập `chihuy / Demo@123`.
- Refresh trình duyệt: vẫn đăng nhập.
- Chuyển liên tục qua 5 menu: không logout, icon và sidebar không đổi cấu trúc.
- Tab Chiến sĩ mở bình thường và hiển thị đủ 14 người.

### 5.2 Dashboard

- Tổng quân số phải là 14, đang hoạt động là 14.
- Dữ liệu tuần phải nằm trong 02/11–08/11/2026 khi đang dùng thời gian mô phỏng trong tuần mẫu.
- Có lịch công tác, lịch địa bàn, thông báo và thông tin gác.
- Không xuất hiện mock data cũ hoặc tên chiến sĩ ngoài danh sách mới.

### 5.3 Lịch gác

- Tuần có đủ 7 ngày, mỗi ngày 12 khung giờ từ `07:00–09:00` đến `05:00–07:00`.
- Mỗi ca có đúng 2 chiến sĩ và địa điểm duy nhất là **Cổng chính**.
- Kiểm tra ngày 02/11:
  - `07:00–09:00`: Trần Ngọc Sang, Phan Văn Phước.
  - `23:00–01:00`: Trịnh Ngọc Duy, Thái Minh Trọng.
  - `05:00–07:00`: Nguyễn Hải Đăng, Trần Đình Hoàng Nhật.
- Tạo thử một ca, sửa ca, đổi người và hủy ca; kiểm tra thông báo và Activity Log tương ứng.
- Thử phân công trùng giờ: hệ thống phải cảnh báo xung đột.

### 5.4 Lịch công tác tuần

- Có một lịch đã công bố từ 02/11 đến 08/11 với 29 nhiệm vụ.
- Kiểm tra các mốc tiêu biểu: chào cờ 06:30 thứ Hai, học 7 nhiệm vụ DQTV thứ Ba, bảo quản vũ khí 16:30 thứ Năm, huấn luyện điều lệnh thứ Sáu, chế độ ngày nghỉ cuối tuần.
- Chỉ huy thấy nút tạo, chỉnh sửa, công bố/xóa khi trạng thái cho phép và xuất PDF.

### 5.5 Theo dõi địa bàn

- Có đủ 14 lượt: sáng `06:30–07:30`, chiều `16:00–17:00` trong 7 ngày.
- Ngày 02/11: sáng Lê Hữu Đạt + Võ Ngọc Sơn; chiều Dương Đại Phát + Nguyễn Quốc Anh.
- Ngày 08/11: sáng Trần Ngọc Sang + Trịnh Ngọc Duy; chiều Phan Văn Phước + Thái Minh Trọng.
- Chỉ huy có quyền tạo và chỉnh sửa.

## 6. Kiểm thử Chiến sĩ

Đăng xuất Chỉ huy rồi đăng nhập `chiensi01 / Demo@123`.

- Sidebar chỉ có 4 menu; không có menu Chiến sĩ.
- Tổng quan chỉ hiển thị dữ liệu của Lê Hữu Đạt, không hiển thị số liệu quản trị toàn đơn vị.
- Lịch gác vẫn xem được thời khóa biểu chung; các thao tác xác nhận/kết quả chỉ áp dụng cho ca của chính tài khoản.
- Lịch công tác tuần mặc định ở tab **Lịch đơn vị**, hiển thị lịch đã công bố với đủ 29 nhiệm vụ. Chuyển sang **Lịch của tôi** còn 24 nhiệm vụ được phân công cho Lê Hữu Đạt; cả hai tab đều không có nút tạo/sửa/xóa/công bố.
- Theo dõi địa bàn mặc định ở tab **Lịch đơn vị**, hiển thị đủ 14 lượt. Chuyển sang **Lịch của tôi** chỉ còn 2 lượt của Lê Hữu Đạt: sáng 02/11 và sáng 05/11; không có nút quản trị.
- Mở trực tiếp `http://localhost:3000/?view=soldiers`: không được xem danh sách quân số.
- Gọi API quản trị bằng session Chiến sĩ phải nhận `403`, sau đó refresh trang vẫn đăng nhập.

Lặp lại nhanh với `chiensi12` để kiểm tra dữ liệu cá nhân khác: Nguyễn Quốc Anh có lượt địa bàn chiều 02/11 và chiều 07/11.

## 7. Ma trận API phân quyền cần kiểm tra

| API | Chỉ huy | Chiến sĩ |
|---|---:|---:|
| `GET /api/dashboard` | 200, toàn đơn vị | 200, dữ liệu cá nhân |
| `GET /api/soldiers` | 200 | 403 |
| `GET /api/guard-shifts` | 200 | 200 |
| Tạo/sửa/hủy ca gác | 2xx | 403 |
| Xác nhận/kết quả ca của mình | 403 | 2xx |
| `GET /api/work-schedules?mine=false` | 200, đầy đủ | 200, lịch đơn vị đã công bố |
| `GET /api/work-schedules?mine=true` | 200, đầy đủ | 200, chỉ nhiệm vụ được phân công |
| CRUD `/api/work-schedules/**` | 2xx | 403 |
| `GET /api/area-monitoring?mine=false` | 200, đầy đủ | 200, lịch đơn vị đã công bố |
| `GET /api/area-monitoring?mine=true` | 200, đầy đủ | 200, chỉ lượt được phân công |
| CRUD `/api/area-monitoring/**` | 2xx | 403 |
| `/api/activity-logs` | 200 | 403 |
| `/api/dev/time/**` | 2xx trong profile dev | 403 |

## 8. Thông báo, đồng bộ và thời gian

- Bell ban đầu có thông báo chưa đọc; mở và đánh dấu đã đọc làm badge giảm.
- Chọn đánh dấu tất cả đã đọc: badge về 0.
- Chỉ huy sửa một ca có Chiến sĩ được phân công; trong tối đa 60 giây phía Chiến sĩ phải nhận dữ liệu mới mà không F5.
- Chiến sĩ xác nhận ca; phía Chỉ huy cập nhật trong chu kỳ đồng bộ tiếp theo.
- Khi tab trình duyệt bị ẩn, polling không được gọi liên tục; quay lại tab phải refetch ngay.
- Trong development, dùng Simulation Mode kiểm tra trước giờ, đúng giờ bắt đầu, trong ca, đúng giờ kết thúc và sau ca. Tắt mô phỏng phải trở về giờ thật.

### 8.1 Kiểm tra biên DateTime

- Với một nhiệm vụ `06:45–08:30`: trước `06:45` là `UPCOMING`, đúng `06:45` và trước `08:30` là `ONGOING`, đúng `08:30` là `COMPLETED`.
- Với ca qua đêm `23:00–01:00`: từ `23:00` ngày bắt đầu đến trước `01:00` ngày hôm sau là `ONGOING`; đúng `01:00` là `COMPLETED`.
- Lịch đã hủy luôn là `CANCELLED`, không bị trạng thái tính theo giờ ghi đè.
- Kiểm tra API trả đúng giờ gốc trong biểu mẫu, ví dụ địa bàn chiều là `16:00–17:00`, không bị cộng hoặc trừ 7 giờ.

### 8.2 Kiểm tra cache và cô lập tài khoản

- Mở DevTools > Network, vào một trang lịch rồi chuyển sang menu khác và quay lại trong vòng 30 giây: dữ liệu dùng cache, không gọi lặp API cùng tham số.
- Sau 30–60 giây hoặc khi focus lại tab, dữ liệu được refetch một lần; không xuất hiện nhiều interval gọi trùng.
- Sau thao tác tạo/sửa/hủy/xác nhận thành công, lần tải tiếp theo phải lấy dữ liệu mới, không giữ cache cũ.
- Đăng xuất `chiensi01`, đăng nhập `chiensi12`: không được thấy dữ liệu cá nhân đã cache của tài khoản trước.
- Lỗi `403` chỉ hiển thị lỗi quyền; chỉ `401` hợp lệ mới chuyển về đăng nhập.

### 8.3 Kiểm tra PDF Preview

- Ở Lịch công tác tuần và Theo dõi địa bàn, chọn **Xem trước PDF**.
- Modal phải hiện đúng tuần, thứ/ngày, thời gian, nội dung, địa điểm và lực lượng; thử phóng to, thu nhỏ và đóng modal.
- Chọn **Tải xuống PDF**, trong hộp thoại in chọn **Save as PDF**; file xuất ra phải đọc được tiếng Việt, bố cục ngang A4 và không chứa menu/header web.

## 9. Responsive và lỗi giao diện nhỏ

Kiểm tra lần lượt ở khoảng 1440 px, 768 px và 390 px:

- Sidebar desktop/mobile chỉ là một menu thống nhất.
- Menu mobile mở/đóng được, backdrop đóng được.
- Bảng có vùng cuộn nội bộ khi thật sự cần, không làm cả trang tràn ngang.
- Header không che nội dung; nút đăng xuất và bell vẫn thao tác được.
- Tiếng Việt không bị lỗi font hoặc mã hóa.
- Loading, empty state, error state và thông báo thành công đều hiển thị rõ.
- Màu chủ đạo là cam sáng, chữ và trạng thái có độ tương phản rõ; không còn nội dung tài khoản demo trên màn hình đăng nhập.
- Logo dùng ảnh từ `NEXT_PUBLIC_UNIT_LOGO_URL` nếu được cấu hình, nếu chưa có thì hiển thị ký hiệu `BC` thay thế mà không vỡ bố cục.
- Footer hiển thị đủ tên đơn vị, địa chỉ/bản đồ, điện thoại, email và Facebook; các liên kết mở đúng đích.

## 10. Checklist tracking

| Mã | Hạng mục | Kết quả | Ghi chú |
|---|---|---|---|
| AUTH-01 | Đăng nhập Chỉ huy | ☐ Pass ☐ Fail | |
| AUTH-02 | Đăng nhập Chiến sĩ | ☐ Pass ☐ Fail | |
| AUTH-03 | Refresh giữ session | ☐ Pass ☐ Fail | |
| AUTH-04 | 403 không gây logout | ☐ Pass ☐ Fail | |
| MENU-01 | Chỉ huy có 5 menu | ☐ Pass ☐ Fail | |
| MENU-02 | Chiến sĩ có 4 menu | ☐ Pass ☐ Fail | |
| DATA-01 | 14 chiến sĩ | ☐ Pass ☐ Fail | |
| DATA-02 | 84 ca, 168 phân công | ☐ Pass ☐ Fail | |
| DATA-03 | 29 công việc tuần | ☐ Pass ☐ Fail | |
| DATA-04 | 14 lượt địa bàn | ☐ Pass ☐ Fail | |
| GUARD-01 | Xem đủ thời khóa biểu | ☐ Pass ☐ Fail | |
| GUARD-02 | CRUD của Chỉ huy | ☐ Pass ☐ Fail | |
| GUARD-03 | Xác nhận/kết quả của Chiến sĩ | ☐ Pass ☐ Fail | |
| WORK-01 | Chỉ huy quản lý lịch tuần | ☐ Pass ☐ Fail | |
| WORK-02 | Chiến sĩ chuyển được Lịch đơn vị/Lịch của tôi | ☐ Pass ☐ Fail | |
| AREA-01 | Chỉ huy quản lý lịch địa bàn | ☐ Pass ☐ Fail | |
| AREA-02 | Chiến sĩ chuyển được Lịch đơn vị/Lịch của tôi | ☐ Pass ☐ Fail | |
| NOTI-01 | Badge, đọc một, đọc tất cả | ☐ Pass ☐ Fail | |
| SYNC-01 | Polling/refetch không cần F5 | ☐ Pass ☐ Fail | |
| TIME-01 | Simulation và trạng thái ca | ☐ Pass ☐ Fail | |
| TIME-02 | Biên bắt đầu/kết thúc và ca qua đêm | ☐ Pass ☐ Fail | |
| CACHE-01 | Không gọi API lặp khi cache còn mới | ☐ Pass ☐ Fail | |
| CACHE-02 | Logout xóa cache, không rò dữ liệu tài khoản | ☐ Pass ☐ Fail | |
| PDF-01 | Preview/zoom/đóng/xuất PDF lịch tuần | ☐ Pass ☐ Fail | |
| PDF-02 | Preview/zoom/đóng/xuất PDF địa bàn | ☐ Pass ☐ Fail | |
| BRAND-01 | Logo cấu hình và footer đúng liên kết | ☐ Pass ☐ Fail | |
| UI-01 | Desktop/tablet/mobile | ☐ Pass ☐ Fail | |

Khi gặp lỗi, ghi thêm: tài khoản, URL/menu, thời điểm mô phỏng, thao tác ngay trước lỗi, HTTP status và ảnh chụp màn hình. Như vậy lần chỉnh sửa tiếp theo có thể tái hiện đúng mà không phải đoán.
