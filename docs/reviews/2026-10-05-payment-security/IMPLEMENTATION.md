# Security hardening — trạng thái triển khai

Ngày 2026-10-05; nhánh local `security/app-hardening`, baseline `cc67c35`. Chưa push hoặc deploy. Review và audit ban đầu trong thư mục này là bằng chứng baseline, không đại diện cho dependency hiện tại.

## Đã triển khai local

- Production fail closed: chặn dev/docker, OTP debug, seed/mock mail, H2 console, schema destructive, cookie không Secure, limiter riêng từng instance và tắt expiry. Origin production phải là HTTPS chính xác; signing key test công khai bị từ chối. Migration V46 vô hiệu hóa tài khoản còn dùng nguyên mật khẩu seed và thu hồi phiên, giữ dữ liệu nghiệp vụ.
- Checkout: guest OTP email trước giữ stock; giới hạn 10/SKU và 3 nhóm pending/buyer dùng khóa DB; UUID idempotency và fingerprint durable. Authenticated/guest cùng email dùng chung quota. Organization và organizer phải active, verified. Pending COD mới hết hạn 48 giờ, hoàn stock một lần; campaign/confirmed giữ quy tắc riêng, không backfill expiry đơn lịch sử.
- Guest tracking: token ngẫu nhiên có hạn, hash trong DB, gửi qua email và header; bỏ tra cứu UUID + email. Redact thông tin cá nhân và giới hạn request. Rate limiter PostgreSQL dùng chung, lỗi storage trả lỗi thay vì bỏ qua giới hạn.
- Auth: refresh token host-only HttpOnly/Secure/SameSite cookie qua frontend `/api`; access token chỉ trong memory. CSRF ký số, kiểm tra Origin, xoay refresh và thu hồi logout; khóa refresh nhiều tab và xóa cache riêng tư khi đổi tài khoản. Bỏ token query URL của SSE, dùng Authorization header.
- Upload: mọi write qua backend; kiểm tra chủ sở hữu, MIME thực, kích thước/dimensions, decode và reencode ảnh loại metadata/trailing payload. Bỏ Supabase client trực tiếp khỏi frontend.
- Dependency: nâng Spring Boot, Vite/Vitest/router và các runtime dependency bị advisory. Scanner npm + OSV Maven runtime không còn advisory chưa xử lý tại thời điểm scan; production npm không có advisory. Một exception build-only của braces hết hạn 2026-11-04, xem `.security/vulnerability-exceptions.json` và [bằng chứng](dependency-verification.json). Đây không phải cam kết rằng dependency không có lỗ hổng chưa công bố.
- CI: gate test, dependency và redacted source-secret scan trước deploy. Audit mutation log chỉ action/actor/outcome/trace, không body/query/token. Headers chống framing/MIME sniffing, HSTS, CSP enforced cho base/object/frame/form; script CSP vẫn report-only để xác minh staging.

## Kiểm chứng

- PostgreSQL backend: 314 tests, 0 failure/error; 1 performance benchmark opt-in skipped. Guard cuối cùng được chạy thêm riêng: 10 tests pass.
- Frontend: 66 unit tests pass; production TypeScript/Vite build pass.
- Browser regression: 92 tests pass; 2 test auth-cookie/legacy-storage bổ sung pass khi chạy riêng (tổng 94 browser tests).
- Dependency scan: 0 unresolved findings, 1 exception build-only có hạn. Source scan hiện tại không có secret finding; không chứng minh lịch sử Git đã sạch.
- Browser dùng API fixtures local; backend cookie flags/CSRF/rotation được kiểm tra trên PostgreSQL. Chưa kiểm chứng cookie/proxy trên HTTPS production. GitHub CI chưa chạy vì chưa push. GitNexus phát hiện 198 symbol thay đổi trong các file đã theo dõi và 112 luồng liên quan, rủi ro tổng thể CRITICAL; symbol mới chưa có trong index baseline, đã được kiểm tra qua test riêng.

## Chưa được xác minh trên production

Supabase đã truy cập được ref của app sau reconnect; [live audit](SUPABASE-LIVE-AUDIT.md) phát hiện hai policy cho phép anon upload và live mới tới V43. Render yêu cầu đăng nhập lại. Vì vậy grants/RLS/storage policy, profile/env thực tế, proxy/HTTPS headers/cookie và vận hành worker chưa được đánh dấu hoàn tất. Hai Gemini API key từng nằm trong lịch sử Git phải được thu hồi tại provider; xóa khỏi source hiện tại không thu hồi chúng.

Strict script/connect CSP, retention và truy cập audit log tập trung cần kiểm chứng staging/production. Các mục này là release gate trước thanh toán online, xem [ROLLOUT.md](ROLLOUT.md).
