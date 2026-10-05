# Kết quả tối ưu API — 05/10/2026

Đã triển khai trên `feature/backend-api-performance`, tách bản sửa tra cứu đơn ở `bugfix/backend-order-dispatch`. Backend `9ca461f`, frontend `42edd7e`, bổ sung qualifier cho benchmark khi có Actuator `e3bf4bb`. Các thay đổi đã được kiểm chứng tại local; chưa push, merge hoặc deploy. [Runbook](ROLLOUT.md), [nội dung PR](PR-DRAFT.md).

## Cặp đo chính và giới hạn

So sánh **103 route trước, 104 route sau**, gồm 8.130 và 8.160 request có số đo, tổng **16.290 mẫu HTTP**. Tất cả mẫu ghi nhận đều 2xx, không lỗi transport; seed counts giống nhau và sample counts/p95 được tính lại từ raw samples. Route mới là pagination nhận hàng.

Đo tuần tự từ hai detached worktree sạch tại `b6c96cc` (V43) và `e3bf4bb` (V45). Không bao gồm các chỉnh sửa AI/test đang dở ở workspace chính. PostgreSQL 17.11 disposable trong Docker tmpfs, Java 21.0.12.1, HTTP/1.1 loopback, Intel i9-10885H 8 core/16 thread. Hikari 5/1; WARN logging; Hibernate statistics bật; không chạy build/browser test/index analysis đồng thời trong cửa sổ đo. Workstation không có CPU isolation. [Environment](validation/environment.json).

Mỗi route có 5 warmup + 30 mẫu; pickup-token customer và visual-search chỉ 15 mẫu để giữ rate limiter thật. Tải closed loop: 6 API × concurrency 1/5/10/20 × 200 request/case, private routes dùng cùng user. Kiểm tra page size 1/20/100: 30 mẫu/case. Chuẩn bị fixture nằm ngoài vùng timing; database vẫn thay đổi qua các bài ghi.

Mail, storage, Vision/Embedding/vector provider được stub; Spring Security/BCrypt, DB, transaction, serializer, outbox và keyword fallback chạy thật. Scheduler tắt. SSE chỉ đo đến dòng đầu. p99 của 15/30 mẫu là giá trị cực đại; đây là benchmark sàng lọc, không phải capacity/tail latency production. Framework Actuator không nằm trong inventory controller ứng dụng; quyền metrics được test riêng.

SQL trong bảng là số Hibernate prepared statements ở request tuần tự, có cả auth. Khi tải đồng thời, đếm statements theo request bị tắt; SQL=0 trong raw load không có nghĩa không truy vấn database.

## Kết quả các thay đổi trọng tâm

| API / page size | p95 trước (ms) | p95 sau (ms) | SQL trước → sau |
|---|---:|---:|---:|
| `/api/v1/customer/orders`, size=1 | 6.75 | 6.24 | 7 → 7 |
| `/api/v1/customer/orders`, size=20 | 9.82 | 6.18 | 45 → 7 |
| `/api/v1/customer/orders`, size=100 | 19.06 | 8.32 | 140 → 7 |
| `/api/v1/admin/orders`, size=1 | 6.31 | 6.03 | 6 → 6 |
| `/api/v1/admin/orders`, size=20 | 8.98 | 6.95 | 25 → 6 |
| `/api/v1/admin/orders`, size=100 | 16.59 | 10.12 | 105 → 6 |
| `/api/v1/organizations/{orgId}/pickup-schedules/{scheduleId}/orders` | 167.78 | 37.01 | 1007 → 12 |
| `/api/v1/organizations/{orgId}/pickup-schedules/{scheduleId}/orders/page` | mới | 8.26 | mới → 8 |
| `/api/v1/categories` | 4.48 | 4.84 | 1 → 0 |

Legacy vẫn trả 1.001 đơn (~988 KB); endpoint mới trả 20 đơn (~20 KB), giảm payload 98.0% khi mở trang đầu. Đây là đổi cách tải qua endpoint mới, không phải hai response chứa cùng số đơn. Metadata, trang cuối, ownership và check-in được test.

Batch order items/distinct schedules làm query count theo trang không còn tăng tuyến tính với số đơn. Query IN legacy chia nhóm 200 ID. Test 25 pickup schedules kiểm chứng một aggregate count thay cho query từng lịch; fixture HTTP chỉ có ít lịch nên không dùng latency của nó để chứng minh tốc độ ở 25 lịch.

Categories dùng DTO cache, TTL 60 giây, eviction sau commit; test rollback/commit pass. GET giỏ đã có bỏ user lock, còn tạo/reactivate/write/checkout giữ khóa/recheck. Popular chỉ tải ảnh của top 10 thay vì tối đa 500 ứng viên; unit test kiểm chứng đúng ID. Rank/giá/tồn kho vẫn lấy dữ liệu hiện hành; không cache DTO popular.

## Tải 20 client đồng thời

| API | p95 trước (ms) | p95 sau (ms) | RPS trước | RPS sau |
|---|---:|---:|---:|---:|
| `/api/v1/public/merch` | 26.17 | 30.75 | 933 | 922 |
| `/api/v1/public/merch/popular` | 139.67 | 67.14 | 229 | 388 |
| `/api/v1/customer/orders` | 70.23 | 41.39 | 369 | 649 |
| `/api/v1/customer/cart` | 63.34 | 35.39 | 469 | 807 |
| `/api/v1/organizations/{orgId}/analytics` | 45.95 | 56.66 | 596 | 483 |
| `/api/v1/public/orders/{orderId}` | 4.84 | 11.19 | 6306 | 2932 |

## Biến động, regression và bằng chứng bổ sung

Giữ các lần đo đã lưu ở phiên trước để tránh chọn kết quả đẹp: baseline [trước 1](validation/before-1.json), [sau 1](validation/after-1.json), [sau 2](validation/after-2.json). Một baseline lặp ở phiên cũ đã hoàn thành nhưng raw file trong `/tmp` mất khi phiên reset; không dùng lần đó để tính số liệu. Cặp đo chính ở trên được chạy lại trong cùng phiên và lưu bền vững.

| API, concurrency=20, phiên trước | p95 baseline (ms) | p95 sau lần 1 (ms) | p95 sau lần 2 (ms) |
|---|---:|---:|---:|
| `/api/v1/public/merch` | 25.69 | 88.90 | 24.54 |
| `/api/v1/public/merch/popular` | 112.22 | 82.95 | 99.53 |
| `/api/v1/customer/orders` | 85.62 | 44.33 | 48.56 |
| `/api/v1/customer/cart` | 63.46 | 34.17 | 32.58 |
| `/api/v1/organizations/{orgId}/analytics` | 44.43 | 203.86 | 48.67 |
| `/api/v1/public/orders/{orderId}` | 6.18 | 12.53 | 9.67 |

Các số load của catalog/analytics biến động rõ giữa lần đo; query giảm và giảm dữ liệu tải không đồng nghĩa mọi p95 đều giảm. Có chi phí observability mới; chưa có profile CPU/GC/security đủ để quy nguyên nhân regression. Không khẳng định đã tối ưu latency toàn hệ thống.

Baseline admin orders ở đầu cặp đo chính có p95 345,18 ms, trong khi baseline phiên trước là 46,24 ms. Warmup ngắn không loại hết biến động khởi động/JIT/GC; không dùng tỷ lệ 345 → 40 để khẳng định mức tăng tốc thuật toán. Bài page-size ở cuối lượt đo và query counts là bằng chứng phù hợp hơn cho thay đổi batch.

Cặp đo chính ghi nhận **6 route trong bài tuần tự tăng p95 >20% và >2 ms**. Các route tăng nhiều nhất theo ms ở dưới; các regression ở tải đồng thời có bảng riêng phía trên. Toàn bộ route và case tải vẫn có trong CSV. Nghiệm thu staging cần kiểm tra các trường hợp này trước khi phát hành.

| API | p95 trước (ms) | p95 sau (ms) | Tăng |
|---|---:|---:|---:|
| POST `/api/v1/customer/wishlist/{merchId}` | 15.29 | 23.45 | 53.4% |
| PATCH `/api/v1/customer/orders/{id}/cancel` | 12.21 | 16.70 | 36.8% |
| GET `/api/v1/organizations/{orgId}/events/{id}` | 10.38 | 14.19 | 36.7% |
| GET `/api/v1/customer/orders/{orderId}/campaign-context` | 8.02 | 11.09 | 38.3% |
| POST `/api/v1/organizations/{orgId}/campaigns/{id}/cancel` | 9.98 | 12.74 | 27.6% |
| POST `/api/v1/organizations` | 8.05 | 10.26 | 27.4% |

**SSE chưa được nghiệm thu vòng đời:** log cả baseline/bản mới có `AccessDeniedException` sau khi đóng stream đã committed. HTTP 200 của dòng đầu không chứng minh disconnect/timeout/reconnect hoặc async-dispatch đúng. Cần issue và integration test riêng cho context/dispatch trước nghiệm thu realtime; không mở quyền ASYNC toàn cục chỉ để làm sạch benchmark. Chi phí log khi đóng stream cũng có thể làm nhiễu timing.

## Kiểm chứng và phát hành

- Backend full suite: 290 pass, 1 benchmark opt-in skipped (291 tổng), PostgreSQL thật; backend package pass. Benchmark bật riêng đã pass trên cả hai source snapshot. [Test summary](validation/test-summary.json).
- [JUnit benchmark và số lần xuất hiện lỗi SSE trong log](validation/benchmark-test-summary.json) được lưu riêng; log errors không bị gọi là không có chỉ vì HTTP samples trả 2xx.
- Frontend: 62 test pass; build pass; 92 Playwright pass. [Desktop pagination](validation/pickup-pagination-1440.png), [mobile pagination](validation/pickup-pagination-375.png).
- Regression coverage: V34 → V45 migration, metadata/legacy contract, quyền customer/org/ADMIN metrics, snapshot tên/giá, cache rollback/commit, đọc/tạo giỏ song song và checkout races hiện có.
- Prepared search test trên 30.000 merch bổ sung chạy bind query qua ngưỡng automatic planning, ghi cả automatic/forced-generic plan. Sau VACUUM/ANALYZE, cả hai chọn GIN trong fixture (~0,88–0,90 ms EXPLAIN). Đây là SQL của một keyword chọn lọc, không phải HTTP latency hay bảo đảm mọi keyword dùng index. [Plans](validation/prepared-search-plan.json).
- GitNexus trước commit backend: CRITICAL/35 flows, frontend: HIGH/10 flows, đã cảnh báo/review với tests. [Backend scope](validation/backend-change-scope.txt), [frontend scope](validation/frontend-change-scope.txt). Static graph không thay thế runtime/security/migration tests.

Trước production: thống nhất baseline tích hợp (local main ahead remote 28 commit), PR/CI, migration direct/session connection và `pg_trgm` trên staging. Chạy tải arrival-rate nhiều user/SKU ≥5–10 phút/mức và soak 30–60 phút; đo DB RTT/slow queries, Hikari wait/timeouts, HTTP p95/error, CPU/GC, job age và provider thật. Xác minh regression và SSE. Chọn pool theo quota/số liệu; chưa tăng pool production hoặc thay hạ tầng trong lượt này. Backend phát hành trước frontend theo [runbook](ROLLOUT.md).

## Dữ liệu đầy đủ

- Raw so sánh chính: [trước](validation/before-final.json), [sau](validation/after-final.json), [environment](validation/environment.json).
- CSV: [104 API](validation/api-comparison.csv), [24 case tải](validation/load-comparison.csv), [9 case page size](validation/page-size-comparison.csv).
- Lệnh tái lập: `bash backend/scripts/performance-audit.sh` từ từng commit. SQL DDL experiments ở baseline chạy sau mọi cửa sổ HTTP, không dùng để suy luận latency API; bản mới mặc định tắt chúng.

| Method | Route | p95 trước (ms) | p95 sau (ms) | SQL trước → sau |
|---|---|---:|---:|---:|
| GET | `/` | 14.44 | 8.03 | 0.0 → 0.0 |
| GET | `/api/v1/admin/orders` | 345.18 | 40.13 | 25.0 → 6.0 |
| GET | `/api/v1/admin/organizations` | 34.84 | 18.68 | 5.0 → 5.0 |
| PATCH | `/api/v1/admin/organizations/{id}/status` | 21.77 | 23.08 | 7.0 → 7.0 |
| GET | `/api/v1/admin/users` | 16.38 | 18.37 | 5.0 → 5.0 |
| PATCH | `/api/v1/admin/users/{id}/active` | 14.21 | 13.01 | 5.0 → 5.0 |
| PATCH | `/api/v1/admin/users/{id}/role` | 10.25 | 11.88 | 5.0 → 5.0 |
| POST | `/api/v1/auth/forgot-password` | 10.45 | 12.14 | 5.0 → 5.0 |
| POST | `/api/v1/auth/login` | 85.00 | 79.23 | 3.0 → 3.0 |
| POST | `/api/v1/auth/logout` | 11.88 | 11.01 | 6.0 → 6.0 |
| POST | `/api/v1/auth/refresh` | 12.99 | 11.11 | 5.0 → 5.0 |
| POST | `/api/v1/auth/register` | 148.45 | 85.04 | 6.0 → 6.0 |
| POST | `/api/v1/auth/register/organizer` | 126.27 | 79.74 | 6.0 → 6.0 |
| POST | `/api/v1/auth/resend-otp` | 12.46 | 9.66 | 5.0 → 5.0 |
| POST | `/api/v1/auth/reset-password` | 95.99 | 83.27 | 4.0 → 4.0 |
| POST | `/api/v1/auth/verify-email` | 11.22 | 7.94 | 4.0 → 4.0 |
| GET | `/api/v1/categories` | 4.48 | 4.84 | 1.0 → 0.0 |
| GET | `/api/v1/customer/campaign-reservations` | 10.55 | 10.24 | 4.0 → 4.0 |
| POST | `/api/v1/customer/campaigns/{id}/reservations` | 34.46 | 27.73 | 20.0 → 20.0 |
| GET | `/api/v1/customer/cart` | 23.99 | 18.63 | 8.0 → 7.0 |
| POST | `/api/v1/customer/cart/checkout` | 116.04 | 73.23 | 76.0 → 76.0 |
| POST | `/api/v1/customer/cart/items` | 32.93 | 19.75 | 11.0 → 11.0 |
| DELETE | `/api/v1/customer/cart/items/{itemId}` | 11.30 | 11.66 | 7.0 → 7.0 |
| PATCH | `/api/v1/customer/cart/items/{itemId}` | 32.63 | 15.07 | 11.0 → 11.0 |
| GET | `/api/v1/customer/following` | 14.87 | 9.77 | 4.0 → 4.0 |
| DELETE | `/api/v1/customer/following/{orgId}` | 20.86 | 10.58 | 6.0 → 6.0 |
| PATCH | `/api/v1/customer/following/{orgId}` | 19.44 | 9.69 | 5.0 → 5.0 |
| POST | `/api/v1/customer/following/{orgId}` | 24.05 | 11.22 | 6.0 → 6.0 |
| GET | `/api/v1/customer/notifications` | 37.98 | 12.91 | 5.0 → 5.0 |
| PATCH | `/api/v1/customer/notifications/read-all` | 13.52 | 7.11 | 4.0 → 4.0 |
| GET | `/api/v1/customer/notifications/stream` | 11.72 | 9.92 | 3.0 → 3.0 |
| GET | `/api/v1/customer/notifications/unread-count` | 10.85 | 8.20 | 4.0 → 4.0 |
| PATCH | `/api/v1/customer/notifications/{id}/read` | 9.21 | 9.18 | 5.0 → 5.0 |
| GET | `/api/v1/customer/orders` | 26.83 | 18.07 | 25.0 → 6.0 |
| POST | `/api/v1/customer/orders/instant` | 18.61 | 18.92 | 12.0 → 12.0 |
| GET | `/api/v1/customer/orders/{id}` | 11.21 | 11.73 | 6.0 → 6.0 |
| PATCH | `/api/v1/customer/orders/{id}/cancel` | 12.21 | 16.70 | 16.0 → 16.0 |
| GET | `/api/v1/customer/orders/{orderId}/campaign-context` | 8.02 | 11.09 | 5.0 → 5.0 |
| GET | `/api/v1/customer/orders/{orderId}/history` | 8.54 | 8.92 | 5.0 → 5.0 |
| POST | `/api/v1/customer/orders/{orderId}/pickup-token` | 8.34 | 9.99 | 9.0 → 9.0 |
| GET | `/api/v1/customer/profile` | 6.76 | 8.34 | 4.0 → 4.0 |
| PATCH | `/api/v1/customer/profile` | 6.21 | 6.68 | 4.0 → 4.0 |
| GET | `/api/v1/customer/restock-subscriptions` | 6.97 | 8.19 | 4.0 → 4.0 |
| POST | `/api/v1/customer/restock-subscriptions` | 9.86 | 9.05 | 6.0 → 6.0 |
| DELETE | `/api/v1/customer/restock-subscriptions/{merchId}` | 8.88 | 7.17 | 5.0 → 5.0 |
| GET | `/api/v1/customer/wishlist` | 11.51 | 9.97 | 7.0 → 7.0 |
| DELETE | `/api/v1/customer/wishlist/{merchId}` | 11.11 | 8.08 | 8.0 → 8.0 |
| POST | `/api/v1/customer/wishlist/{merchId}` | 15.29 | 23.45 | 13.0 → 13.0 |
| GET | `/api/v1/dev/otps` | 6.20 | 5.39 | 2.0 → 2.0 |
| POST | `/api/v1/organizations` | 8.05 | 10.26 | 4.0 → 4.0 |
| GET | `/api/v1/organizations/mine` | 11.51 | 13.47 | 7.0 → 7.0 |
| PATCH | `/api/v1/organizations/{id}` | 12.48 | 10.80 | 6.0 → 6.0 |
| GET | `/api/v1/organizations/{orgId}/analytics` | 17.94 | 14.74 | 4.0 → 4.0 |
| GET | `/api/v1/organizations/{orgId}/campaigns` | 10.03 | 9.34 | 5.0 → 5.0 |
| POST | `/api/v1/organizations/{orgId}/campaigns` | 11.20 | 12.44 | 12.0 → 12.0 |
| GET | `/api/v1/organizations/{orgId}/campaigns/{campaignId}` | 13.09 | 9.38 | 7.0 → 7.0 |
| POST | `/api/v1/organizations/{orgId}/campaigns/{id}/cancel` | 9.98 | 12.74 | 11.0 → 11.0 |
| GET | `/api/v1/organizations/{orgId}/events` | 13.43 | 8.35 | 6.0 → 6.0 |
| POST | `/api/v1/organizations/{orgId}/events` | 12.51 | 8.31 | 10.0 → 10.0 |
| DELETE | `/api/v1/organizations/{orgId}/events/{id}` | 6.71 | 8.47 | 6.0 → 6.0 |
| GET | `/api/v1/organizations/{orgId}/events/{id}` | 10.38 | 14.19 | 8.0 → 8.0 |
| PATCH | `/api/v1/organizations/{orgId}/events/{id}` | 22.26 | 15.89 | 10.0 → 10.0 |
| POST | `/api/v1/organizations/{orgId}/events/{id}/merch` | 17.84 | 17.81 | 12.0 → 12.0 |
| DELETE | `/api/v1/organizations/{orgId}/events/{id}/merch/{merchId}` | 9.46 | 9.83 | 8.0 → 8.0 |
| GET | `/api/v1/organizations/{orgId}/merchs` | 12.16 | 13.00 | 7.0 → 7.0 |
| POST | `/api/v1/organizations/{orgId}/merchs` | 8.08 | 8.73 | 6.0 → 6.0 |
| DELETE | `/api/v1/organizations/{orgId}/merchs/{id}` | 8.58 | 6.85 | 6.0 → 6.0 |
| GET | `/api/v1/organizations/{orgId}/merchs/{id}` | 14.84 | 8.41 | 6.0 → 6.0 |
| PATCH | `/api/v1/organizations/{orgId}/merchs/{id}` | 13.31 | 14.73 | 12.0 → 12.0 |
| GET | `/api/v1/organizations/{orgId}/orders` | 22.12 | 12.03 | 26.0 → 7.0 |
| POST | `/api/v1/organizations/{orgId}/orders/pickup/checkin` | 26.36 | 13.09 | 18.0 → 18.0 |
| POST | `/api/v1/organizations/{orgId}/orders/pickup/verify` | 25.32 | 7.90 | 8.0 → 8.0 |
| GET | `/api/v1/organizations/{orgId}/orders/{id}` | 7.68 | 6.79 | 7.0 → 7.0 |
| PATCH | `/api/v1/organizations/{orgId}/orders/{id}/cancel` | 14.68 | 9.56 | 15.0 → 15.0 |
| PATCH | `/api/v1/organizations/{orgId}/orders/{id}/checkin` | 18.89 | 8.88 | 14.0 → 14.0 |
| PATCH | `/api/v1/organizations/{orgId}/orders/{id}/status` | 14.07 | 12.55 | 13.0 → 13.0 |
| GET | `/api/v1/organizations/{orgId}/orders/{orderId}/campaign-context` | 10.30 | 7.03 | 6.0 → 6.0 |
| GET | `/api/v1/organizations/{orgId}/orders/{orderId}/history` | 7.41 | 8.31 | 6.0 → 6.0 |
| GET | `/api/v1/organizations/{orgId}/pickup-schedules` | 9.09 | 7.83 | 6.0 → 6.0 |
| POST | `/api/v1/organizations/{orgId}/pickup-schedules` | 9.37 | 9.03 | 13.0 → 13.0 |
| GET | `/api/v1/organizations/{orgId}/pickup-schedules/{scheduleId}/orders` | 167.78 | 37.01 | 1007.0 → 12.0 |
| GET | `/api/v1/organizations/{orgId}/pickup-schedules/{scheduleId}/orders/page` | mới | 8.26 | mới → 8.0 |
| GET | `/api/v1/organizer/notifications` | 5.78 | 6.62 | 5.0 → 5.0 |
| PATCH | `/api/v1/organizer/notifications/read-all` | 5.00 | 3.85 | 4.0 → 4.0 |
| GET | `/api/v1/organizer/notifications/stream` | 4.15 | 4.28 | 3.0 → 3.0 |
| GET | `/api/v1/organizer/notifications/unread-count` | 4.85 | 5.95 | 4.0 → 4.0 |
| PATCH | `/api/v1/organizer/notifications/{id}/read` | 4.65 | 4.79 | 5.0 → 5.0 |
| GET | `/api/v1/public/campaigns` | 4.00 | 5.22 | 2.0 → 2.0 |
| GET | `/api/v1/public/campaigns/{id}` | 4.20 | 2.95 | 4.0 → 4.0 |
| GET | `/api/v1/public/events` | 7.38 | 6.88 | 2.0 → 2.0 |
| GET | `/api/v1/public/events/{id}` | 4.70 | 6.27 | 5.0 → 5.0 |
| GET | `/api/v1/public/merch` | 9.55 | 8.17 | 3.0 → 3.0 |
| GET | `/api/v1/public/merch/popular` | 22.68 | 19.41 | 5.0 → 5.0 |
| POST | `/api/v1/public/merch/visual-search` | 33.70 | 14.81 | 3.0 → 3.0 |
| GET | `/api/v1/public/merch/{id}` | 8.21 | 7.87 | 2.0 → 2.0 |
| GET | `/api/v1/public/merch/{merchId}/purchase-context` | 5.90 | 3.20 | 3.0 → 3.0 |
| POST | `/api/v1/public/orders` | 15.89 | 9.63 | 8.0 → 8.0 |
| GET | `/api/v1/public/orders/{orderId}` | 3.33 | 3.48 | 2.0 → 2.0 |
| POST | `/api/v1/public/orders/{orderId}/pickup-receipt` | 4.21 | 3.51 | 5.0 → 5.0 |
| POST | `/api/v1/public/orders/{orderId}/pickup-token` | 4.62 | 4.32 | 8.0 → 8.0 |
| GET | `/api/v1/public/organizations` | 9.31 | 7.65 | 3.0 → 3.0 |
| GET | `/api/v1/public/organizations/{id}` | 5.68 | 4.53 | 3.0 → 3.0 |
| GET | `/api/v1/public/organizations/{id}/events` | 6.35 | 5.38 | 2.0 → 2.0 |
| GET | `/api/v1/public/organizations/{id}/merch` | 7.74 | 6.89 | 3.0 → 3.0 |
