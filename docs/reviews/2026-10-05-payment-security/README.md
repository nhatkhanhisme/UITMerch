# Kiểm tra bảo mật trước tích hợp thanh toán online

Ngày: 2026-10-05 (Asia/Ho_Chi_Minh). Source: commit `cc67c35`, branch `feature/backend-api-performance`.

## Kết luận

Có thể lập kế hoạch kỹ thuật dựa trên báo cáo này, nhưng chưa đủ bằng chứng để mở thanh toán thật. Cần xử lý chống lạm dụng checkout, giảm nguy cơ lộ session, và xác minh cấu hình production. App hiện chỉ hỗ trợ COD; các thiếu hụt payment bên dưới là yêu cầu cho tính năng mới, không phải kết luận rằng gateway hiện tại bị khai thác.

Đây là review code local, truy vết GitNexus, chạy test có sẵn và npm audit. Không sửa source ứng dụng, không commit, không tấn công môi trường live. GitNexus MCP không có trong session nên dùng CLI tương đương; đã refresh index cũ. Index có cảnh báo giới hạn số execution flows, vì vậy kết luận dựa cả trên source đọc trực tiếp. Các file hướng dẫn do analyzer tự sinh đã được trả về trạng thái ban đầu.

## Phát hiện hiện tại

### S1 — HIGH: Guest checkout có thể bị dùng để chiếm tồn kho

Bằng chứng: `backend/src/main/java/com/uitmerch/backend/order/service/OrderService.java:211–290` trừ tồn kho ngay khi tạo đơn public; DTO không có giới hạn quantity trên (`order/dto/GuestOrderItemRequest.java`), email không bắt buộc và không được xác minh. Controller chỉ giới hạn 20 request/IP/giờ (`order/controller/PublicOrderController.java:34–51`). Không tìm thấy timeout tự trả tồn kho cho đơn thường trong order module; campaign có chính sách riêng.

Kịch bản: người không đăng nhập lấy merch ID public rồi đặt quantity bằng toàn bộ stock, dùng thông tin guest tùy ý. Một request hợp lệ có thể làm sản phẩm hết hàng; row lock chống oversell nhưng không chống hành vi này. Chưa chạy kịch bản phá tồn kho trên live.

Đề xuất: hạn mức số lượng và giá trị đơn, xác minh guest hoặc challenge phù hợp, giới hạn theo nhiều tín hiệu. Với online payment, reservation phải có thời hạn và hoàn stock đúng một lần khi hết hạn; phối hợp với webhook đến trễ bằng khóa transaction.

### S2 — MEDIUM: Access token trong URL SSE

Bằng chứng: `frontend/src/hooks/useNotificationStream.ts:58` tạo `?token=...`; `backend/src/main/java/com/uitmerch/backend/common/config/JwtAuthenticationFilter.java:136–144` nhận access token từ query cho SSE. Token có quyền API của session, không chỉ đọc thông báo. TTL mặc định access token là 24 giờ (`application.yaml`).

Nguy cơ: URL có thể vào proxy/access log, telemetry hoặc bản ghi hỗ trợ nếu không che query. Chưa xác nhận log production thực sự lưu token.

Đề xuất: SSE qua transport có Authorization header hoặc ticket ngắn hạn, giới hạn phạm vi và dùng một lần. Che token trong mọi log và giảm TTL access token.

### S3 — MEDIUM: Access/refresh token được persist vào localStorage

Bằng chứng: `frontend/src/stores/authStore.ts:38–78` dùng Zustand persist mặc định, lưu cả accessToken và refreshToken. `frontend/vercel.json` không khai báo CSP; header ở hạ tầng chưa được kiểm tra.

Một XSS nếu xuất hiện có thể đọc cả hai token. Đây là mức độ phơi nhiễm, không phải bằng chứng đã có XSS. Đề xuất đánh giá refresh token qua cookie HttpOnly/Secure, access token trong memory, CSP và TTL ngắn. Nếu chuyển sang cookie, phải thiết kế CSRF/SameSite/CORS cùng lúc; CSRF đang tắt là phù hợp với API bearer hiện tại, không tự động là lỗi.

### S4 — MEDIUM: Guest tracking chỉ dùng order UUID + email

Bằng chứng: `PublicOrderController.java:66–81`, `OrderService.java:619–632`. Không yêu cầu chứng minh sở hữu email, không có limiter riêng cho tracking. Response trả tên, email, điện thoại, note và thông tin pickup (`order/dto/OrderResponse.java`).

UUID ngẫu nhiên là rào cản; không kết luận có thể enumerate mọi đơn. Nhưng khi UUID bị lộ qua chia sẻ/link/email hỗ trợ, email không phải secret mạnh. Email còn nằm trong query URL.

Đề xuất: token tracking ngẫu nhiên riêng, có hạn và khả năng thu hồi; trả dữ liệu tối thiểu, thêm rate limit và no-store. Không dùng email làm credential cho payment/refund.

### S5 — CRITICAL nếu deploy nhầm profile dev/docker: Endpoint public trả OTP

Bằng chứng: `auth/controller/DevOtpController.java:25–45` trả OTP chưa dùng bằng email; `common/config/SecurityConfig.java` cho phép `/api/v1/dev/**` khi bật dev/docker. `common/config/DevDataInitializer.java` còn seed tài khoản demo với mật khẩu cố định trên DB trống.

Đây là rủi ro có điều kiện, chưa xác nhận production bật các profile này. Production phải chặn dev/docker bằng kiểm tra khởi động hoặc pipeline, đồng thời kiểm tra `/api/v1/dev/otps` không public và không có tài khoản demo. Dùng container không đồng nghĩa phải bật Spring profile docker.

### S6 — MEDIUM: Rate limiter chỉ ở memory của từng instance

Bằng chứng: `common/service/RateLimiterService.java` dùng ConcurrentHashMap. Restart reset quota; scale nhiều instance chia nhỏ quota. `IpUtil` chỉ tin proxy IP được cấu hình, là biện pháp tốt; khi trust proxy cần bảo đảm proxy ghi đè/chuẩn hóa X-Forwarded-For vì code lấy phần tử đầu.

Đề xuất: limiter chia sẻ hoặc tại edge, quota theo account/action và cấu hình trusted proxy được kiểm chứng. Payment/webhook cần chính sách riêng để không chặn retry hợp lệ của gateway.

### S7 — Dependency advisory cần xử lý trước rollout

`npm audit` frontend: 21 package affected (11 high, 8 moderate, 2 low; 0 critical). `--omit=dev`: 6 affected (2 high, 4 moderate): axios, form-data, fflate, react-router, react-router-dom, @remix-run/router. Đây là số package affected, không phải 21 đường khai thác đã xác nhận.

Phải phân loại theo runtime: advisory axios/form-data có nhiều điều kiện Node adapter, trong khi app frontend chạy browser; Vite/PostCSS/Tailwind chủ yếu thuộc build/dev. Không suy diễn SSR advisory áp dụng cho SPA. Đọc từng advisory trong JSON đính kèm, nâng cấp có kiểm thử và đánh giá đường redirect có liên quan trước khi dùng làm payment return flow. Không chạy audit fix --force tự động.

Chưa thực hiện SCA backend Maven/transitive dependencies, scan toàn lịch sử Git hoặc xác minh secrets thực tế. Không coi backend sạch CVE chỉ vì test pass.

## Các kiểm soát tốt đã thấy

- Backend lấy giá từ merch/campaign, dùng BigDecimal và snapshot unit price/subtotal, không nhận tổng tiền từ checkout DTO.
- Transaction, pessimistic lock và conditional stock deduction; constraint stock không âm trong migration.
- Customer read/cancel kiểm tra userId; organizer kiểm tra ownership organization và orgId của order. Guest tracking từ chối order có userId.
- JWT phân biệt access/refresh; session refresh hash ở DB, rotation có khóa, session revocation và authVersion. Filter lấy role hiện tại từ DB, kiểm tra active/verified session.
- BCrypt, OTP giới hạn số lần thử; reset password và logout có regression tests.
- Swagger tắt mặc định; actuator yêu cầu ADMIN trong security chain (test riêng actuator bị skip ở lần chạy này).

## Điều kiện bắt buộc đưa vào kế hoạch payment

1. **Tách payment khỏi fulfillment.** Hiện updateOrderStatus, createPickupSchedule, completePickup không kiểm paymentStatus; phù hợp COD nhưng khi thêm online phải chặn giao hàng chưa PAID. cancelCustomerOrder/cancelOrgOrder hiện hoàn stock nhưng chưa có refund workflow. Không chỉ thêm enum và nút thanh toán.
2. **Payment attempt và sổ giao dịch riêng.** Lưu immutable amount/currency/order linkage, merchant account, provider transaction ID, idempotency key, status, timestamps và event dedupe bằng unique constraints. Checkout thường chưa có idempotency; campaign đã có requestId riêng, không thay thế payment idempotency.
3. **Webhook là nguồn xác nhận được kiểm chứng.** Xác minh chữ ký theo gateway, đối chiếu amount/currency/order/merchant và trạng thái; chống replay, xử lý callback lặp và sai thứ tự trong transaction. Return URL trên browser chỉ hiển thị kết quả backend đã xác nhận, không được tự set PAID.
4. **Race conditions.** Thiết kế success đến sau cancel/expiry, refund đồng thời, retries và timeout provider. Không hoàn stock hay refund hai lần; reconciliation để xử lý trạng thái không chắc chắn.
5. **Đơn nhiều tổ chức.** Checkout hiện tách thành nhiều orders theo org. Phải quyết định một payment cho checkout group hay từng order; định nghĩa phân bổ tiền, thành công một phần và hoàn tiền từng phần.
6. **Quyền và audit.** Customer chỉ tạo payment cho đơn của mình; guest dùng credential riêng; organizer không tự đánh dấu online PAID. Refund có authorization, giới hạn amount và log actor/reason/transaction; không log token/secrets hoặc dữ liệu thẻ.
7. **Hosted checkout và secrets server-side.** Gateway key/signing secret chỉ ở backend, tách sandbox/live; callback HTTPS và allowlist return destination. Kiểm thử invalid signature, wrong amount/currency/merchant, duplicate/out-of-order event, cross-user/org access, simultaneous cancel/pay/refund và failed provider calls.

Các yêu cầu webhook, đối chiếu giao dịch và idempotency được đối chiếu với [OWASP Payment Gateway Integration](https://cheatsheetseries.owasp.org/cheatsheets/Third_Party_Payment_Gateway_Integration_Cheat_Sheet.html).

## Phần hạ tầng chưa xác minh

Frontend upload trực tiếp qua Supabase anon client (`frontend/src/api/storage.ts`, `supabaseClient.ts`) trong khi app dùng Spring JWT. Chưa thấy luồng liên kết JWT này với Supabase session trong các file đã đọc. Giới hạn MIME/10MB ở browser có thể bị bỏ qua. Cần kiểm chứng RLS/storage policy và bucket limits thực tế; không kết luận anon đã được cấp upload chỉ từ source. [Supabase Storage Access Control](https://supabase.com/docs/guides/storage/security/access-control) xác định quyền upload tại policies trên storage.objects.

Migration local không chứa cấu hình RLS/grants cho dữ liệu ứng dụng. Nếu database được expose qua Supabase Data API, phải xác minh quyền anon/authenticated và RLS cho users/orders/auth_sessions; config dashboard có thể khác migration. Cũng chưa kiểm tra TLS, CSP thực tế, reverse proxy, secret rotation, network DB và backup/restore của deployment.

## Validation

Lệnh: `cd backend && ./mvnw -q -Dtest=OrderServiceTest,AuthServiceTest,SecurityAndMailRegressionTest,GuestOrderSecurityTest,ApiObservabilitySecurityTest,DevAuthRegressionTest test`.

| Suite | Pass | Skip | Fail/error |
|---|---:|---:|---:|
| AuthServiceTest | 35 | 0 | 0 |
| OrderServiceTest | 33 | 0 | 0 |
| GuestOrderSecurityTest | 5 | 0 | 0 |
| SecurityAndMailRegressionTest | 6 | 0 | 0 |
| DevAuthRegressionTest | 1 | 0 | 0 |
| ApiObservabilitySecurityTest | 0 | 2 | 0 |
| Tổng | 80 | 2 | 0 |

Hai test observability yêu cầu UITMERCH_TEST_DATABASE_URL trỏ tới PostgreSQL test riêng, không có ở lần chạy này. Các test có sẵn không chứng minh đã bao phủ payment abuse, concurrency PostgreSQL hoặc cấu hình production.

Audit artifacts: [frontend-audit.json](frontend-audit.json), [frontend-production-audit.json](frontend-production-audit.json).

Ưu tiên tiếp theo: xử lý S1/S2 và ngăn profile dev/docker ở production; xác minh S5 và Supabase quyền thực tế; phân loại/nâng dependency; sau đó chốt mô hình gateway và payment lifecycle. Trước mọi sửa function/class/method, cần GitNexus upstream impact và báo blast radius theo AGENTS.md; báo cáo này không sửa symbol.
