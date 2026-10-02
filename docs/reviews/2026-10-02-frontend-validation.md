# UITMerch — Frontend integration and deployment review

Ngày kiểm tra: **02/10/2026**, nhánh **`refactor/fe`**, xuất phát từ `main` / `7b1ab1f`. Các thay đổi trong báo cáo này được kiểm tra tại workspace; chưa push hoặc gọi deploy production.

## Những gì đã triển khai

- Refresh token tập trung và điều phối giữa các tab; session/cache/cart được dọn khi logout hoặc đổi tài khoản. Refresh đang chạy không khôi phục session cũ; request 403 không gây refresh loop.
- Notification panel tách khỏi navbar, theo token mới khi reconnect, tải lại REST records/count, dẫn đúng resource và không tự mark-read-all khi mở. Hỗ trợ wire field `read` của backend và field `isRead` cũ.
- Restock subscriptions trên sản phẩm/wishlist và trang cài đặt; following với preferences và rollback khi API lỗi. Own list được đọc đủ các page trước khi kết luận trạng thái nút.
- Customer READY pickup QR, history riêng tư, Guest email/receipt tracking và receipt exchange; Organizer camera/manual verify rồi xác nhận check-in. Credential chỉ giữ trong memory; receipt URL được loại sau khởi tạo.
- Analytics theo tổ chức/ngày, tách giá trị đơn hoàn thành khỏi giá trị PAID; tỷ lệ hủy đổi từ fraction sang phần trăm. Dùng bảng dữ liệu, chưa thêm chart.
- Public campaign list/detail, variant từ API thật, Customer reservations và Organizer create/detail/cancel. Request ID + payload được giữ cố định để người dùng thử lại sau timeout/reload; không tự retry mutation không rõ kết quả.
- Product/cart/wishlist và thao tác fulfill của Organizer kiểm tra campaign context; UI khóa khi chưa xác minh được. Checkout thường lưu order ID, dùng tổng tiền server và dẫn về đúng private/guest route.
- Bốn API GET read-only bổ sung purchase/order context và owner campaign detail; quyền/ownership kiểm tra ở backend. Không thêm migration sau V41. `fulfillmentAllowed` chỉ kiểm tra điều kiện campaign, không thay quyền/transition của mutation API.
- Thiếu optional Supabase Storage config không còn làm SPA crash khi import; upload báo lỗi cấu hình. Không đổi buckets, RLS hoặc remote storage.

## Kiểm thử local

| Kiểm tra | Kết quả |
|---|---|
| Backend, PostgreSQL 17 + pgvector | **257 tests / 28 classes; 0 failures, 0 errors, 0 skips** |
| Backend package | Maven package thành công, executable JAR 83 MB |
| Frontend Vitest | **40 tests / 6 files**, thành công |
| Frontend TypeScript + Vite | Production build thành công |
| Playwright Chromium | **17 tests**, thành công |
| GitHub Actions YAML | `actionlint` 1.7.12 thành công |
| Docker healthcheck | Lệnh healthcheck chạy thành công trong JRE base image với `PORT=5188` |
| Git diff whitespace | `git diff --check` thành công |

Backend chạy bằng harness database/container riêng, nguồn copy không có project `.env`:

```bash
UITMERCH_TEST_POSTGRES_IMAGE=public.ecr.aws/supabase/postgres:17.6.1.167 backend/scripts/test-postgres.sh -q
# Trong source copy do harness tạo:
./mvnw -q -DskipTests package
```

Report Maven ở `/tmp/uitmerch-backend-test.HYrvoQ/target/surefire-reports`. Hash của hai file Java mới và test context trùng với source copy đã test. Dùng Java 21 đã sửa tại user environment; không dùng `/usr/bin/java` bị lỗi ELF.

Frontend:

```bash
npm test --prefix frontend
npm run build --prefix frontend
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/home/nhatkhanhh/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome npm run test:e2e --prefix frontend
```

40 test gồm refresh concurrency/logout/account switch/revocation, private cache cleanup, paginated lookup/fail-closed, error/date/percentage/read adapters, SSE lifecycle, subscription rollback, reservation replay/account-switch race, verify-before-check-in, receipt response và optional storage config.

Lượt browser tổng hợp có 16 test đạt; test account checkout gặp selector fixture trùng số điện thoại và đã chạy lại đạt sau khi sửa selector. Tổng cộng **17/17 kịch bản đã đạt**, không có lỗi sản phẩm còn treo. Các kết quả kiểm tra áp dụng cho final workspace, không phải chạy lại độc lập ở từng commit trung gian.

17 browser scenarios gồm campaign anonymous/reservation timeout+reload, restock/follow preferences, customer QR/history, organizer verify/check-in, guest/mobile tracking, wrong-role guard, analytics dates, **hai tab thật cùng gặp 401 và chỉ rotate một lần**, checkout guest/cart/account, purchase-context failure, camera denied, mobile settings và notification `read`/viewport. Lỗi fixture của test account checkout đã sửa để dùng thông tin profile được prefill.

### Giới hạn của bằng chứng

- Browser tests dùng API contract fixtures qua interception. Đây không phải một luồng browser → live backend → SMTP/camera thực tế. Backend authorization/concurrency/migrations được test riêng trên PostgreSQL với MockMvc/service.
- Không tạo đơn thử, gửi email receipt hoặc upload ảnh lên production. Không kiểm thử camera vật lý; đã kiểm thử fallback khi bị từ chối quyền.
- Chưa build toàn bộ Docker app image trong máy này vì filesystem root thiếu dung lượng. Đã package JAR, khởi động dev instance riêng và kiểm tra health command trong JRE image.
- Vite còn cảnh báo chunk lớn của bundle dùng chung. QR encoder và camera decoder được lazy import. Chưa refactor toàn bộ dashboard/catalog cũ.
- GitNexus impact đã chạy trước sửa symbol; shared navbar/storage/error helpers có mức HIGH/CRITICAL và được cảnh báo. Graph thiếu một số edges auth, nên đã đối chiếu import/source. Detect-changes chạy trước từng commit; đồ thị không thay thế test thực tế.

## Vercel — đã kiểm tra read-only

- [GitHub deployment / Vercel detail](https://vercel.com/nhat-khanh-s-projects/uitmerch/J3VXyw6seFr7mkHJw3CkDrP2K9bD): trạng thái **success**, SHA `7b1ab1fcb635bc21788e24acad4e3f2aad356eb2`, tạo `2026-10-02T05:21:47Z`.
- [Production alias](https://uitmerch.vercel.app) và deep routes `/campaigns`, `/orders/example`, `/guest-orders` trả HTTP 200 HTML, xác nhận SPA rewrite. Bundle đang deployed là bản cũ; HTTP 200 không chứng minh các màn hình mới đã được phát hành.
- API origin lấy từ public bundle: `https://uitmerch-backend.onrender.com`.
- Không có quyền đọc private Vercel build settings/logs trong phiên này. Root/build/output/env hướng dẫn đã cập nhật trong README.

## Render — phát hiện lệch backend đang chạy

| Request production | Kết quả |
|---|---|
| `GET /api/v1/public/events?size=1` | **200**, `success:true`, có dữ liệu |
| `GET /api/v1/public/campaigns?size=1` | **404**, `success:false` |
| Event GET với Origin production Vercel | CORS trả đúng `https://uitmerch.vercel.app`, credentials enabled |

Một số request đầu timeout khi service chưa phản hồi; lượt sau events thành công. Chưa đủ bằng chứng kết luận nguyên nhân là cold start hay failed deployment.

Render connector yêu cầu **reauthentication**, đã thông báo và thử lại nhưng vẫn không đọc được service/deploy logs. Vì vậy chưa xác nhận deployed commit, Flyway V41 hoặc nguyên nhân campaign 404. Cần kết nối lại Render rồi kiểm tra deploy branch/commit, startup/Flyway logs và route readiness. Không suy đoán migration đã chạy chỉ từ pipeline xanh.

## CI và thứ tự phát hành

[Run main hiện có](https://github.com/nhatkhanhisme/UITMerch/actions/runs/36968549832) thành công, nhưng workflow cũ thiếu test PostgreSQL URL nên có thể bỏ qua các suite phụ thuộc database; deploy-hook HTTP thành công chỉ xác nhận nhận trigger.

Workflow mới tại `.github/workflows/maven.yml` chạy PostgreSQL harness, package backend, frontend unit/build/browser, lưu reports và chỉ trigger Render sau cả hai job thành công trên `main`. Bước sau hook retry public campaign endpoint để tránh báo xanh khi API này còn 404. Đây là kiểm tra endpoint availability, **không chứng minh Render đã chạy đúng SHA**. Workflow mới đã lint local, chưa chạy trên GitHub vì chưa push.

Thứ tự cần dùng khi phát hành:

1. Xác nhận Render dùng nhánh/commit đúng, database hỗ trợ pgvector và migrations đến V41 hoàn tất; kiểm tra campaigns cùng bốn context/detail endpoint mới.
2. Vercel root `frontend`, install `npm ci`, build `npm run build`, output `dist`; cấu hình `VITE_API_BASE_URL` trước build.
3. CORS Render cho phép chính xác production/preview origins. Không wildcard mọi project `*.vercel.app`.
4. Kiểm tra production customer/organizer flows bằng dữ liệu được phép sau deploy. Vercel Git integration độc lập; workflow này không gate Vercel deployment.

Chưa push, redeploy hoặc thay cấu hình cloud trong lượt này. Phần kiểm tra log/cấu hình private còn phụ thuộc quyền truy cập Render/Vercel.

## Commit thực tế

- `cb49266` — feat(backend): expose ownership-checked campaign purchase and order context
- `a9a364b` — fix(frontend): coordinate refresh rotation and isolate private session state
- `cad797d` — fix(frontend): keep the app usable without optional storage configuration
- `cdd74f0` — refactor(frontend): reconcile persisted notifications across SSE reconnects
- `b2d2819` — feat(frontend): add restock and organization-follow preferences
- `53ded14` — feat(frontend): add pickup QR, guest tracking and verified check-in
- `6b5ca78` — feat(frontend): add organization-scoped analytics and date filters
- `f9e072f` — feat(frontend): add campaign management and idempotent reservations
- `f86d8d7` — feat(frontend): integrate feature routes and campaign-aware checkout flows
- `cffcb3b` — ci: run full-stack checks before Render deployment and honor its port
- Commit tài liệu cuối cập nhật plan, README và báo cáo này.

Hash source/config và summary kiểm tra: [validation JSON](2026-10-02-frontend-validation.json).

Navbar được điều chỉnh trong lượt review tiếp theo; xem [navbar follow-up](2026-10-02-navbar-review.md) với ảnh desktop/mobile và lượt kiểm tra frontend mới: 40 unit/component tests, 30 browser tests và production build đạt. Hash source trong JSON phía trên ghi nhận bản tích hợp trước lượt sửa navbar này.
