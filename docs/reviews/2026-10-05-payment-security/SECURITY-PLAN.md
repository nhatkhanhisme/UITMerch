# Kế hoạch bảo mật UITMerch

Ngày 2026-10-05. Baseline: local `main`, commit `cc67c35`. Đầu vào: [security review](README.md) và hai npm audit artifacts trong cùng thư mục.

## Mục tiêu và phạm vi

Bảo vệ tài khoản, dữ liệu đơn hàng và tồn kho trước khi tích hợp thanh toán online. Hoàn thành hardening của app COD trước; gateway, webhook và refund được thiết kế trong bước payment tiếp theo. Đây là kế hoạch gốc. Tiến độ triển khai, kết quả kiểm chứng và các release gate còn lại được ghi trong [IMPLEMENTATION.md](IMPLEMENTATION.md).

Ưu tiên dựa trên khả năng khai thác và ảnh hưởng thực tế. S5 chỉ thành CRITICAL nếu môi trường public bật dev/docker; quyền Supabase và headers production chưa được xác nhận. Không đánh dấu chúng đã an toàn chỉ từ code local.

## Thứ tự triển khai

| Giai đoạn | Phạm vi | Điều kiện hoàn thành |
|---|---|---|
| 1 — P0 | Production profile, OTP/demo data, quyền DB/storage, secrets | Không có đường public vượt xác thực hoặc lộ dữ liệu trực tiếp |
| 2 — P1 | Checkout abuse, idempotency, guest access, rate limiting | Không giữ stock vô hạn bằng guest checkout; retry không tạo đơn trùng |
| 3 — P1 | SSE và session, CSRF/CORS, CSP | Token không nằm trong URL/localStorage; session và quyền được kiểm chứng |
| 4 — P1/P2 | Dependencies, uploads, CI, logs | Advisory được xử lý/phân loại; kiểm soát backend và pipeline có bằng chứng |
| 5 — Release gate | PostgreSQL, browser E2E, staging và rollback | Các test bảo mật bắt buộc pass và cấu hình live được kiểm tra |

Giữ từng phần thành PR/commit nhỏ có migration tương thích; tránh gộp toàn bộ auth, stock và dependency upgrade vào một thay đổi.

## 1. Khóa cấu hình production và xác minh đường truy cập dữ liệu

**Thực hiện**

- Thêm môi trường production rõ ràng; khi deployment đánh dấu production, ứng dụng phải từ chối khởi động nếu dev/docker được bật. Không dùng tên profile `docker` để chỉ việc chạy container.
- Giới hạn DevOtpController, DevEmailService và DevDataInitializer cho local/test bằng cờ explicit mặc định tắt; production không đăng ký endpoint trả OTP hoặc seed tài khoản demo.
- Đối chiếu profile/env của deployment thực tế; tìm tài khoản demo đã tồn tại, xử lý từng tài khoản và thu hồi session nếu cần. Không xóa user hàng loạt theo tên/email.
- Scan secret trong source và lịch sử Git bằng công cụ có redaction. Nếu phát hiện secret thật, rotate ở nhà cung cấp, thu hồi credential cũ và kiểm tra audit log; chỉ xóa khỏi source là chưa đủ.
- Xác minh Supabase Data API exposed schemas, grants và RLS của users/orders/auth_sessions/OTP. Với backend Spring là chủ sở hữu truy cập, ưu tiên ngăn anon/authenticated truy cập bảng riêng tư qua Data API. Kiểm tra cả views/functions, không áp một policy auth.uid() cho mọi bảng.
- Xác minh bucket policy và credential frontend; không có secret/service-role/S3 secret trong bundle. Kiểm tra không đăng nhập có thể upload, sửa, xóa asset của user/org khác hay không bằng dữ liệu test riêng.

**Nghiệm thu**

- Prod không khởi động với profile nguy hiểm; khi prod chạy, endpoint dev không có handler khả dụng và không trả OTP cho mọi role.
- Test từ anon/authenticated không đọc được dữ liệu private qua Data API; không ghi được asset ngoài quyền sở hữu.
- Có bảng kiểm môi trường ghi kết quả thực tế cho TLS, origins, trusted proxy, Swagger, actuator, DB network và secrets. Thiếu quyền xem cấu hình phải ghi là chưa xác minh và giữ gate tương ứng chưa hoàn thành.

## 2. Chống lạm dụng đơn hàng và bảo vệ guest

**Checkout policy**

- Dùng policy chung cho public, cart, instant và campaign checkout; normalize merch ID trùng trước khi áp quota. Kiểm tra quantity theo tổng của mỗi merch, tổng items, giá trị đơn và số đơn đang pending. Mọi giới hạn được thực thi ở service, không chỉ DTO/frontend.
- Giá vẫn lấy từ backend, không thay thế bằng client total. Giá trị quota là cấu hình business cần chốt bằng dữ liệu bán hàng, không dùng con số tùy ý như một bảo đảm chống abuse.
- Guest phải xác minh email bằng OTP trước khi tạo đơn giữ stock; giữ rate limits cho gửi/xác minh OTP và thêm challenge khi có tín hiệu lạm dụng. Có lựa chọn checkout tài khoản để không phụ thuộc guest credentials.
- Thêm idempotency cho checkout thường: key gắn với authenticated user hoặc guest checkout session đã xác minh, fingerprint payload, unique constraint và kết quả lưu bền vững. Retry cùng payload trả cùng tập order IDs; cùng key khác payload bị từ chối. Bảo đảm một checkout nhiều org commit atomically.
- Không coi giới hạn quantity riêng lẻ là đủ: thêm quota pending stock theo actor và hạn mức tổng, bao phủ guest lẫn tài khoản verified.

**Tồn kho và hết hạn**

- Phân biệt PENDING COD chờ organizer xác nhận với reservation online chờ payment. Trước mắt định nghĩa thời hạn xác nhận COD theo business; không áp timer payment ngắn lên mọi đơn COD.
- Thêm expiration timestamp và worker cho các đơn pending thuộc policy; khi tạo/confirm/cancel/expire dùng cùng khóa order và transition guards. Chỉ hoàn stock một lần và record history trong cùng transaction.
- Đơn hiện có chỉ backfill timeout theo policy đã chốt; không tự hủy hàng loạt. Index phục vụ worker, xử lý theo batch, retry an toàn và tránh tranh chấp nhiều instance.
- Expiry worker chỉ xử lý trạng thái hợp lệ; order đã CONFIRMED/READY/COMPLETED không bị hết hạn theo policy pending. Campaign giữ quy tắc riêng và phải có regression test để tránh hoàn stock hai lần.

**Guest tracking**

- Đổi UUID + email thành credential tracking riêng: random đủ entropy, hash lưu ở DB, giới hạn scope read-only theo order/checkout group, có expiry/revocation.
- Email gửi link nhận credential; nếu dùng URL, ưu tiên fragment rồi đổi thành header trong browser, loại bỏ fragment sớm, đặt Referrer-Policy phù hợp và không tải third-party tracking trên trang nhận credential. Không đưa secret vào query/access logs.
- Trả DTO tracking tối thiểu, che PII theo nhu cầu; Cache-Control no-store. Endpoint UUID+email cũ phải ngừng cho phép đọc, không để fallback phá cơ chế mới. Đơn cũ có luồng cấp credential qua email đã xác minh, không tự mất quyền theo dõi.
- Limiter chia sẻ bằng backend store hoặc edge: theo IP/actor/action, bounded keyspace và TTL. Định nghĩa hành vi khi store lỗi cho từng endpoint; checkout/OTP không được lặng lẽ bỏ toàn bộ giới hạn. Trả 429/Retry-After. Proxy chỉ tin hop được cấu hình và chuỗi header được chuẩn hóa.

**Nghiệm thu**

- Một merch lặp nhiều lần trong payload không vượt quota; giả email hoặc đổi IP không bỏ được mọi kiểm soát giữ stock.
- Retry và hai request đồng thời cùng key chỉ tạo một tập đơn và trừ stock một lần.
- Confirm/cancel/expire đồng thời không âm stock, không restore hai lần; worker restart và nhiều instance vẫn đúng.
- UUID+email không mở được đơn; credential sai/hết hạn/thu hồi hoặc của đơn khác bị từ chối; response không có PII dư thừa.

## 3. Giảm nguy cơ chiếm session

**SSE trước, session tiếp theo**

- Thay native EventSource mang JWT trong query bằng SSE dùng fetch/Authorization header; quản lý parser, abort, reconnect/backoff, refresh token và last-event semantics. Backend từ chối JWT query sau khi frontend mới đã deploy.
- Nếu bắt buộc giữ EventSource, dùng ticket ngắn hạn, một lần, chỉ mở đúng stream; JWT API không được dùng làm ticket. Chọn một phương án khi triển khai, không duy trì hai cơ chế dài hạn.
- Hạ access-token TTL mục tiêu ban đầu xuống 15 phút, cấu hình được; kiểm thử refresh và SSE reconnect trước rollout.
- Access token chỉ giữ trong memory; refresh token dùng cookie HttpOnly/Secure. API refresh/login không trả refresh token cho JavaScript. Giữ refresh hash, khóa rotation, authVersion và revocation ở backend.
- Chốt topology trước cookie migration: ưu tiên frontend/API cùng site qua domain hoặc reverse proxy. Nếu khác site, SameSite=None cần Secure và có rủi ro third-party cookie bị chặn; phải kiểm thử trên browser mục tiêu, không tự chọn None như cách sửa mọi lỗi cookie.
- Cookie host-only, path tối thiểu phù hợp refresh/logout; refresh và logout cookie-authenticated được bảo vệ CSRF bằng token phù hợp cùng kiểm tra Origin, CORS allowlist exact. Bao phủ login CSRF. CORS không thay thế CSRF.
- Làm bootstrap session khi reload và điều phối refresh giữa tab; kiểm thử simultaneous refresh, tránh logout giả do rotation race. Đánh giá phát hiện refresh reuse với chiến lược concurrency rõ ràng, không revoke tất cả session một cách tùy tiện.
- Migration client xóa token persisted cũ; cutover thu hồi session cũ hoặc hỗ trợ compatibility rất ngắn có ngày tắt rõ ràng. Logout xóa cookie đúng path/domain và mọi private cache; logout/reset password/role change/disable user tiếp tục revoke quyền tương ứng.
- CSP bắt đầu Report-Only để kiểm kê script/style/connect/img thực tế; chuyển enforce sau khi kiểm thử giao diện/WebGL/storage/SSE. Bổ sung frame-ancestors, nosniff và Referrer-Policy; kiểm tra HSTS trên HTTPS endpoint, không bật includeSubDomains khi chưa xác minh mọi subdomain.

**Nghiệm thu**

- Không có bearer/refresh token trong localStorage, query URL, logs hoặc analytics; cookie có flags đúng trên deployment.
- Request CSRF giả bị từ chối trên login/refresh/logout; nguồn không được phép không nhận credentialed response.
- Reload, đa tab, refresh đồng thời, hết hạn token và SSE reconnect hoạt động; logout/reset password/đổi role/khóa account vô hiệu hóa quyền cũ.
- Tests cross-user/cross-org bao phủ order, pickup QR, history, notification, campaign, admin và upload; không dựa vào frontend route guard.

Khuyến nghị session/cookie và CSRF được đối chiếu với [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) và [OWASP CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).

## 4. Dependencies, upload và vận hành

- Chạy SCA Maven resolved dependencies và cả frontend/scripts npm lockfiles; tạo inventory runtime/build-only. Nâng phiên bản theo advisory, không dùng audit fix --force. Với advisory chưa thể sửa, ghi điều kiện reachability, biện pháp giảm thiểu, người phụ trách và hạn xem lại.
- Với auth Spring hiện tại, ưu tiên upload qua backend hoặc signed upload URL do backend cấp sau khi kiểm tra user/org ownership. Giới hạn size/MIME ở bucket/backend; đọc magic bytes và decode ảnh để kiểm tra nội dung. Ban đầu không nhận SVG chưa sanitize, không tin file.type/extension. Chuẩn hóa file path và kiểm soát URL asset gắn vào profile/org/merch.
- Xác minh storage policies thực tế theo [Supabase Storage Access Control](https://supabase.com/docs/guides/storage/security/access-control). Không mở anon write để chữa lỗi upload. Public read ảnh không đồng nghĩa public write.
- CI có security regression tests, dependency scan và secret scan redacted; fail khi finding high/critical có đường khai thác liên quan chưa xử lý hoặc exception hợp lệ hết hạn.
- Không log OTP, Authorization, cookies, tracking credentials hoặc toàn request body private. Thêm audit events cho auth/admin/stock transitions với actor, action, target, outcome và correlation ID; định nghĩa retention/access.
- Theo dõi tăng 401/403/429, OTP failures, quota stock, expiry backlog và session errors. Alert có ngưỡng từ baseline; kiểm tra backup restore trên dữ liệu riêng và credential rotation runbook.

## 5. Kiểm chứng và rollout

1. Dùng database PostgreSQL test riêng để chạy migration upgrade, pessimistic-lock/concurrency tests và hai ApiObservabilitySecurityTest đã skip trong audit. Không chạy test phá dữ liệu trên production.
2. Chạy backend security/integration tests, frontend unit tests, build và browser E2E cho customer/guest/organizer/admin. Test browser cookie/CORS trên topology HTTPS như production.
3. Kiểm tra staging từ ngoài ứng dụng: dev endpoint không public; actuator chỉ ADMIN; Data API/storage quyền đúng; headers, trusted proxy và log redaction đúng. Lưu evidence có redaction và timestamps.
4. Rollout theo expand → deploy reader/writer tương thích → enforce → remove legacy. Theo dõi lỗi auth, conversion checkout, stock mismatch, mail delivery và limiter false positives.
5. Rollback bằng version ứng dụng tương thích schema mới; không xóa cột hoặc chạy migration hạ cấp làm mất dữ liệu. Không mở lại endpoint OTP/public data hay cơ chế tracking yếu để chữa sự cố; có thể tạm dừng guest checkout và hướng người dùng qua account.

**Gate trước payment:** P0 đã xác minh; stock abuse và session exposure được xử lý; không còn high/critical exploitable chưa có biện pháp; tests bắt buộc không skip; có rollback/runbook. Sau gate mới triển khai thiết kế payment ledger, webhook signature/dedupe, paid fulfillment guards, refunds và reconciliation nêu trong review.

## Phạm vi ảnh hưởng và chia PR

GitNexus CLI upstream impact tại baseline (MCP không được expose trong session):

| Target | Trực tiếp | Phạm vi graph | Risk graph | Cách đánh giá triển khai |
|---|---|---|---|---|
| createPublicOrder | guestCheckout, createGuestOrder | 2 symbols; guest checkout, 3 traces được liệt kê | LOW | HIGH về business vì thay đổi giữ stock và guest flow |
| getGuestOrderByEmail | trackGuestOrder | 1 symbol; guest tracking, 3 traces | LOW | MEDIUM, cần migration quyền đọc đơn cũ |
| JwtAuthenticationFilter | 2 dependents trực tiếp | 6 symbols; graph không liệt kê flow | LOW | HIGH cho rollout auth; servlet filter có ảnh hưởng toàn API |

Graph bị giới hạn số flows; số liệu không thay thế test tích hợp và phân tích middleware. Trước sửa từng function/class/method phải chạy impact lại, báo direct callers/flows/risk và cảnh báo HIGH/CRITICAL theo AGENTS.md. Trước commit chạy detect_changes và kiểm tra phạm vi; không rename bằng replace.

| PR đề xuất | Nội dung | Phụ thuộc |
|---|---|---|
| A | Production guard, dev feature flags và regression tests | Bắt đầu ngay |
| B | Evidence hạ tầng, secrets/SCA, quyền DB/storage | Bắt đầu sau khi có quyền đọc cấu hình |
| C | Checkout policy, verification, shared limiter và idempotency | A; chốt policy business |
| D | Pending expiry và tracking credential migration | C; policy COD/campaign |
| E | SSE Authorization, query-token removal và log redaction | A |
| F | Cookie/session migration, CSRF/CORS và headers | E; topology domain đã kiểm chứng |
| G | Dependency upgrades và upload backend/signed URL | B; chia commit theo module |
| H | PostgreSQL/browser gates, staging evidence và runbook | Các PR liên quan đã hoàn thành |

Chưa ước lượng ngày hoàn tất khi chưa xác minh hạ tầng/topology và policy guest/COD. Đây là các quyết định cần chốt trong triển khai, không phải lý do dừng việc chuẩn bị code/tests độc lập.
