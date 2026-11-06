# Business API - Hệ thống quản lý Trung đội Dân quân thường trực

Base URL mặc định: `http://localhost:8080/api`

## Quy ước chung

- Các API nghiệp vụ nhận người dùng từ session cookie `SESSION`; không còn dùng hoặc tin cậy header `X-User-Id`.
- Client phải gửi mọi request với `credentials: "include"`.
- Trước login hoặc request thay đổi dữ liệu, gọi `GET /auth/csrf`, giữ cookie và gửi giá trị `token` trong header `X-CSRF-TOKEN`.
- `401` nghĩa là chưa đăng nhập/session hết hạn; `403` nghĩa là đã đăng nhập nhưng không đủ quyền. Client không được tự logout khi gặp `403`.
- Danh sách phân trang trả về `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.
- Lỗi trả về `timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`.
- Ngày thuần túy dùng `yyyy-MM-dd`; thời điểm ca gác dùng ISO-8601 có offset, ví dụ `2026-11-05T22:00:00+07:00`.

## Authentication

| Method | Endpoint | Request | Response | Quyền |
|---|---|---|---|---|
| GET | `/auth/csrf` | Không có | `{ headerName, parameterName, token }` | Công khai |
| POST | `/auth/login` | `{ "username": "chihuy", "password": "Demo@123" }` | User hiện tại, không có password | Công khai + CSRF |
| GET | `/auth/me` | Cookie session | User hiện tại | Đã đăng nhập |
| POST | `/auth/logout` | Cookie session + CSRF | `204` | Đã đăng nhập |

Session được lưu trong PostgreSQL bằng Spring Session JDBC, mặc định hết hạn sau 90 phút không hoạt động. Khi đăng nhập, session ID được thay đổi để chống session fixation. Chi tiết tích hợp client và kiểm thử thủ công xem [AUTHENTICATION.md](AUTHENTICATION.md).

## 1. Quản lý quân số

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/soldiers` | Query `search`, `active`, `page`, `size` | Trang `SoldierResponse` | Danh sách, tìm theo tên/username và lọc hoạt động |
| GET | `/soldiers/{soldierId}` | Path ID | `SoldierResponse` | Chi tiết chiến sĩ và số ca được phân công/hoàn thành |

Hai API này dành cho Chỉ huy. Quân số được lấy trực tiếp từ toàn bộ tài khoản `SOLDIER`, không suy ra từ một ca gác.

## 2. Lịch gác cổng

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/guard-shifts` | Query `from`, `to`, `status`, `soldierId`, `page`, `size` | Trang `GuardShiftResponse` | Thời khóa biểu chung hoặc lọc lịch cá nhân |
| GET | `/guard-shifts/{shiftId}` | Path ID | `GuardShiftResponse` | Chi tiết ca, assignment, xác nhận, kết quả và sự cố |
| POST | `/guard-shifts` | `GuardShiftRequest` | `201 GuardShiftResponse` | Tạo ca và phân công nhiều chiến sĩ |
| PUT | `/guard-shifts/{shiftId}` | `GuardShiftRequest` | `GuardShiftResponse` | Cập nhật ca và thay đổi danh sách phân công |
| PATCH | `/guard-shifts/{shiftId}/status` | `{ "status": "PUBLISHED" }` hoặc `CANCELLED` | `GuardShiftResponse` | Công bố hoặc hủy ca |
| DELETE | `/guard-shifts/{shiftId}` | Không có body | `204` | Xóa ca nháp |
| POST | `/guard-shifts/{shiftId}/acknowledgement` | Không có body | `GuardShiftResponse` | Chiến sĩ xác nhận đã xem lịch |
| POST | `/guard-shifts/{shiftId}/result` | `GuardResultRequest` | `GuardShiftResponse` | Chiến sĩ ghi nhận kết quả/sự cố |

Ví dụ tạo ca gác:

```json
{
  "startAt": "2026-11-05T22:00:00+07:00",
  "endAt": "2026-11-06T00:00:00+07:00",
  "location": "Cổng chính",
  "note": "Có mặt trước giờ nhận ca 10 phút",
  "status": "DRAFT",
  "soldierIds": [2, 3]
}
```

Ví dụ kết quả ca gác:

```json
{
  "resultCondition": "INCIDENT",
  "resultContent": "Đã kiểm tra và báo cáo trực ban.",
  "incidentNote": "Có phương tiện đến liên hệ ngoài danh sách đăng ký."
}
```

`status` trong response là trạng thái tương thích UI (`DRAFT`, `PUBLISHED`, `COMPLETED`, `CANCELLED`). `managementStatus` là trạng thái lưu trong database; `occurrenceStatus` (`UPCOMING`, `ONGOING`, `COMPLETED`) được tính tại thời điểm trả response.

## 3. Lịch công tác tuần

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/work-schedules` | Query `page`, `size` | Trang `ScheduleResponse` | Danh sách tuần công tác |
| GET | `/work-schedules/{scheduleId}` | Path ID | `ScheduleResponse` | Chi tiết tuần và các công việc |
| POST | `/work-schedules` | `ScheduleRequest` | `201 ScheduleResponse` | Tạo lịch tuần |
| PUT | `/work-schedules/{scheduleId}` | `ScheduleRequest` | `ScheduleResponse` | Cập nhật thông tin lịch |
| POST | `/work-schedules/{scheduleId}/publish` | Không có body | `ScheduleResponse` | Công bố lịch |
| DELETE | `/work-schedules/{scheduleId}` | Không có body | `204` | Xóa lịch nháp |
| POST | `/work-schedules/{scheduleId}/items` | `WorkItemRequest` | `201 WorkItemResponse` | Thêm công việc |
| PUT | `/work-schedules/{scheduleId}/items/{itemId}` | `WorkItemRequest` | `WorkItemResponse` | Sửa công việc và phân công |
| DELETE | `/work-schedules/{scheduleId}/items/{itemId}` | Không có body | `204` | Xóa công việc |

Ví dụ tạo lịch tuần:

```json
{
  "title": "Lịch công tác tuần 45",
  "weekStartDate": "2026-11-02",
  "scopeArea": null,
  "generalNote": "Thực hiện theo kế hoạch của đơn vị.",
  "status": "DRAFT"
}
```

Ví dụ thêm công việc:

```json
{
  "workDate": "2026-11-03",
  "period": "MORNING",
  "startTime": "07:00:00",
  "endTime": "09:00:00",
  "allDay": false,
  "content": "Huấn luyện điều lệnh đội ngũ",
  "leader": "Trung đội trưởng",
  "participantsDescription": "Tiểu đội 1 và Tiểu đội 2",
  "location": "Sân huấn luyện",
  "preparation": "Trang phục thống nhất",
  "dutyLeader": "Trần Quốc Anh",
  "note": null,
  "sortOrder": 1,
  "soldierIds": [2, 3, 4]
}
```

## 4. Nắm tình hình địa bàn

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/area-monitoring` | Query `page`, `size` | Trang `ScheduleResponse` | Danh sách lịch địa bàn |
| GET | `/area-monitoring/{scheduleId}` | Path ID | `ScheduleResponse` | Chi tiết lịch theo tuần |
| GET | `/area-monitoring/items` | Query `date`, `period`, `page`, `size` | Trang `WorkItemResponse` | Lọc nhiệm vụ theo ngày và buổi |
| POST | `/area-monitoring` | `ScheduleRequest` | `201 ScheduleResponse` | Tạo lịch địa bàn |
| PUT | `/area-monitoring/{scheduleId}` | `ScheduleRequest` | `ScheduleResponse` | Cập nhật lịch |
| POST | `/area-monitoring/{scheduleId}/publish` | Không có body | `ScheduleResponse` | Công bố lịch |
| DELETE | `/area-monitoring/{scheduleId}` | Không có body | `204` | Xóa lịch nháp |
| POST | `/area-monitoring/{scheduleId}/items` | `WorkItemRequest` | `201 WorkItemResponse` | Thêm phân công theo ngày/buổi/khu vực |
| PUT | `/area-monitoring/{scheduleId}/items/{itemId}` | `WorkItemRequest` | `WorkItemResponse` | Cập nhật phân công và lực lượng |
| DELETE | `/area-monitoring/{scheduleId}/items/{itemId}` | Không có body | `204` | Xóa phân công |

Nhóm này tái sử dụng `WorkSchedule`, `WorkItem` và `WorkItemAssignment` với `scheduleType=AREA_MONITORING`. FE nhóm các item theo `workDate` và `period` để hiển thị dạng từng ngày/buổi.

## 5. Dashboard

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/dashboard` | Session Chỉ huy hoặc Chiến sĩ | `DashboardResponse` | Thống kê theo đúng phạm vi của tài khoản đăng nhập |

Dashboard không có bảng riêng. Số liệu được tổng hợp bằng truy vấn đếm từ `app_users`, ca gác, phân công, lịch công tác và thông báo. Với Chiến sĩ, ca gác và công việc chỉ tính các bản ghi được phân công cho chính tài khoản đó. Tiến độ công việc được suy ra theo ngày/giờ kết thúc vì mô hình hiện tại không có quy trình xác nhận hoàn thành riêng cho lịch công tác.

## 6. Thông báo nội bộ

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/notifications` | Query `page`, `size` | Trang `NotificationResponse` | Danh sách thông báo của người đăng nhập, mới nhất trước |
| GET | `/notifications/unread-count` | Không có | `{ unreadCount }` | Số thông báo chưa đọc |
| PATCH | `/notifications/{id}/read` | Không có | `NotificationResponse` | Đánh dấu một thông báo thuộc người dùng là đã đọc |
| PATCH | `/notifications/read-all` | Không có | `204` | Đánh dấu toàn bộ thông báo của người dùng là đã đọc |
| GET | `/work-items/{id}` | Path ID | `WorkItemResponse` | Mở nhiệm vụ được tham chiếu; Chiến sĩ chỉ xem nhiệm vụ được phân công hoặc đã nhận thông báo |

Thông báo được tạo trong cùng transaction với nghiệp vụ ca gác/lịch công tác. Scheduler dùng `NOTIFICATION_REMINDER_HOURS` (mặc định `12,2`) và `NOTIFICATION_SCHEDULER_DELAY_MS` (mặc định `60000`) theo múi giờ `Asia/Ho_Chi_Minh`. `event_key` có unique constraint để không gửi lặp cùng sự kiện hoặc cùng mốc nhắc.

## 7. Kiểm tra xung đột phân công

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| POST | `/assignment-validation/guard-shift` | Thời gian ca, danh sách chiến sĩ, ID ca đang sửa nếu có | `AssignmentValidationResponse` | Cảnh báo ca gác trùng ca gác hoặc lịch công tác |
| POST | `/assignment-validation/work-item` | Ngày/giờ công việc, danh sách chiến sĩ, ID công việc đang sửa nếu có | `AssignmentValidationResponse` | Cảnh báo công việc trùng ca gác hoặc nhiệm vụ khác |

Các API này chỉ dành cho Chỉ huy và trả cảnh báo trước khi lưu. Hệ thống không tự đặt giới hạn số ca hoặc số giờ làm việc; Chỉ huy có thể xác nhận tiếp tục sau khi xem cảnh báo.

## 8. Lịch sử hoạt động

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/activity-logs` | Query tùy chọn `from`, `to`, `action`, `targetType`, `targetId`, `page`, `size` | Trang `ActivityLogResponse` | Chỉ huy xem lịch sử nghiệp vụ, mới nhất trước |

`from` và `to` nhận thời điểm ISO-8601 có offset (ví dụ `2026-11-01T00:00:00+07:00`). Actor luôn lấy từ session đăng nhập. Activity Log được ghi trong cùng transaction sau thao tác nghiệp vụ thành công, không ghi request GET, mật khẩu, token hoặc nội dung xác thực.

## 9. Mô phỏng thời gian (chỉ profile `dev`)

| Method | Endpoint | Request | Response | Mục đích |
|---|---|---|---|---|
| GET | `/dev/time` | Không có | `SimulationStatus` | Xem thời gian nghiệp vụ hiện tại |
| PUT | `/dev/time` | `{ "simulatedAt": "2026-11-05T03:00:00+07:00" }` | `SimulationStatus` | Bật hoặc đặt lại thời gian mô phỏng |
| POST | `/dev/time/advance` | `{ "hours": 1, "days": 0 }` | `SimulationStatus` | Tăng/giảm thời gian; chấp nhận số âm |
| DELETE | `/dev/time` | Không có | `SimulationStatus` | Tắt mô phỏng, trở về thời gian thực |

Các endpoint chỉ được đăng ký khi Backend chạy với `SPRING_PROFILES_ACTIVE=dev` và chỉ Chỉ huy được gọi. Đồng hồ mô phỏng chỉ dùng để tính trạng thái lịch, Dashboard, giới hạn thao tác theo thời gian và reminder. Session, authentication, `createdAt`, `updatedAt`, thời điểm xác nhận và thời điểm nộp kết quả vẫn dùng thời gian thực.

## 10. Xuất lịch công tác PDF

Frontend dùng dữ liệu `ScheduleResponse` lấy từ `/work-schedules`, dựng mẫu A4 ngang có thứ/ngày, thời gian, nội dung, địa điểm và chiến sĩ. Nút **Xuất PDF** mở hộp thoại in của trình duyệt; chọn **Save as PDF / Lưu dưới dạng PDF** để tải tệp có hỗ trợ tiếng Việt.
