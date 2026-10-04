# Kế hoạch tối ưu tốc độ API UITMerch

Ngày lập: 04/10/2026. Căn cứ: [báo cáo benchmark 103 route](README.md).

Đây là kế hoạch triển khai; chưa áp dụng các thay đổi tối ưu. Ước lượng **7–10 ngày làm việc cho một lập trình viên** để hoàn thành phần ưu tiên, với điều kiện có staging/database tương đương môi trường thực. Các công việc xác thực, AI và worker là đợt tiếp theo, dự kiến thêm 2–4 ngày sau khi có số đo. Thời gian có thể thay đổi nếu cần chuyển vùng database hoặc điều chỉnh hợp đồng API với frontend.

## 1. Vấn đề cần giải quyết và mục tiêu

Hai nhóm số liệu dưới đây thuộc hai môi trường khác nhau; không dùng để so sánh trước/sau trực tiếp.

| Bằng chứng hiện có | Ưu tiên xử lý | Tiêu chí kiểm chứng |
|---|---|---|
| Backend đang chạy: popular p95 7,16 s, tìm kiếm 4,75 s, danh sách sản phẩm 4,22 s; health 4 ms | Phân rã thời gian request, đo database và giảm thao tác nối tiếp | Xác định được thời gian chờ pool, SQL, xử lý ứng dụng và upstream cho các route chậm |
| Benchmark cô lập: lịch nhận khoảng 1.001 đơn tạo 1.007 câu lệnh Hibernate/request | Batch loading và phân trang | Với một trang tối đa 100 đơn, số SQL không tăng tuyến tính theo số đơn; mục tiêu ban đầu ≤12 câu lệnh Hibernate cho trang phổ biến, tính cả auth |
| Trang customer orders size 1/20/100 tạo 7/45/140 câu lệnh Hibernate | Batch order items và pickup schedules | Trang size 20 và 100 giữ số truy vấn gần nhau; nội dung và quyền truy cập tương đương trước sửa |
| Giỏ hàng đọc khóa user; p95 C=20 cùng user là 100,42 ms trong local | Tách đọc giỏ đã có khỏi khởi tạo/khóa | Giảm contention khi đọc; không tạo nhiều active cart hoặc sai tồn kho khi ghi đồng thời |
| Pool 5 kết nối có đến 15 request chờ tại concurrency 20 trong local | Chọn pool bằng thử tải trên staging | Chọn cấu hình có p95/throughput tốt với quota DB, thay vì chỉ giảm số thread chờ |

Mục tiêu ban đầu cho staging: API đọc thông thường **p95 ≤300 ms**, API ghi **p95 ≤500 ms**, auth **p95 ≤1 s**, không có 5xx ngoài dự kiến hoặc sai lệch nghiệp vụ trong bộ thử. Đây là mục tiêu cần xác nhận với traffic và hạ tầng thực, không phải cam kết đạt ngay. AI và SSE có tiêu chí riêng.

Trong đợt đầu, mục tiêu trung gian cho nhóm public hiện chậm là **giảm ít nhất 50% p95 so với baseline staging cùng điều kiện**. Nếu chưa đạt, phân tích trace để chọn bước tiếp theo. Chốt tải mục tiêu từ traffic quan sát; nếu chưa có traffic, báo cáo theo từng mức RPS và điểm bắt đầu suy giảm, không quy đổi trực tiếp thành số người dùng.

## 2. Các đợt triển khai

| Đợt | Ưu tiên | Ước lượng | Kết quả bàn giao |
|---|---|---|---|
| 0. Chuẩn hóa phép đo và xác định nút thắt | P0 | 0,5–1 ngày | Baseline staging, trace các API chậm, dashboard và quyết định về DB/network |
| 1. Giảm truy vấn đơn hàng và phân trang lịch nhận | P0 | 2 ngày | Batch queries, API/frontend phân trang tương thích, kiểm thử nghiệp vụ và số query |
| 2. Tối ưu public catalog và index đã được chứng minh | P1 | 1–2 ngày | Cache categories, giảm tải popular, migration index có EXPLAIN trước/sau |
| 3. Tách khóa giỏ hàng và chọn connection pool | P1 | 1–2 ngày | Đường đọc ít contention, kiểm thử race/stock, cấu hình pool theo kết quả staging |
| 4. Kiểm thử tổng hợp và chuẩn bị phát hành | P0 | 1–2 ngày | Báo cáo trước/sau, kết quả tải kéo dài, checklist phát hành/rollback |
| 5. Auth, AI và worker theo số đo | P2 | Thêm 2–4 ngày | Tối ưu từng thành phần có bằng chứng chi phí đáng kể |

Đợt 1 và việc sửa code của đợt 2 có thể thực hiện trên DB cô lập nếu staging chưa sẵn sàng. Các quyết định về vùng triển khai, pool và nghiệm thu độ trễ thực phụ thuộc kết quả đợt 0. Không gộp tất cả thay đổi vào một bản phát hành vì sẽ khó xác định nguyên nhân cải thiện hoặc hồi quy.

### Đợt 0 — Đo đúng nút thắt

- Ghi nhận commit/image, CPU/RAM, JVM, pool, logging, scheduler, kích thước/phân bố dữ liệu và vùng backend/DB. Có baseline cùng image và cấu hình để so sánh sau sửa.
- Bổ sung histogram theo route template cho `http.server.requests`; theo dõi Hikari acquire/pending/active, timeout, JVM/GC và DB CPU/I/O/locks. Chỉ lấy mẫu trace/SQL, không log bind values hoặc token. Spring Boot cung cấp metrics HTTP và datasource qua Micrometer: [tài liệu metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html).
- Trace ít nhất popular, search, merch list, categories và một trang orders; phân biệt thời gian chờ connection, thực thi SQL, khóa, serialization và upstream. Thời gian JDBC có thể bao gồm network; dùng số đo phía database để đối chiếu.
- Đo round trip `SELECT 1` qua kết nối tái sử dụng, lấy query plan thực và kiểm tra endpoint/pooler. Nếu network là thành phần lớn, chuẩn bị phương án đặt backend và DB cùng vùng, kèm chi phí và cách chuyển kết nối; không chuyển hạ tầng trong bước lập kế hoạch.
- Thu thập traffic theo route. Dùng dữ liệu này xác định RPS và tỷ lệ đọc/ghi cho đợt nghiệm thu.

**Hoàn thành khi:** có baseline có thể chạy lại và bằng chứng đủ để xác định thành phần chiếm phần lớn thời gian của ít nhất ba route chậm nhất.

### Đợt 1 — Đơn hàng: batch loading trước, phân trang sau

- Phạm vi: `OrderService.getCustomerOrders`, `getOrgOrders`, `getAllOrders`, `getPickupScheduleOrders` và repository/DTO/frontend liên quan.
- Lấy một trang order rồi lấy toàn bộ order items bằng danh sách order IDs; gom theo order ID trong bộ nhớ. Lấy pickup schedules bằng danh sách ID khác nhau thay cho gọi từng đơn.
- Tránh fetch join collection trực tiếp với pagination nếu gây nhân bản hàng hoặc phân trang trong bộ nhớ. Dùng bước lấy trang IDs/orders rồi tải quan hệ theo lô.
- Thêm endpoint/contract phân trang cho danh sách đơn theo lịch nhận: mặc định 20, tối đa 100, thứ tự ổn định bằng thời gian và ID. Cập nhật frontend/types cùng đợt. Nếu có client khác đang dùng contract `List`, giữ lộ trình chuyển tiếp rõ ràng thay vì đổi response âm thầm.
- Với chức năng cần toàn bộ đơn như xuất danh sách hoặc thao tác hàng loạt, giữ một luồng riêng và đọc theo từng trang; không coi trang đầu là toàn bộ dữ liệu.
- Kiểm thử ownership/customer/org/admin, đơn không có lịch nhận, dữ liệu snapshot, cancel/pickup và ranh giới trang có cùng thời gian tạo. Dùng kiểm thử số truy vấn để phát hiện N+1 quay lại.

**Hoàn thành khi:** response nghiệp vụ đúng, frontend hoạt động, các trang size 20/100 không còn một truy vấn cho mỗi đơn và benchmark cùng fixture cho thấy cải thiện.

### Đợt 2 — Public catalog và index

- `CategoryService.listAll`: cache danh sách đúng thứ tự; invalidation sau khi cập nhật thành công. Chọn cache hiện có, chưa cần thêm Redis chỉ để cache danh mục.
- `MerchService.getPopularMerch`: xếp hạng trước, chỉ tải ảnh cho top 10. Bước tiếp theo có thể cache danh sách ranking IDs với TTL khởi đầu 30–60 giây và invalidation phù hợp; đọc giá, tồn kho và trạng thái hiện hành khi dựng response. Kiểm thử sản phẩm vừa ẩn/hết hàng/đổi giá và cache miss đồng thời.
- Frontend: với danh sách/detail catalog có response giống nhau giữa khách và user, dùng request không gắn Authorization để tránh chi phí auth không cần thiết. Chỉ áp dụng cho danh sách endpoint đã kiểm chứng; giữ danh tính trên checkout và mọi luồng phụ thuộc tài khoản.
- Rà soát index hiện có, bổ sung ứng viên `orders(user_id, created_at DESC, id DESC)` sau khi đối chiếu query thực. Xem xét notification user/time và trigram `lower(name)` cho search nếu EXPLAIN với bind parameters xác nhận lợi ích. Index org/time đã có ở V42, tránh tạo trùng.
- Đo query plan và tốc độ ghi trước/sau trên dữ liệu có phân bố tương đương. Index partial phải được kiểm tra với prepared/generic plan mà ứng dụng dùng.
- Với bảng lớn đang phục vụ traffic, cân nhắc `CREATE INDEX CONCURRENTLY`, chạy ngoài transaction và cấu hình migration tương ứng; kiểm tra index hợp lệ sau khi tạo. PostgreSQL nêu rõ giới hạn và chi phí của cách tạo này: [CREATE INDEX](https://www.postgresql.org/docs/17/sql-createindex.html).

**Hoàn thành khi:** categories có cache hit thật, popular không tải ảnh của toàn bộ candidates, dữ liệu thương mại vẫn cập nhật đúng và từng index có bằng chứng được planner sử dụng.

### Đợt 3 — Giỏ hàng và connection pool

- `CartService.getCart`: đọc active cart đã tồn tại mà không lấy khóa user. Chỉ khởi tạo khi cần; kiểm tra constraint hiện có, dùng constraint bảo đảm một active cart/user và xử lý conflict khi tạo song song.
- Giữ transaction/khóa cần thiết cho add/update/checkout. Kiểm thử tạo giỏ đồng thời, đọc khi đang sửa, checkout trùng, nhiều user tranh cùng SKU, rollback khi thiếu hàng và tính tổng giá.
- Sau khi giảm truy vấn và contention, thử pool 5/10/15 nếu quota DB cho phép. Tính tổng kết nối của tất cả instance và worker, chừa phần cho quản trị/migration. Chọn theo latency, throughput và DB saturation. Hikari khuyến nghị thử tải để chọn pool phù hợp: [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).
- Chạy riêng case một user gửi nhiều request và case nhiều user độc lập để phân biệt khóa theo user với giới hạn pool chung.

**Hoàn thành khi:** đường đọc không chờ khóa user khi giỏ đã tồn tại, race tests không tạo dữ liệu sai và cấu hình pool được chọn từ số đo trên staging.

### Đợt 4 — Nghiệm thu và phát hành từng phần

- Chạy lại bộ inventory/benchmark 103 route trên DB cô lập; lưu số query, latency và response size. Chạy các test nghiệp vụ phù hợp, không dùng HTTP 2xx làm bằng chứng duy nhất cho tính đúng.
- So sánh trước/sau trên cùng dữ liệu, image/config/phần cứng, warmup và logging. Phân biệt kết quả SQL riêng với cải thiện end-to-end API.
- Trên staging: arrival-rate workload, ít nhất 5–10 phút/mức tải; thử mức nền, mục tiêu và cao hơn mục tiêu; thêm một bài ổn định 30–60 phút tại tải mục tiêu. Theo dõi cả request không gửi được vì thiếu client capacity.
- Workload gồm nhiều user, cùng SKU/nhiều SKU, cache nóng/lạnh, filter/phân trang, pool wait, lock wait, worker bật và queue backlog. Ghi rõ AI/provider được gọi thật hay mô phỏng; chỉ thử provider thật ở mức phù hợp quota.
- Ghi p50/p95/p99, RPS hoàn tất, lỗi/429, timeout, CPU/RAM/GC, SQL/request và pool acquire. Các 4xx/409/429 hợp lệ ở bài thử tranh chấp/rate limit được đánh giá theo kết quả mong đợi.
- Phát hành theo nhóm thay đổi nhỏ sau bước duyệt triển khai. Chuẩn bị rollback image/config/cache trước mỗi nhóm; với index bổ sung, thường giữ lại khi rollback app nếu không có vấn đề tài nguyên. Contract phân trang phải tương thích với frontend hiện được phục vụ.
- Rollback nếu có lỗi ownership, giá/tồn kho sai, checkout trùng; hoặc p95 tăng >20% so với baseline dưới tải tương đương trong hai cửa sổ 5 phút; hoặc xuất hiện lỗi/timeout vượt ngưỡng đã thống nhất.

**Hoàn thành khi:** có báo cáo trước/sau cho mọi nhóm thay đổi, đạt mục tiêu đã chốt hoặc ghi rõ phần chưa đạt và nguyên nhân; không có hồi quy nghiệp vụ nghiêm trọng.

### Đợt 5 — Các thành phần cần đo thêm

- **Xác thực:** nếu ba lượt đọc blacklist/user/session chiếm tỷ trọng lớn, xem xét một query/projection. Kiểm thử logout/revocation, authVersion, đổi quyền, tài khoản bị khóa và session expiry. Giữ cost BCrypt hiện tại.
- **Visual search:** đo Gemini/vector thật; thống nhất một budget tổng giữa backend và timeout frontend, cancellation/fallback; chỉ cache theo hash khi quyền truy cập và dữ liệu cho phép. Đánh giá chất lượng kết quả, latency provider và 429 riêng.
- **Worker:** đo tuổi job, throughput và thời gian gửi OTP thật trước khi tăng batch/concurrency. Giữ retry/idempotency/claim bằng SKIP LOCKED; không giữ transaction database trong thời gian chờ provider.
- **Serialization/logging:** chỉ thêm compression hoặc giảm log sau khi trace/response size cho thấy chi phí đáng kể; giữ dữ liệu chẩn đoán cần thiết.

## 3. Quy trình thay đổi và bàn giao

Mỗi nhóm thay đổi cần một bản diff có thể review, test phù hợp và kết quả benchmark trước/sau. Không gom sửa security/order checking của lượt trước vào phần tối ưu mà không ghi rõ phạm vi.

Tuân thủ AGENTS.md: trước khi sửa bất kỳ symbol hiện có, chạy GitNexus upstream impact, báo direct callers/processes/risk; cảnh báo HIGH/CRITICAL trước khi sửa. Nếu index stale, cập nhật trước. Chạy detect-changes trước khi commit. Kế hoạch này chưa thay đổi symbol nên chưa có kết quả impact cho các bản sửa tương lai.

Bàn giao cuối đợt ưu tiên:

1. Diff từng nhóm: quan sát, đơn hàng, catalog/index, giỏ hàng/pool.
2. Migration và ghi chú tương thích frontend/rollback.
3. Kiểm thử nghiệp vụ/race và kiểm thử số query.
4. Báo cáo đối chiếu 103 route, bài tải staging, dashboard và cấu hình được chọn.
5. Danh sách vấn đề còn lại, sắp xếp theo chi phí đo được.

Lệnh benchmark cô lập hiện có: `bash backend/scripts/performance-audit.sh`. Các script này không tự triển khai tối ưu hoặc thay thế phép đo staging; xem [hướng dẫn và artifact](README.md#9-chạy-lại-và-các-artifact).
