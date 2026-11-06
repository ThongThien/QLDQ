-- Dữ liệu demo bám theo 3 biểu mẫu tuần 02/11/2026 - 08/11/2026.
-- CẢNH BÁO: script này xóa toàn bộ dữ liệu nghiệp vụ hiện có trước khi nạp lại.
-- Không xóa schema và không tác động các bảng SPRING_SESSION.

BEGIN;
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
SET LOCAL search_path TO public, extensions;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

TRUNCATE TABLE notifications, activity_logs, guard_assignments, work_item_assignments,
    guard_shifts, work_items, work_schedules, app_users RESTART IDENTITY CASCADE;

-- Tất cả tài khoản demo dùng mật khẩu Demo@123.
INSERT INTO app_users
    (username, password_hash, full_name, role, rank_or_position, active, created_at, updated_at)
VALUES
    ('chihuy',crypt('Demo@123',gen_salt('bf',10)),'Huỳnh Hiếu Hân','COMMANDER','Trung đội trưởng',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi01',crypt('Demo@123',gen_salt('bf',10)),'Lê Hữu Đạt','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi02',crypt('Demo@123',gen_salt('bf',10)),'Võ Ngọc Sơn','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi03',crypt('Demo@123',gen_salt('bf',10)),'Võ Thành Tâm','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi04',crypt('Demo@123',gen_salt('bf',10)),'Phạm Hoàng Châu','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi05',crypt('Demo@123',gen_salt('bf',10)),'Nguyễn Hải Đăng','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi06',crypt('Demo@123',gen_salt('bf',10)),'Trần Đình Hoàng Nhật','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi07',crypt('Demo@123',gen_salt('bf',10)),'Trần Ngọc Sang','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi08',crypt('Demo@123',gen_salt('bf',10)),'Phan Văn Phước','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi09',crypt('Demo@123',gen_salt('bf',10)),'Trịnh Ngọc Duy','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi10',crypt('Demo@123',gen_salt('bf',10)),'Thái Minh Trọng','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi11',crypt('Demo@123',gen_salt('bf',10)),'Dương Đại Phát','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi12',crypt('Demo@123',gen_salt('bf',10)),'Nguyễn Quốc Anh','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi13',crypt('Demo@123',gen_salt('bf',10)),'Thái Tấn Toàn','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    ('chiensi14',crypt('Demo@123',gen_salt('bf',10)),'Nguyễn Đức Huy','SOLDIER','Chiến sĩ DQTT',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- 84 ca gác cổng: 12 ca/ngày, mỗi ca 2 giờ, 2 chiến sĩ/ca.
WITH slot_data(slot_no,start_time) AS (
    VALUES (1,TIME '07:00'),(2,TIME '09:00'),(3,TIME '11:00'),(4,TIME '13:00'),
           (5,TIME '15:00'),(6,TIME '17:00'),(7,TIME '19:00'),(8,TIME '21:00'),
           (9,TIME '23:00'),(10,TIME '01:00'),(11,TIME '03:00'),(12,TIME '05:00')
), day_data(day_offset,first_group) AS (VALUES (0,4),(1,2),(2,7),(3,5),(4,3),(5,1),(6,6))
INSERT INTO guard_shifts
    (id,start_at,end_at,location,note,status,created_by_id,created_at,updated_at)
SELECT 100000+d.day_offset*12+s.slot_no,
       ((DATE '2026-11-02'+d.day_offset)+s.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (((DATE '2026-11-02'+d.day_offset)+s.start_time)+INTERVAL '2 hours') AT TIME ZONE 'Asia/Ho_Chi_Minh',
       'Cổng chính','Thực hiện đúng quy định gác cổng; bàn giao đầy đủ khi hết ca.','PUBLISHED',
       (SELECT id FROM app_users WHERE username='chihuy'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM day_data d CROSS JOIN slot_data s;

WITH slot_data(slot_no) AS (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12)),
day_data(day_offset,first_group) AS (VALUES (0,4),(1,2),(2,7),(3,5),(4,3),(5,1),(6,6)),
guard_groups(group_no,first_user,second_user) AS (
    VALUES (1,'chiensi05','chiensi06'),(2,'chiensi03','chiensi13'),
           (3,'chiensi14','chiensi04'),(4,'chiensi07','chiensi08'),
           (5,'chiensi09','chiensi10'),(6,'chiensi01','chiensi11'),
           (7,'chiensi02','chiensi12')
), assignment_rows AS (
    SELECT 100000+d.day_offset*12+s.slot_no AS shift_id, person.username
    FROM day_data d CROSS JOIN slot_data s
    JOIN guard_groups g ON g.group_no=((d.first_group-1+s.slot_no-1)%7)+1
    CROSS JOIN LATERAL (VALUES(g.first_user),(g.second_user)) person(username)
)
INSERT INTO guard_assignments
    (guard_shift_id,soldier_id,acknowledged_at,status,result_condition,result_content,
     incident_note,completed_at,created_at,updated_at)
SELECT a.shift_id,u.id,
       CASE WHEN gs.start_at<=CURRENT_TIMESTAMP THEN gs.start_at-INTERVAL '12 hours' END,
       CASE WHEN gs.end_at<=CURRENT_TIMESTAMP THEN 'COMPLETED' ELSE 'ASSIGNED' END,
       CASE WHEN gs.end_at<=CURRENT_TIMESTAMP THEN 'NORMAL' END,
       CASE WHEN gs.end_at<=CURRENT_TIMESTAMP THEN 'Hoàn thành ca gác, tình hình cổng chính bình thường.' END,
       NULL,CASE WHEN gs.end_at<=CURRENT_TIMESTAMP THEN gs.end_at END,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM assignment_rows a JOIN guard_shifts gs ON gs.id=a.shift_id
JOIN app_users u ON u.username=a.username;

INSERT INTO work_schedules
    (id,schedule_type,title,week_start_date,week_end_date,scope_area,general_note,status,
     created_by_id,created_at,updated_at)
VALUES
    (2001,'WEEKLY_WORK','Lịch công tác tuần của bDQTT','2026-11-02','2026-11-08',NULL,
     'Lịch trực có thể thay đổi khi có nhiệm vụ đột xuất của Ban CHQS phường.','PUBLISHED',
     (SELECT id FROM app_users WHERE username='chihuy'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2002,'AREA_MONITORING','Lịch theo dõi tình hình trên địa bàn phường Bắc Cam Ranh của bDQTT',
     '2026-11-02','2026-11-08','Phường Bắc Cam Ranh',
     'Chấp hành quy định giao thông; nắm chắc tình hình, báo cáo ngay trường hợp khẩn cấp và ghi sổ sau nhiệm vụ.',
     'PUBLISHED',(SELECT id FROM app_users WHERE username='chihuy'),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

INSERT INTO work_items
    (id,work_schedule_id,work_date,period,start_time,end_time,all_day,content,leader,
     participants_description,location,preparation,duty_leader,note,sort_order,created_at,updated_at)
VALUES
    (2101,2001,'2026-11-02','MORNING','06:30','07:00',FALSE,'Chào cờ đầu tuần','PCHT','Cơ quan, bDQTT','Sân trước Ban CHQS',NULL,'A1',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2102,2001,'2026-11-02','MORNING','07:00','07:30',FALSE,'Giao ban cơ quan','PCHT','Cán bộ cơ quan','Phòng giao ban',NULL,'A1',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2103,2001,'2026-11-02','MORNING','07:30','11:00',FALSE,'Luyện tập phương án đánh địch đột nhập đường không của tổ bộ binh bắn mục tiêu bay thấp bằng súng bộ binh','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'A1',NULL,3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2104,2001,'2026-11-02','AFTERNOON','13:30','17:00',FALSE,'Luyện tập phương án đánh địch đột nhập đường không của tổ bộ binh bắn mục tiêu bay thấp bằng súng bộ binh','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'A1',NULL,4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2105,2001,'2026-11-02','EVENING','18:30','19:00',FALSE,'Sinh hoạt tổ, tiểu đội','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'At',NULL,5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2106,2001,'2026-11-02','EVENING','19:00','21:00',FALSE,'Đọc báo, nghe tin, xem truyền hình','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'TBb: Châu',NULL,6,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2107,2001,'2026-11-03','MORNING','06:45','08:30',FALSE,'Học tập 7 nhiệm vụ DQTV','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'A2',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2108,2001,'2026-11-03','AFTERNOON','14:30','16:30',FALSE,'Họp chi bộ đánh giá chất lượng tổ chức đảng, đảng viên','Bí thư','Đảng viên chi bộ','Phòng giao ban',NULL,'A2',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2109,2001,'2026-11-03','EVENING','18:30','19:00',FALSE,'Sinh hoạt tổ, tiểu đội','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'At',NULL,3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2110,2001,'2026-11-03','EVENING','19:00','21:00',FALSE,'Đọc báo, nghe tin, xem truyền hình','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'TBb: Nhật',NULL,4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2111,2001,'2026-11-04','MORNING','07:00','07:30',FALSE,'Giao ban cơ quan','PCHT','Cán bộ cơ quan','Phòng giao ban',NULL,'A3',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2112,2001,'2026-11-04','ALL_DAY',NULL,NULL,TRUE,'Thực hiện các nhiệm vụ được phân công','bĐT','bDQTT','Ban CHQS',NULL,'bDQTT',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2113,2001,'2026-11-04','ALL_DAY',NULL,NULL,TRUE,'Tham gia Cuộc thi trực tuyến kỷ niệm 80 năm Ngày thành lập Đảng bộ Quân khu 5','CTVP','Toàn đơn vị','Phòng giao ban',NULL,'A3','Thời gian cuộc thi từ 26/10/2026 đến 15/11/2026.',3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2114,2001,'2026-11-04','EVENING','18:30','19:00',FALSE,'Sinh hoạt tổ, tiểu đội','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'At',NULL,4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2115,2001,'2026-11-04','EVENING','19:00','21:00',FALSE,'Sinh hoạt trung đội','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'TBb: Phước',NULL,5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2116,2001,'2026-11-05','MORNING','06:45','07:00',FALSE,'Học tập 10 lời thề danh dự','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'A1',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2117,2001,'2026-11-05','MORNING','07:00','07:30',FALSE,'Giao ban cơ quan','PCHT','Cán bộ cơ quan','Phòng giao ban',NULL,'A1',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2118,2001,'2026-11-05','AFTERNOON','16:30','18:00',FALSE,'Bảo quản vũ khí trang bị kỹ thuật','PCHT','Cơ quan, bDQTT','Kho vũ khí Ban CHQS phường',NULL,'bDQTT',NULL,3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2119,2001,'2026-11-05','EVENING','18:30','19:00',FALSE,'Sinh hoạt tổ, tiểu đội','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'At',NULL,4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2120,2001,'2026-11-05','EVENING','19:00','21:00',FALSE,'Đọc báo, nghe tin, xem truyền hình','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'TBb: H.Duy',NULL,5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2121,2001,'2026-11-06','MORNING','06:45','08:30',FALSE,'Huấn luyện điều lệnh đội ngũ: Đi đều, đứng lại, giậm chân, đổi chân trong khi đi','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'A2',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2122,2001,'2026-11-06','MORNING','07:00','07:30',FALSE,'Giao ban cơ quan','PCHT','Cán bộ cơ quan','Phòng giao ban',NULL,'A2',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2123,2001,'2026-11-06','AFTERNOON','13:30','16:30',FALSE,'Huấn luyện điều lệnh đội ngũ: Đi đều, đứng lại, giậm chân, đổi chân trong khi đi','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'A2',NULL,3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2124,2001,'2026-11-06','EVENING','18:30','19:00',FALSE,'Sinh hoạt tiểu đội','bĐT','bDQTT','Sân trước Ban CHQS',NULL,'At',NULL,4,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2125,2001,'2026-11-06','EVENING','19:00','21:00',FALSE,'Xem thời sự','bĐT','bDQTT','Phòng sinh hoạt tập trung',NULL,'TBb: Toàn',NULL,5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2126,2001,'2026-11-07','MORNING','10:00','11:30',FALSE,'Xem Chương trình truyền hình “Chiến sĩ – Đường tới vinh quang”','bĐT','bDQTT','Phòng giao ban',NULL,'A3',NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2127,2001,'2026-11-07','MORNING','07:00','09:00',FALSE,'Tổng dọn vệ sinh doanh trại','bĐT','bDQTT','Ban CHQS phường',NULL,'bDQTT',NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2128,2001,'2026-11-07','ALL_DAY',NULL,NULL,TRUE,'Thực hiện chế độ ngày nghỉ, duy trì trực SSCĐ thường xuyên','PCHT','Toàn cơ quan','Ban CHQS phường',NULL,'A3','TBb: Đ.Tâm',3,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (2129,2001,'2026-11-08','ALL_DAY',NULL,NULL,TRUE,'Thực hiện chế độ ngày nghỉ, duy trì trực SSCĐ thường xuyên','PCHT','Toàn cơ quan','Ban CHQS phường',NULL,'A1','TBb: Đạt',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

INSERT INTO work_item_assignments (work_item_id,soldier_id,created_at,updated_at)
SELECT wi.id,u.id,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM work_items wi CROSS JOIN app_users u
WHERE wi.work_schedule_id=2001 AND u.role='SOLDIER'
  AND (wi.participants_description ILIKE '%bDQTT%' OR wi.participants_description ILIKE '%Toàn đơn vị%'
       OR wi.participants_description ILIKE '%Toàn cơ quan%');

-- Lịch theo dõi địa bàn: hai buổi/ngày, đúng người và giờ trong biểu mẫu.
INSERT INTO work_items
    (id,work_schedule_id,work_date,period,start_time,end_time,all_day,content,leader,
     participants_description,location,preparation,duty_leader,note,sort_order,created_at,updated_at)
VALUES
    (3001,2002,'2026-11-02','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Lê Hữu Đạt, Võ Ngọc Sơn','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3002,2002,'2026-11-02','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Dương Đại Phát, Nguyễn Quốc Anh','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3003,2002,'2026-11-03','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Nguyễn Hải Đăng, Võ Thành Tâm','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3004,2002,'2026-11-03','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Trần Đình Hoàng Nhật, Thái Tấn Toàn','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3005,2002,'2026-11-04','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Trần Ngọc Sang, Phạm Hoàng Châu','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3006,2002,'2026-11-04','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Phan Văn Phước, Nguyễn Đức Huy','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3007,2002,'2026-11-05','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Lê Hữu Đạt, Trịnh Ngọc Duy','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3008,2002,'2026-11-05','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Dương Đại Phát, Thái Minh Trọng','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3009,2002,'2026-11-06','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Nguyễn Hải Đăng, Võ Thành Tâm','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3010,2002,'2026-11-06','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Trần Đình Hoàng Nhật, Thái Tấn Toàn','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3011,2002,'2026-11-07','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Võ Ngọc Sơn, Phạm Hoàng Châu','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3012,2002,'2026-11-07','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Nguyễn Quốc Anh, Nguyễn Đức Huy','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3013,2002,'2026-11-08','MORNING','06:30','07:30',FALSE,'Nắm tình hình trên địa bàn',NULL,'Trần Ngọc Sang, Trịnh Ngọc Duy','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
    (3014,2002,'2026-11-08','AFTERNOON','16:00','17:00',FALSE,'Nắm tình hình trên địa bàn',NULL,'Phan Văn Phước, Thái Minh Trọng','Phường Bắc Cam Ranh','Sổ nắm tình hình và phương tiện liên lạc',NULL,NULL,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

WITH area_assignments(item_id,username) AS (
    VALUES (3001,'chiensi01'),(3001,'chiensi02'),(3002,'chiensi11'),(3002,'chiensi12'),
      (3003,'chiensi05'),(3003,'chiensi03'),(3004,'chiensi06'),(3004,'chiensi13'),
      (3005,'chiensi07'),(3005,'chiensi04'),(3006,'chiensi08'),(3006,'chiensi14'),
      (3007,'chiensi01'),(3007,'chiensi09'),(3008,'chiensi11'),(3008,'chiensi10'),
      (3009,'chiensi05'),(3009,'chiensi03'),(3010,'chiensi06'),(3010,'chiensi13'),
      (3011,'chiensi02'),(3011,'chiensi04'),(3012,'chiensi12'),(3012,'chiensi14'),
      (3013,'chiensi07'),(3013,'chiensi09'),(3014,'chiensi08'),(3014,'chiensi10')
)
INSERT INTO work_item_assignments (work_item_id,soldier_id,created_at,updated_at)
SELECT a.item_id,u.id,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM area_assignments a JOIN app_users u ON u.username=a.username;

INSERT INTO notifications
    (recipient_id,title,content,type,reference_type,reference_id,event_key,created_at,updated_at)
SELECT u.id,'Đã phân công lịch gác tuần',
       'Đồng chí đã được phân công lịch gác cổng tuần 02/11/2026 - 08/11/2026.',
       'GUARD_ASSIGNED','GUARD_SHIFT',MIN(ga.guard_shift_id),
       'seed-guard-week-2026-11-02-user-'||u.id,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM app_users u JOIN guard_assignments ga ON ga.soldier_id=u.id
WHERE u.role='SOLDIER' GROUP BY u.id;

INSERT INTO activity_logs (actor_id,action,target_type,target_id,description,created_at)
VALUES
 ((SELECT id FROM app_users WHERE username='chihuy'),'WORK_SCHEDULE_CREATED','WORK_SCHEDULE',2001,'Đã nhập lịch công tác tuần 02/11/2026 - 08/11/2026 từ biểu mẫu.',CURRENT_TIMESTAMP),
 ((SELECT id FROM app_users WHERE username='chihuy'),'WORK_SCHEDULE_CREATED','WORK_SCHEDULE',2002,'Đã nhập lịch theo dõi tình hình địa bàn tuần 02/11/2026 - 08/11/2026 từ biểu mẫu.',CURRENT_TIMESTAMP),
 ((SELECT id FROM app_users WHERE username='chihuy'),'GUARD_CREATED','GUARD_SHIFT',100001,'Đã nhập lịch gác cổng gồm 84 ca theo biểu mẫu tuần.',CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('guard_shifts','id'),(SELECT MAX(id) FROM guard_shifts),TRUE);
SELECT setval(pg_get_serial_sequence('work_schedules','id'),(SELECT MAX(id) FROM work_schedules),TRUE);
SELECT setval(pg_get_serial_sequence('work_items','id'),(SELECT MAX(id) FROM work_items),TRUE);
COMMIT;
