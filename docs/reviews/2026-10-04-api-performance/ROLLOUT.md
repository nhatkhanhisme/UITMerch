# Quy trình review và phát hành tối ưu API

Ngày thực hiện: 05/10/2026. Đối chiếu với [kế hoạch](OPTIMIZATION-PLAN.md) và [benchmark ban đầu](README.md).

## Branch và phạm vi

- `bugfix/backend-order-dispatch`: tách bản sửa ERROR dispatch và 5 test tra cứu đơn của lượt trước; commit `3a44d76`.
- `feature/backend-api-performance`: dựa trên branch bugfix, chứa commit benchmark `b6c96cc`, tối ưu backend, frontend và kết quả kiểm chứng. Đây là nhánh đang làm việc.
- Base local là `main` tại `f070580870b389bd0e4258fe8806862fe561107d`. Sau `git fetch origin --prune`, local main vẫn đi trước `origin/main` 28 commit; `origin/dev` có lịch sử khác. Các commit này đã tồn tại trước nhiệm vụ tối ưu, không tự động push hoặc rebase chúng.
- Review thay đổi riêng của nhiệm vụ bằng `git diff main...feature/backend-api-performance` và `git log --oneline main..feature/backend-api-performance`.
- Trước khi mở PR trên GitHub, đồng bộ baseline đã được review tới branch tích hợp phù hợp. PR so với `origin/main` hiện tại sẽ bao gồm cả 28 commit cũ; không coi tất cả là phần tối ưu mới. Không force-push, merge hoặc deploy trong lượt triển khai local này.

Repo đã có CI chạy toàn bộ PostgreSQL tests, frontend tests/build và Playwright trên PR vào main. Push main tự gọi deploy hook Render; vì vậy việc phát hành cần dùng PR đã review và kiểm chứng staging. Không có lý do tạo lại pipeline đang hoạt động.

## Thay đổi đã chuẩn bị

1. Batch order items và distinct pickup schedules cho trang customer/organizer/admin. Các query IN được chia nhóm tối đa 200 ID để hỗ trợ cả endpoint legacy.
2. Gom order counts cho một trang pickup schedules thành một aggregate query.
3. Thêm `GET /api/v1/organizations/{orgId}/pickup-schedules/{scheduleId}/orders/page`, mặc định 20, cap 100 và thứ tự có ID làm tie-breaker. Endpoint `/orders` cũ vẫn trả toàn bộ danh sách và được đánh dấu deprecated trên OpenAPI.
4. Frontend check-in dùng endpoint mới, tải từng trang, có busy/error/retry và cache tách theo user/org/schedule/page; check-in thành công cập nhật và invalidate các trang liên quan.
5. GET giỏ đã có không khóa user. Tạo/reactivate giỏ và các đường ghi/checkout vẫn giữ khóa và recheck. Database đã có unique `carts.user_id`, không cần thêm constraint trùng.
6. Popular vẫn tính thứ hạng từ dữ liệu hiện hành nhưng chỉ tải ảnh cho top 10. Chưa cache rank/DTO để giữ cập nhật tức thời về đơn hàng, giá, tồn kho và archival.
7. Categories cache DTO có thứ tự; Caffeine tối đa 32 entry/cache, TTL 60 giây. Cache put/eviction phối hợp với commit qua transaction-aware cache manager. Cache nội bộ từng instance; thay đổi từ instance khác hoặc SQL trực tiếp có thể cần tối đa 60 giây mới hiện.
8. Transport anonymous riêng cho 12 đường đọc catalog/frontend helper, giữ auth/refresh trên checkout, quản trị, pickup và request phụ thuộc tài khoản.
9. Flyway V44 thêm index user/time/ID cho orders, org/schedule/time/ID cho pickup và user/time/ID cho notifications. V45 thêm `pg_trgm` và GIN `lower(name)` không có partial status predicate.
10. Actuator HTTP/Hikari/JVM metrics với HTTP histograms và các ngưỡng 300 ms/500 ms/1 s. `/actuator/**` chỉ ADMIN; chỉ health và metrics được expose. SMTP health tắt riêng ở dev/docker vì các profile này dùng mail stub. Root health hiện có tiếp tục hoạt động.

Index không buộc planner phải chọn một cách thực thi. Khi keyword ngắn/phổ biến hoặc generic plan, database có thể chọn B-tree/Seq Scan. Kiểm chứng cả query có bind parameters, statistics và execution time; không tự động ép `force_custom_plan` hoặc tắt server-side prepare trên toàn ứng dụng.

## Kiểm thử và phép đo

```bash
# Toàn bộ backend, PostgreSQL disposable, không đọc backend/.env.
bash backend/scripts/test-postgres.sh -q

# Frontend: chạy trong frontend/.
npm test
npm run build
npm run test:e2e

# HTTP benchmark cô lập; SQL DDL experiments mặc định tắt trên schema đã tối ưu.
bash backend/scripts/performance-audit.sh
```

`ApiOptimizationRegressionTest` kiểm chứng query count, metadata/legacy contract, quyền customer/org/role, snapshot giá/tên, cache commit/rollback và tạo/đọc giỏ đồng thời. `PopularMerchReadTest` kiểm tra ảnh chỉ được tải cho kết quả đã xếp hạng. `ApiObservabilitySecurityTest` kiểm tra quyền metrics và endpoint nhạy cảm không expose. `PreparedSearchIndexTest` kiểm chứng query bind trên 30.000 sản phẩm bổ sung và lưu cả automatic/forced-generic plan. `MigrationUpgradeFeatureTest` thực hiện nâng cấp V34 → V45, giữ nguyên stock và không gửi lại historical publication alerts.

Các bài hiệu năng HTTP ngắn dùng để so sánh implementation. Nghiệm thu production capacity cần staging, workload arrival-rate nhiều user/SKU, ít nhất 5–10 phút/mức tải và một bài ổn định 30–60 phút. Dịch vụ AI/email/storage vẫn được stub trong benchmark HTTP; không dùng các số đó làm latency provider thật.

Benchmark đóng stream SSE sau dòng đầu và ghi nhận `AccessDeniedException` khi kết nối đã committed ở cả baseline và bản tối ưu. HTTP mở stream vẫn trả 200; điều này không chứng minh vòng đời stream đúng. Trước nghiệm thu thông báo realtime, cần bài integration cho connect/disconnect/timeout/reconnect và kiểm tra security context ở async dispatch. Không mở rộng quyền ASYNC toàn cục chỉ để làm sạch log benchmark. Chi tiết và phạm vi phép đo nằm trong [kết quả triển khai](OPTIMIZATION-RESULTS.md).

## Database trước khi phát hành

- V44/V45 tạo index đồng thời, ngoài transaction qua companion `.sql.conf`. Flyway dùng session advisory lock (`spring.flyway.postgresql.transactional-lock=false`), theo [tài liệu Flyway](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-postgresql-namespace/flyway-postgresql-transactional-lock-setting).
- Dùng kết nối trực tiếp hoặc session pooling phù hợp cho migration; transaction pooler không bảo đảm session advisory lock. Có thể cấu hình `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER`, `SPRING_FLYWAY_PASSWORD` riêng với datasource ứng dụng.
- Xác nhận quyền tạo `pg_trgm`, dung lượng/I/O và backup trên staging trước. Không sửa migration đã được apply.
- Theo dõi `pg_stat_progress_create_index` và kiểm tra `pg_index.indisvalid/indisready` của bốn index mới. Chạy ANALYZE sau khi nạp/chuyển dữ liệu lớn; với GIN cần lưu ý pending list và autovacuum.
- Migration ngoài transaction có thể để lại một phần index nếu bị ngắt. Không dùng `IF NOT EXISTS` để âm thầm chấp nhận index INVALID. Khi thất bại, kiểm tra index và Flyway history; loại bỏ các index mới thuộc migration thất bại bằng thao tác concurrent thích hợp, rồi repair/retry có kiểm soát. Không xóa index cũ hoặc tự động sửa history trong ứng dụng.

## Pool và metrics

Default pool production vẫn là max 10/min idle 2/timeout 20.000 ms; có thể thử `APP_DB_POOL_MAX`, `APP_DB_POOL_MIN_IDLE`, `APP_DB_CONNECTION_TIMEOUT_MS` trên staging. Profile docker giữ max 5/min 1; benchmark pin max 5/min 1 để so sánh.

Ví dụ metric names cần theo dõi: `http.server.requests`, `hikaricp.connections.active`, `hikaricp.connections.pending`, `hikaricp.connections.acquire`, `hikaricp.connections.timeout`, `jvm.memory.used`, `jvm.gc.pause`. Đọc qua `/actuator/metrics` bằng token ADMIN, không đặt token trong URL/log. JSON metrics endpoint phục vụ kiểm tra; dashboard/exporter và cảnh báo cần được nối với hệ thống giám sát của môi trường triển khai. [Spring Boot Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html).

Thử các pool 5/10/15 nếu quota cho phép sau khi giảm N+1 và contention. Tính tổng kết nối mọi instance và worker, chừa phần cho migration/quản trị. Chọn theo p95, throughput và tài nguyên DB; xem [Hikari pool sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).

## Thứ tự phát hành và rollback

1. Chốt baseline tích hợp và review các commit mới. Chạy CI đầy đủ.
2. Kiểm thử migration trên staging và ghi baseline staging cùng dữ liệu/cấu hình trước/sau.
3. Phát hành backend hỗ trợ cả endpoint cũ/mới trước, xác nhận metadata và quyền truy cập.
4. Phát hành frontend dùng pagination/anonymous catalog, kiểm tra đơn ở trang cuối và checkout có auth.
5. Theo dõi latency/error/lock/pool và tính đúng giá/tồn kho. Chỉ chọn pool mới sau bài tải đã nêu.

Rollback frontend trước nếu cần; backend vẫn giữ endpoint legacy. Có thể rollback image/config backend về baseline V43 vì các migration mới chỉ thêm extension/index, không đổi dữ liệu hoặc xóa cột. Giữ index khi rollback app nếu chúng không gây vấn đề tài nguyên; quản lý DROP/repair ngoài transaction riêng nếu thực sự cần. Tắt/xóa cache runtime bằng restart trong rollback.

Rollback ngay nếu sai quyền truy cập, giá/tồn kho hoặc checkout trùng. Với hiệu năng, điều tra/rollback khi p95 tăng trên 20% ở tải tương đương qua hai cửa sổ 5 phút, hoặc lỗi/timeout vượt ngưỡng đã thống nhất.

## Phần cần xác minh trên hạ tầng thực

Chưa đổi vị trí backend/database, quota/provider hay pool production; chưa gọi AI/SMTP/storage thật để thử tải. Chưa triển khai cache rank, gộp query auth hay tăng worker concurrency: các thay đổi này cần số đo riêng và giữ immediate revocation/idempotency. Các API public chậm nhiều giây trong báo cáo ban đầu cần được đo lại sau triển khai lên staging; kết quả local không chứng minh chúng đã nhanh hơn trên backend đang chạy.
