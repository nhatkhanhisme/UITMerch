-- Opt-in demo data, outside Flyway. Existing rows are never reset or deleted.
BEGIN;
INSERT INTO users (id,email,password_hash,full_name,phone,role,is_verified,is_active) VALUES
 ('f0100000-0000-4000-8000-000000000001','demo.customer@uitmerch.test',{{DEMO_PASSWORD_HASH}},'Khách hàng Demo UIT','0900000001','CUSTOMER',true,true),
 ('f0100000-0000-4000-8000-000000000002','demo.organizer@uitmerch.test',{{DEMO_PASSWORD_HASH}},'Người tổ chức Demo UIT','0900000002','ORGANIZER',true,true),
 ('f0100000-0000-4000-8000-000000000003','demo.admin@uitmerch.test',{{DEMO_PASSWORD_HASH}},'Quản trị Demo UIT','0900000003','ADMIN',true,true),
 ('f0100000-0000-4000-8000-000000000004','demo.inactive@uitmerch.test',{{DEMO_PASSWORD_HASH}},'Tài khoản Demo chưa xác minh','0900000004','CUSTOMER',false,false)
ON CONFLICT DO NOTHING;
INSERT INTO organizations (id,owner_id,name,description,status,logo_url) VALUES
 ('f0200000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000002','UIT Creative Lab · Demo','Không gian demo để thử theo dõi, vật phẩm miễn phí, lịch nhận và mở đặt trước.','ACTIVE','https://placehold.co/300x300/e7f5fa/155e75?text=UIT+Creative'),
 ('f0200000-0000-4000-8000-000000000002','f0100000-0000-4000-8000-000000000002','UIT Art Studio · Demo','Bộ sưu tập sinh viên và hoạt động nghệ thuật để thử tính năng theo dõi.','ACTIVE','https://placehold.co/300x300/fff7ed/9a3412?text=UIT+Art'),
 ('f0200000-0000-4000-8000-000000000003','f0100000-0000-4000-8000-000000000002','CLB Demo chờ duyệt','Dùng để thử duyệt hoặc từ chối tổ chức từ admin.','PENDING',NULL)
ON CONFLICT DO NOTHING;
INSERT INTO merch_items (id,org_id,name,description,price,stock,status,publication_announced) VALUES
 ('f0300000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Sticker UIT sáng tạo · Demo','Vật phẩm miễn phí, nhận tại trường. Dùng để thử tạo đơn giá 0đ.',0,100,'PUBLISHED',true),
 ('f0300000-0000-4000-8000-000000000002','f0200000-0000-4000-8000-000000000001','Túi vải Campus · Demo','Vật phẩm hết hàng để thử đăng ký nhắc khi có hàng trở lại.',70000,0,'PUBLISHED',true),
 ('f0300000-0000-4000-8000-000000000003','f0200000-0000-4000-8000-000000000001','Áo Campus Stories · Demo — M','Phiên bản M, chỉ giữ chỗ qua đợt mở đặt trước đang hoạt động.',150000,38,'PUBLISHED',true),
 ('f0300000-0000-4000-8000-000000000004','f0200000-0000-4000-8000-000000000001','Áo Campus Stories · Demo — L','Phiên bản L, chỉ giữ chỗ qua đợt mở đặt trước đang hoạt động.',150000,40,'PUBLISHED',true),
 ('f0300000-0000-4000-8000-000000000005','f0200000-0000-4000-8000-000000000002','Túi Canvas Art · Demo','Phiên bản dùng cho đợt mở đặt trước của Art Studio.',90000,30,'PUBLISHED',true),
 ('f0300000-0000-4000-8000-000000000006','f0200000-0000-4000-8000-000000000001','Sổ tay UIT · Demo','Vật phẩm cho các đơn thường và đợt đặt trước đã đạt mục tiêu.',50000,75,'PUBLISHED',true)
ON CONFLICT DO NOTHING;
INSERT INTO merch_images (id,merch_id,url,position)
SELECT ('f0900000-0000-4000-8000-'||lpad(n::text,12,'0'))::uuid,
 ('f0300000-0000-4000-8000-'||lpad(n::text,12,'0'))::uuid,
 'https://placehold.co/600x600/e7f5fa/155e75?text=UIT+Demo+'||n,0
FROM generate_series(1,6) n ON CONFLICT DO NOTHING;
INSERT INTO events (id,org_id,title,description,status,starts_at,ends_at,cover_url,publication_announced) VALUES
 ('f0400000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Ngày hội Sáng tạo UIT · Demo',E'Gặp gỡ cộng đồng sinh viên, thử vật phẩm mới và nhận sticker miễn phí.\n\nĐịa điểm: Phòng B101, UIT.\nMang mã đơn hoặc mã QR nếu bạn đã đặt vật phẩm. Đây là sự kiện demo.', 'PUBLISHED', CURRENT_DATE+7+TIME '09:00',CURRENT_DATE+7+TIME '16:00','https://placehold.co/1200x500/e7f5fa/155e75?text=UIT+Creative+Day',true),
 ('f0400000-0000-4000-8000-000000000002','f0200000-0000-4000-8000-000000000002','Workshop Thiết kế · Demo','Một sự kiện đã kết thúc để thử trạng thái và trang chi tiết.', 'ENDED', CURRENT_DATE-7+TIME '09:00',CURRENT_DATE-7+TIME '12:00',NULL,true)
ON CONFLICT DO NOTHING;
INSERT INTO event_merch(event_id,merch_id) VALUES
 ('f0400000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000001'),
 ('f0400000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000006') ON CONFLICT DO NOTHING;
INSERT INTO preorder_campaigns (id,org_id,title,description,minimum_quantity,deadline,state,closed_quantity,created_at) VALUES
 ('f0500000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Campus Stories — Mặc dấu ấn UIT','Bộ sưu tập áo sinh viên mở đặt trước. Giữ chỗ phiên bản M hoặc L; đạt đủ 10 áo để triển khai.',10,NOW()+INTERVAL '14 days','ACTIVE',NULL,NOW()),
 ('f0500000-0000-4000-8000-000000000002','f0200000-0000-4000-8000-000000000002','Canvas Days — Mang theo sáng tạo','Bộ sưu tập túi vải Art Studio. Cùng đạt mốc 5 chiếc để mở đợt sản xuất.',5,NOW()+INTERVAL '21 days','ACTIVE',NULL,NOW()),
 ('f0500000-0000-4000-8000-000000000003','f0200000-0000-4000-8000-000000000001','UIT Notes — Đã sẵn sàng trao tay','Đợt demo đã đạt mục tiêu; người tổ chức có thể xác nhận đơn và xếp lịch nhận.',1,NOW()-INTERVAL '1 day','SUCCEEDED',1,NOW()-INTERVAL '7 days')
ON CONFLICT DO NOTHING;
INSERT INTO campaign_variants (id,campaign_id,merch_id,label,unit_price) VALUES
 ('f0510000-0000-4000-8000-000000000001','f0500000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000003','Áo Campus Stories — M',150000),
 ('f0510000-0000-4000-8000-000000000002','f0500000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000004','Áo Campus Stories — L',150000),
 ('f0510000-0000-4000-8000-000000000003','f0500000-0000-4000-8000-000000000002','f0300000-0000-4000-8000-000000000005','Canvas Art',90000),
 ('f0510000-0000-4000-8000-000000000004','f0500000-0000-4000-8000-000000000003','f0300000-0000-4000-8000-000000000006','Sổ UIT Notes',50000)
ON CONFLICT DO NOTHING;
INSERT INTO pickup_schedules (id,org_id,pickup_date,pickup_time_slot,location,notes) VALUES
 ('f0600000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001',CURRENT_DATE+3,'09:00–11:00','Phòng B101, UIT','Mang mã QR nhận hàng. Đây là lịch nhận demo.') ON CONFLICT DO NOTHING;
INSERT INTO orders (id,user_id,org_id,guest_name,guest_phone,total_amount,status,payment_method,payment_status,pickup_schedule_id,cancel_reason,cancelled_by) VALUES
 ('f0700000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',0,'READY','CASH_ON_DELIVERY','PENDING','f0600000-0000-4000-8000-000000000001',NULL,NULL),
 ('f0700000-0000-4000-8000-000000000002','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',50000,'PENDING','CASH_ON_DELIVERY','PENDING',NULL,NULL,NULL),
 ('f0700000-0000-4000-8000-000000000003','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',50000,'CONFIRMED','CASH_ON_DELIVERY','PENDING',NULL,NULL,NULL),
 ('f0700000-0000-4000-8000-000000000004','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',50000,'COMPLETED','CASH_ON_DELIVERY','PAID',NULL,NULL,NULL),
 ('f0700000-0000-4000-8000-000000000005','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',50000,'CANCELLED','CASH_ON_DELIVERY','PENDING',NULL,'Tôi không còn nhu cầu nữa','customer'),
 ('f0700000-0000-4000-8000-000000000006','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',300000,'PENDING','CASH_ON_DELIVERY','PENDING',NULL,NULL,NULL),
 ('f0700000-0000-4000-8000-000000000007','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001','Khách hàng Demo UIT','0900000001',50000,'PENDING','CASH_ON_DELIVERY','PENDING',NULL,NULL,NULL)
ON CONFLICT DO NOTHING;
INSERT INTO order_items (id,order_id,merch_id,merch_name,unit_price,quantity,subtotal)
SELECT ('f0710000-0000-4000-8000-'||lpad(n::text,12,'0'))::uuid,
 ('f0700000-0000-4000-8000-'||lpad(n::text,12,'0'))::uuid,
 CASE WHEN n=1 THEN 'f0300000-0000-4000-8000-000000000001'::uuid WHEN n=6 THEN 'f0300000-0000-4000-8000-000000000003'::uuid ELSE 'f0300000-0000-4000-8000-000000000006'::uuid END,
 CASE WHEN n=1 THEN 'Sticker UIT sáng tạo · Demo' WHEN n=6 THEN 'Áo Campus Stories · Demo — M' ELSE 'Sổ tay UIT · Demo' END,
 CASE WHEN n=1 THEN 0 WHEN n=6 THEN 150000 ELSE 50000 END,
 CASE WHEN n=6 THEN 2 ELSE 1 END,
 CASE WHEN n=1 THEN 0 WHEN n=6 THEN 300000 ELSE 50000 END
FROM generate_series(1,7) n ON CONFLICT DO NOTHING;
INSERT INTO campaign_reservations(id,campaign_id,user_id,merch_id,order_id,quantity,request_id,created_at) VALUES
 ('f0520000-0000-4000-8000-000000000001','f0500000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000003','f0700000-0000-4000-8000-000000000006',2,'f0530000-0000-4000-8000-000000000001',NOW()),
 ('f0520000-0000-4000-8000-000000000002','f0500000-0000-4000-8000-000000000003','f0100000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000006','f0700000-0000-4000-8000-000000000007',1,'f0530000-0000-4000-8000-000000000002',NOW()-INTERVAL '2 days') ON CONFLICT DO NOTHING;
INSERT INTO organization_follows(id,user_id,org_id,enabled,notify_merch,notify_events,email_enabled,followed_at) VALUES
 ('f0800000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001',true,true,true,false,NOW()),
 ('f0800000-0000-4000-8000-000000000002','f0100000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000002',true,true,false,false,NOW()) ON CONFLICT DO NOTHING;
INSERT INTO restock_subscriptions(id,user_id,merch_id,enabled,email_enabled,subscribed_at) VALUES
 ('f0810000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','f0300000-0000-4000-8000-000000000002',true,false,NOW()) ON CONFLICT DO NOTHING;
INSERT INTO order_history(id,order_id,actor_id,from_status,to_status,source,pickup_schedule_id,created_at) VALUES
 ('f0820000-0000-4000-8000-000000000001','f0700000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','PENDING','PENDING','INSTANT',NULL,NOW()-INTERVAL '2 days'),
 ('f0820000-0000-4000-8000-000000000002','f0700000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000002','PENDING','CONFIRMED','ORGANIZER',NULL,NOW()-INTERVAL '1 day'),
 ('f0820000-0000-4000-8000-000000000003','f0700000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000002','CONFIRMED','READY','PICKUP_SCHEDULE','f0600000-0000-4000-8000-000000000001',NOW()) ON CONFLICT DO NOTHING;
INSERT INTO notifications(id,user_id,title,message,type,is_read,related_order_id,related_org_id) VALUES
 ('f0830000-0000-4000-8000-000000000001','f0100000-0000-4000-8000-000000000001','Đơn demo đã sẵn sàng nhận','Mở đơn để xem lịch nhận và tạo mã QR.','PICKUP_SCHEDULED',false,'f0700000-0000-4000-8000-000000000001','f0200000-0000-4000-8000-000000000001'),
 ('f0830000-0000-4000-8000-000000000002','f0100000-0000-4000-8000-000000000001','Đặt trước Campus Stories thành công','Bạn đã giữ chỗ 2 áo. Đợt đặt trước đang chờ đạt đủ mục tiêu.','ORDER_PLACED',false,'f0700000-0000-4000-8000-000000000006','f0200000-0000-4000-8000-000000000001'),
 ('f0830000-0000-4000-8000-000000000003','f0100000-0000-4000-8000-000000000001','Đơn demo đã hoàn thành','Thông báo này đã được đọc để so sánh trạng thái.','ORDER_COMPLETED',true,'f0700000-0000-4000-8000-000000000004','f0200000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;
COMMIT;
