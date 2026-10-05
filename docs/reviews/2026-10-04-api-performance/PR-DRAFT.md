# perf: giảm query lặp và phân trang luồng nhận hàng

Danh sách đơn hiện tải quan hệ theo từng đơn, còn luồng nhận hàng tải toàn bộ đơn của lịch trước khi hiển thị. Thay đổi gom các truy vấn liên quan theo trang, bổ sung endpoint phân trang và chuyển giao diện organizer sang tải 20 đơn/lần. Endpoint danh sách cũ vẫn hoạt động để backend có thể phát hành trước frontend.

Các thay đổi bổ sung: GET giỏ đã tồn tại không khóa user; popular chỉ tải ảnh cho top 10; cache categories có TTL và eviction sau commit; frontend dùng transport anonymous cho các GET catalog công khai; thêm bốn index đọc và metrics HTTP/Hikari chỉ ADMIN được truy cập. Không thay đổi kiểm tra quyền sở hữu, snapshot giá/tên, khóa checkout hoặc hợp đồng endpoint ghi.

## Validation

- Backend: 291 test được thống kê, 290 pass, 1 benchmark opt-in skipped; PostgreSQL thật, bao gồm migration V34 → V45, query counts, cache rollback/commit, cart concurrency và quyền truy cập.
- Frontend: 62 test pass; build pass; 92 Playwright test pass, bao gồm pagination/check-in trên desktop và mobile.
- Backend package pass. `git diff --check` pass.
- Benchmark chọn bean routing ứng dụng bằng qualifier khi có Actuator (`e3bf4bb`); đã chạy riêng với opt-in bật.
- Benchmark trước/sau, dữ liệu thô và các giới hạn phép đo: [OPTIMIZATION-RESULTS.md](OPTIMIZATION-RESULTS.md).

## Review và phát hành

- Review diff so với local `main` tại `f070580`, không coi 28 commit local đã tồn tại trước nhiệm vụ là thay đổi tối ưu mới.
- Branch bugfix chứa `3a44d76`; branch tối ưu chứa benchmark `b6c96cc`, backend `9ca461f`, frontend `42edd7e` và tài liệu kết quả.
- GitNexus trước commit backend: CRITICAL, 35 execution flows; frontend: HIGH, 10 flows. Các phạm vi này đã được kiểm tra bằng regression tests và toàn bộ suite.
- V44/V45 dùng concurrent index ngoài transaction; Flyway cần direct/session connection và quyền tạo `pg_trgm`. Kiểm chứng staging trước production.
- Backend trước, frontend sau; rollback frontend trước nếu cần. Cache categories theo instance có thể trễ tối đa 60 giây giữa các instance.
- Chi tiết migration, metrics, rollback và tiêu chí nghiệm thu: [ROLLOUT.md](ROLLOUT.md).
- Cần kiểm chứng thêm vòng đời SSE: các lần đo cả baseline/bản mới ghi nhận lỗi security khi đóng stream đã committed. HTTP 200 lúc mở stream chưa phải kiểm thử disconnect/timeout/reconnect; chưa nghiệm thu realtime hoặc capacity production.

Đây là nội dung PR đã chuẩn bị tại local; chưa tạo PR remote, push, merge hoặc deploy. Baseline tích hợp cần được thống nhất trước khi mở PR so với remote.
