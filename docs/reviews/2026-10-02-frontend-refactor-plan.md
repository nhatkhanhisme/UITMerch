# UITMerch — Kế hoạch refactor frontend và tích hợp backend

Ngày lập: **02/10/2026**. Nhánh: **`refactor/fe`**, tạo từ `main` tại commit `7b1ab1f`.

Trạng thái: **đã triển khai trên `refactor/fe`; xem [kết quả kiểm tra](2026-10-02-frontend-validation.md)**. Backend đã có restock, QR pickup/history, following, analytics và preorder campaigns, với migration đến V41. Hợp đồng đang chạy được đối chiếu từ controller/DTO và [tài liệu tính năng backend](2026-10-02-backend-features.md), thay vì coi mọi đề xuất trong [plan backend cũ](2026-10-01-backend-plan.md) là tính năng đã có.

## 1. Mục tiêu và phạm vi

- Tích hợp đầy đủ các API mới vào trải nghiệm Customer, Organizer và Guest.
- Sửa những điểm frontend chưa tương thích với session revocation, refresh rotation, phân trang và lỗi API của backend.
- Tách các phần liên quan trong dashboard, API, types và notification để thêm tính năng theo từng commit có thể review.
- Giữ React 18, Vite, TypeScript, Zustand và giao diện hiện tại. Tận dụng `@tanstack/react-query` đã có trong dependencies cho dữ liệu server ở các luồng được chỉnh sửa; không nâng React chỉ để dùng API của React 19.
- Việc bổ sung hợp đồng backend được liệt kê riêng ở mục 8. Đã bổ sung bốn API đọc context/detail ở backend; không thêm migration hoặc thay chính sách storage. Chưa push hoặc triển khai các thay đổi này.

## 2. Hiện trạng lúc lập kế hoạch (trước triển khai)

| Vị trí hiện tại | Hiện trạng | Điều chỉnh cần thiết |
| --- | --- | --- |
| [api/client.ts](../../frontend/src/api/client.ts), [api/auth.ts](../../frontend/src/api/auth.ts), [authStore.ts](../../frontend/src/stores/authStore.ts) | Có Axios client và lưu access/refresh token, nhưng chưa có hàm gọi `/auth/refresh` hoặc cơ chế refresh tập trung | Điều phối refresh, thay token pair, xử lý session bị thu hồi và tránh request retry vô hạn |
| [useNotificationStream.ts](../../frontend/src/hooks/useNotificationStream.ts) | EventSource dùng `?token=`, reconnect bằng access token trong closure | Reconnect theo session/token mới; đồng bộ lại notification sau khi mất kết nối |
| [TopNavBar.tsx](../../frontend/src/components/home/TopNavBar.tsx) | Notification gắn vào navbar; customer click chỉ mark-read; mở panel đánh dấu tất cả; mọi customer event phát `order-status-changed` | Tách notification panel/hook, điều hướng theo loại và related ID, invalidation đúng domain |
| [types/shared.ts](../../frontend/src/types/shared.ts) | Thiếu types cho 5 feature và 3 related ID mới; một số view model lấy thông tin size/color từ mock | Types theo DTO thật; không dùng size/color minh họa để chọn SKU hoặc quyết định đặt hàng |
| [main.tsx](../../frontend/src/main.tsx) | Chưa có route campaign, guest tracking, following/restock settings; chưa có QueryClientProvider | Bổ sung route và provider; dùng route guard chung cho các luồng mới |
| [ProductDetailPage.tsx](../../frontend/src/pages/ProductDetailPage.tsx) | Mua ngay, cart, wishlist và form gọi public checkout; public checkout success không giữ response order | Phân biệt rõ checkout tài khoản/guest/campaign; giữ order ID để theo dõi và nhận hàng |
| [OrderDetailPage.tsx](../../frontend/src/pages/OrderDetailPage.tsx), [api/order.ts](../../frontend/src/api/order.ts) | Xem/hủy đơn, chưa có QR, guest receipt exchange hay audit history | Thêm pickup credential và timeline từ API |
| [OrganizationDetailPage.tsx](../../frontend/src/pages/OrganizationDetailPage.tsx) | Có sản phẩm/sự kiện, chưa có follow/preferences | Follow/unfollow, tùy chọn thông báo và liên kết campaign |
| [OrganizerDashboardPage.tsx](../../frontend/src/pages/OrganizerDashboardPage.tsx) | Khoảng 2.000 dòng, các tab merch/events/orders/pickup/profile nằm cùng file | Tách tab liên quan và thêm analytics/campaign/scan; giữ org selector làm scope chung |
| [AdminDashboardPage.tsx](../../frontend/src/pages/AdminDashboardPage.tsx) | Bộ lựa chọn trạng thái tổ chức có `SUSPENDED` | Đối chiếu enum backend chỉ có `PENDING`, `ACTIVE`, `INACTIVE`; loại lựa chọn không được API hỗ trợ |
| [sessionCache.ts](../../frontend/src/lib/sessionCache.ts), [cartStore.ts](../../frontend/src/stores/cartStore.ts) | Có cache sessionStorage và cart store; logout ở navbar chỉ clear auth | Xóa dữ liệu riêng tư khi đổi session; kiểm tra invalidation catalog/stock/cart và cô lập cache theo user/org |
| [package.json](../../frontend/package.json) | Có `dev`, `build`, `preview`, chưa có test script | Thiết lập kiểm thử các luồng có trạng thái, concurrency, credential và retry |

Khảo sát dùng GitNexus qua CLI vì MCP GitNexus không có trong phiên này. Index đã được làm mới. Query tìm được luồng `AppRouter → HomeFixedChrome → TopNavBar → useNotificationStream → connect`; context xác nhận `PurchasePanel` được gọi từ `ProductDetailPage`, và `useNotificationStream` được gọi từ `TopNavBar`. Analyzer cảnh báo đồ thị không chứa mọi flow, nên các kết luận được đối chiếu thêm với source.

## 3. Nền tảng cần làm trước các feature — P0

### 3.1. Auth, refresh và lỗi API

- Thêm API refresh và một lần refresh dùng chung cho các request đồng thời trong cùng tab. Không để mỗi request tự rotate cùng một refresh token.
- Cập nhật access token, refresh token, user và Axios header cùng nhau; chặn response refresh cũ khôi phục session sau logout hoặc sau khi đổi tài khoản.
- Đồng bộ session giữa các tab và xử lý hai tab cùng refresh. Một promise dùng chung trong một tab không giải quyết được race giữa hai tab.
- Request thất bại vì access token có thể được phát lại có giới hạn sau refresh thành công; loại login/register/verify/reset/refresh khỏi vòng interceptor đó. Không tự retry checkout hoặc mutation sau timeout/5xx khi chưa biết server đã commit hay chưa.
- Session bị thu hồi hoặc refresh bị từ chối xác thực: đóng SSE, xóa auth/cart/private query cache và chuyển về login, giữ đường dẫn quay lại. Lỗi mạng tạm thời khi refresh có thông báo và retry có kiểm soát, không coi mọi lỗi là tài khoản bị khóa.
- `403` là lỗi quyền, không phải lý do tự refresh/logout. History hoặc tài nguyên không thuộc người dùng có thể trả `404`; hiển thị trạng thái không truy cập được mà không suy diễn thêm.
- Chuẩn hóa lỗi `400`, `401`, `403`, `404`, `409`, `429`, lỗi mạng và validation field map. Đọc `Retry-After` khi có; không giả định toàn bộ rate limit cũ đều trả `429`.
- Tránh phụ thuộc vòng `client → authStore → auth API → client` khi thêm interceptor. Đăng ký callback đọc/cập nhật session ở bước bootstrap hoặc tách phần session khỏi việc cấu hình client.

Nghiệm thu: request đồng thời không tạo nhiều refresh trong một tab; credential bị thu hồi không tiếp tục dùng được; logout không bị refresh đang chạy ghi đè; `403` không gây vòng đăng nhập.

### 3.2. Types, phân trang và dữ liệu server

- Bổ sung `SpringPage<T>` và adapter phân trang. API cũ thường trả `data: T[]` với `meta`; các list restock/follow/history/campaign/reservation mới trả `data: Page<T>`, entries ở `data.content`.
- Tách types theo domain cho phần mới; thống nhất response/error types giữa `api/auth.ts` và `types/shared.ts`, có xử lý trường null bị backend bỏ khỏi JSON.
- Bổ sung notification enum, campaign enum, `PickupCredential`, `OrderHistory`, `RestockSubscription`, `OrganizationFollow`, `OrganizerAnalytics`, `Campaign`, `CampaignReservation`.
- Khởi tạo QueryClientProvider. Chuyển dữ liệu server của các feature và tab được sửa sang query/mutation; Zustand giữ auth và trạng thái UI. Không để sessionCache, component state và query cache cùng làm ba nguồn dữ liệu cho một resource.
- Query key bao gồm user và org khi có dữ liệu riêng tư. Đổi org hủy/bỏ qua response cũ; logout/đổi user xóa cache liên quan và state notification.
- Có invalidation cụ thể sau subscribe/follow, checkout/reserve/cancel, stock update, pickup/check-in và campaign closure. Không dùng một event “order changed” cho mọi thông báo mới.
- Tách view model catalog khỏi mock ở luồng mua hàng. Chỉ dùng stock/price/merchId/variant từ backend cho quyết định giao dịch; request lỗi không biến mock thành sản phẩm có thể mua.
- Date-only pickup/filter giữ `YYYY-MM-DD`; deadline/token expiry dùng Instant. Hiển thị mốc campus theo `Asia/Ho_Chi_Minh`, không xử lý tất cả timestamp thiếu offset như UTC.

Nghiệm thu: cả hai dạng phân trang hiển thị đúng tổng/page; private data không đi theo tài khoản cũ; đổi org liên tục không trộn kết quả; quantity không vượt stock mới nhất từ server.

### 3.3. Notification và luồng có sẵn

- Tách panel và logic notification khỏi navbar; hỗ trợ `MERCH_RESTOCKED`, `MERCH_PUBLISHED`, `EVENT_PUBLISHED` và `relatedMerchId`, `relatedOrgId`, `relatedEventId`.
- Customer notification điều hướng tới `/orders/:id`, `/merch/:id`, `/organization/:id`, `/event/:id` theo type/related ID; thiếu ID hiển thị nội dung nhưng không dựng link sai.
- Organizer notification dẫn tới đúng đơn trong scope org. Không dùng order ID làm persisted notification ID để gọi mark-read; payload SSE organizer hiện khác persisted notification DTO, cần adapter/re-fetch phù hợp.
- Loại thông báo quyết định dữ liệu cần refresh. Dedupe persisted notification theo ID; sau reconnect tải lại danh sách/unread count để bù event bị lỡ và không đếm hai lần.
- Giữ hợp đồng SSE hiện có: bearer header hoặc access token query trên hai stream endpoint. **Chưa có endpoint stream-ticket** trong backend đang chạy. Hook cần theo token mới, cleanup timer/connection và không reconnect mãi bằng token đã hết hạn.
- Không tự đánh dấu tất cả khi mở panel; mark-read khi đọc/chọn thông báo, thêm thao tác “Đánh dấu tất cả đã đọc” riêng.
- Sửa enum admin không hợp lệ; giữ transition event đã đúng trong organizer. Đổi text “Đặt trước thành công” của checkout thường thành “Đặt hàng thành công” để phân biệt preorder campaign.

## 4. Tích hợp theo thứ tự feature

### P1 — Restock subscriptions

Vị trí: trang sản phẩm, wishlist và trang cài đặt restock của Customer.

- Thêm `api/restock.ts`, types và query/mutation subscribe/list/unsubscribe.
- Sản phẩm hết hàng: hiện “Báo tôi khi có hàng”; đã đăng ký: “Đang theo dõi tồn kho”, có hủy đăng ký và tùy chọn email.
- Gửi `emailEnabled` rõ ràng theo lựa chọn. Backend mặc định true cho subscription được yêu cầu; không tự đăng ký khi thêm wishlist.
- Guest được dẫn tới login rồi quay lại sản phẩm, không tự subscribe thay người dùng sau đăng nhập.
- Danh sách `/restock-subscriptions` phân trang, đổi email bằng POST lại cùng merchId. Chỉ GET own subscriptions được backend hỗ trợ; không giả định có GET từng subscription. Không kết luận “chưa đăng ký” chỉ từ page đầu tiên.
- Khi có restock event, refresh product/stock/subscription state thích hợp và cho notification dẫn về sản phẩm.

Nghiệm thu: subscribe lặp không trùng; preference không đổi ngoài ý muốn; unsubscribe cập nhật UI; wishlist không tự opt-in; API lỗi không hiển thị subscribe thành công.

### P2 — QR pickup, guest tracking và order history

Vị trí: order detail Customer, guest order page, tab pickup Organizer và order drawer/detail Organizer.

- Thêm `api/pickup.ts`, API history và guest tracking vào lớp API.
- Customer chỉ thấy “Tạo mã nhận hàng” cho order READY. POST cấp credential là thao tác có chủ đích, không chạy tự động mỗi lần render/refetch.
- Render opaque token thành QR ở client; hiển thị lịch nhận hàng và expiry 30 phút. Tạo lại mã phải có thông báo mã cũ mất hiệu lực. Expiry dựa vào `expiresAt`, không dựa vào thời điểm component mount.
- Credential chỉ giữ trong bộ nhớ của màn hình, không lưu localStorage/query cache tồn tại lâu, URL hay log. Reload có thể yêu cầu cấp lại mã.
- Organizer chọn org và lịch nhận, quét camera hoặc nhập token thủ công, gọi verify rồi hiện order preview trước nút “Xác nhận đã nhận hàng”. Verify không làm hoàn tất đơn.
- Check-in gửi token và assigned schedule UUID; dùng null khi order không có schedule. Quét hợp lệ không thay thế quyền sở hữu org hoặc kiểm tra ngày nhận của server.
- Khóa thao tác check-in khi đang gửi; cùng QR xuất hiện nhiều frame không tạo nhiều request đồng thời. Thành công refresh order, schedule list, history, analytics và xóa preview token.
- Xử lý quyền camera bị từ chối, môi trường không hỗ trợ camera, token hết hạn/bị cấp lại/đã dùng, sai org/lịch và đơn đã đổi trạng thái. Có đường nhập token và manual check-in hiện có.
- Guest checkout giữ order IDs từ response và cho đi tới `/guest-orders?orderId=…`. Tracking nhập email qua form; dùng API tracking hiện có, không đưa email/receipt/token vào route.
- Guest READY có email: request receipt với phản hồi chung `202`, nhập credential nhận qua email rồi exchange một lần sang QR. Không hiển thị thông báo “đơn tồn tại” chỉ vì receipt endpoint trả `202`.
- Email guest hiện là optional. Nếu không cung cấp email, giải thích không có luồng QR qua email và giữ phương án nhận hàng thủ công; không đổi âm thầm toàn bộ checkout thành bắt buộc email.
- Timeline hiển thị `fromStatus`, `toStatus`, `source`, `actorId`, `pickupScheduleId`, `createdAt`. READY → READY có thể là lần cấp lại QR, không phải thêm bước giao hàng. Dữ liệu cũ không có history thì hiện trạng thái trống; không bịa audit từ progress bar.

Nghiệm thu: mã hết hạn/cấp lại không còn được dùng; verify không đổi order; check-in chỉ hoàn tất một lần; lịch tương lai bị từ chối; guest receipt dùng một lần; manual completion làm QR cũ không còn hợp lệ.

### P3 — Organization following

Vị trí: organization detail và trang `/following` của Customer.

- Thêm `api/following.ts` và follow/preferences/list/unfollow query/mutations.
- Nút follow có trạng thái loading/đã theo dõi. Preferences: báo merch, báo event, nhận email; mặc định true/true/false theo backend.
- PATCH chỉ gửi trường người dùng đổi; không reset preference bị bỏ qua. POST lại là idempotent theo quan hệ user/org.
- `/following` hiển thị danh sách và các lựa chọn của người dùng, xử lý phân trang và org không còn ACTIVE.
- Đọc follow state chính xác, không suy ra từ page đầu danh sách; chưa có endpoint GET follow riêng theo org.
- Notification publication chỉ phát ở lần đầu; UI không hứa thông báo mỗi lần tổ chức chỉnh sửa hoặc tái công bố.

Nghiệm thu: follow/unfollow/preference cập nhật đúng; guest/role khác không gọi API CUSTOMER; đổi user không giữ follow state cũ; email không tự bật.

### P4 — Organizer analytics

Vị trí: tab “Thống kê” trong `/organizer`, dùng org selector hiện tại.

- Thêm `api/analytics.ts`, types và query key theo org/from/to. Date filter 7/30 ngày hoặc tùy chọn, inclusive, tối đa 366 ngày; lấy ngày mặc định theo campus.
- KPI: tổng đơn, từng status, completed quantity/value, non-cancelled value, PAID value và cancellation rate.
- Ghi nhãn riêng “Giá trị đơn hoàn tất” và “Giá trị đã thanh toán”; không gọi mọi đơn COMPLETED là doanh thu tiền đã thu. Fraction cancellation nhân 100 khi trình bày phần trăm.
- Daily orders là chart theo ngày tạo và trạng thái hiện tại. Có thể thêm zero cho ngày không có dữ liệu khi dựng chart, không sửa ý nghĩa response.
- Top products tối đa 20 theo completed quantity; stock là inventory hiện tại, không phải snapshot ở cuối date range.
- Pickup workload lọc theo ngày lịch nhận, có thể gồm đơn tạo ngoài cửa sổ thống kê đơn.
- Có empty/error/loading state, refresh và bảng dữ liệu tương đương chart. Đổi org xóa dữ liệu preview cũ ngay, không để chart của org A xuất hiện dưới tên org B.

Nghiệm thu: totals/chart/table cùng scope; không double-count số lượng order items; 0 đơn không gây NaN; from > to và quá 366 ngày được chặn/hiển thị lỗi; permission denied không lộ report cũ.

### P5 — Preorder campaigns

Vị trí: `/campaigns`, `/campaigns/:id`, Customer reservations và tab “Chiến dịch” của Organizer.

- Thêm `api/campaigns.ts` và types phân biệt campaign state với order state.
- Organizer form: title ≤255, description ≤4000, minimum 1–100.000 không vượt stock tổng, deadline tương lai ≤90 ngày, 1–20 SKU PUBLISHED thuộc org; mỗi label ≤128 ký tự. Chọn SKU thật làm variant, không tạo size/color giả từ mock.
- List dùng summary; `variants` của list hiện rỗng. Trang detail gọi detail API để lấy label, snapshot price và available quantity.
- Public list backend không có filter org/state theo contract hiện tại và có thể gồm campaign đã đóng của org ACTIVE. UI phải hiển thị state thật, không coi mọi item là đang mở; không dùng lọc một page rồi báo đó là tổng kết quả toàn server.
- Customer chọn variant, quantity 1–100 theo available stock, note ≤1000. Giá hiển thị dựa vào campaign variant snapshot.
- Sinh một `requestId` UUID cho mỗi intent đặt mới; giữ cùng ID và payload khi retry. Disable submit đang chạy. Timeout có trạng thái “chưa xác định kết quả”; không tự tạo request ID mới để gửi lại cùng intent.
- Giữ thông tin operation theo user đủ để xử lý retry/reload có kiểm soát; xóa khi logout/đổi user. Payload đã gửi cần được giải quyết trước khi cho chỉnh thành một intent mới.
- Reservation response chứa `reservation` + `order`; giữ quan hệ campaignId/orderId. Dùng list reservations của chính Customer để dựng màn hình và liên kết về order.
- Campaign ACTIVE: giải thích đợi đủ minimum đến deadline, organizer chưa fulfill. SUCCEEDED: tiếp tục quy trình order; FAILED/CANCELLED: các pending reservation orders được server hủy và trả stock.
- Qua deadline nhưng server còn ACTIVE: hiển thị “Đang chốt chiến dịch”, khóa đặt mới và refetch có giới hạn; không tự gán FAILED/SUCCEEDED bằng đồng hồ client. Server poll mặc định 60 giây.
- Refresh khi quay lại tab/trang và sau reserve/cancel. Không giả định backend phát notification riêng cho mọi campaign transition.
- Trên catalog/product/cart, trạng thái tham gia ACTIVE campaign phải được lấy từ nguồn đã xác minh trước khi đổi CTA “Mua ngay” thành “Tham gia chiến dịch”. Khoảng trống API ở mục 8 quyết định mức tích hợp có thể phát hành.
- Organizer order phải có nguồn liên kết campaign đáng tin cậy để vô hiệu confirm/schedule trước SUCCEEDED. Không suy ra chỉ từ merchId vì đơn thường hoặc campaign cũ cũng có thể chứa SKU đó.
- Không có guest campaign reservation, payment gateway hoặc auto-refund; không dựng UI thanh toán online cho feature này.

Nghiệm thu: cùng request ID/payload trả cùng order, không nhân đôi stock deduction; payload khác cùng ID báo conflict; sold-out/closed campaign không đặt mới; order đi đúng trạng thái; deadline không bị lệch timezone; ordinary checkout không được dùng để vượt campaign policy.

## 5. Routes và cấu trúc đề xuất

| Route / điểm tích hợp | Vai trò | Nội dung |
| --- | --- | --- |
| `/merch/:id`, `/wishlist` | Public/CUSTOMER | Restock CTA và campaign context khi có nguồn liên kết |
| `/restock-subscriptions` | CUSTOMER | Danh sách đăng ký và email preference |
| `/organization/:id` | Public/CUSTOMER | Follow/preferences và campaign navigation |
| `/following` | CUSTOMER | Tổ chức đang theo dõi |
| `/orders/:id` | CUSTOMER | QR pickup, timeline và campaign context của đơn nếu biết |
| `/guest-orders?orderId=…` | Guest | Tracking theo email, receipt exchange và QR |
| `/campaigns`, `/campaigns/:id` | Public | Discovery/detail; đặt variant chỉ CUSTOMER |
| `/reservations` | CUSTOMER | Reservation → campaign/order |
| `/organizer?orgId=…&tab=pickup` | ORGANIZER | Lịch nhận, verify/check-in, order history |
| `/organizer?orgId=…&tab=analytics` | ORGANIZER | Thống kê theo org/ngày |
| `/organizer?orgId=…&tab=campaigns` | ORGANIZER | Tạo/list/cancel campaign |

Các route mới đã tồn tại trong `main.tsx`. Scanner dùng tab `scanner`; tab `pickup` vẫn quản lý lịch nhận. Giữ đường dẫn `/events` và `/event/:id` thực tế; không dựng notification link nhầm `/events/:id`.

Cấu trúc dự kiến lúc lập kế hoạch (bản triển khai gom các component theo domain trong `src/features/*.tsx`, types trong `types/features.ts`):

```text
frontend/src/
  api/{restock,pickup,following,analytics,campaigns}.ts
  types/{notification,restock,pickup,following,analytics,campaign}.ts
  hooks/                         # auth coordination, feature queries/mutations, SSE
  features/notifications/        # panel, target resolver, payload adapters
  features/organizer/             # tabs và shared order detail/selector
  features/pickup/                # QR display, scanner, verify preview
  pages/                         # các route mới ở bảng trên
```

Tách từng tab khi làm tính năng liên quan; không vừa viết feature vừa thay toàn bộ dashboard trong một commit. Thiết kế chung tiếp tục dùng components/styles hiện tại.

## 6. Các commit dự kiến và thứ tự

| Thứ tự | Commit dự kiến | Kết quả có thể review |
| --- | --- | --- |
| 1 | `fix(frontend): coordinate refresh rotation and revoked sessions` | Auth API/client/store, kiểm thử concurrent refresh/logout và hai tab |
| 2 | `refactor(frontend): normalize pagination and scope server caches` | Types/adapters/provider, private cache scope, enum compatibility |
| 3 | `refactor(frontend): extract notifications and handle resource targets` | Panel/SSE lifecycle, notification routing và reconciliation |
| 4 | `feat(frontend): add restock subscriptions and preferences` | Product/wishlist/settings với API thật |
| 5 | `feat(frontend): add pickup QR and order history` | Customer QR, Organizer verify/check-in, history |
| 6 | `feat(frontend): add guest tracking and pickup receipt exchange` | Giữ response checkout, guest route, emailed credential flow |
| 7 | `feat(frontend): add organization following and alert settings` | Follow UI/settings/list |
| 8 | `feat(frontend): add organization analytics dashboard` | Org-scoped filters, metrics, chart/table |
| 9 | `feat(frontend): add campaign discovery and organizer management` | List/detail và create/cancel campaign |
| 10 | `feat(frontend): add idempotent campaign reservations` | Reservation/order link, retry handling, mua theo campaign; chỉ bật tích hợp mở rộng sau khi giải quyết mục 8 |
| 11 | `test(frontend): cover backend integration and session boundaries` | E2E tổng hợp, README cấu hình, ma trận nghiệm thu |

Kiểm thử tập trung phải đi cùng mỗi commit, không chờ commit 11 mới viết test. Số commit có thể tách thêm nếu một nhóm quá lớn. Bảng này ghi thứ tự dự kiến; lịch sử Git của `refactor/fe` ghi nhóm công việc thực tế.

## 7. Kiểm thử và điều kiện hoàn thành

Trước triển khai: ghi nhận baseline TypeScript/build và thiết lập unit/component/E2E runner phù hợp Vite/React. Fixture phải mô phỏng đúng API envelope, hai kiểu phân trang, status codes và token expiry.

Baseline trong lượt lập plan: TypeScript kiểm tra thành công bằng compiler local với `--project frontend/tsconfig.json --noEmit --incremental false`. Chưa chạy production build hoặc E2E trong lượt này. Thay đổi hiện tại chỉ là tài liệu kế hoạch.

| Nhóm | Kịch bản bắt buộc |
| --- | --- |
| Auth | Nhiều request 401 cùng lúc, hai tab refresh, logout khi refresh đang chạy, password-reset/account revocation, 403 không refresh, lỗi mạng không retry vô hạn |
| Notification/cache | Dedupe SSE/list, reconnect bù event, account switch, đúng link theo type, logout đóng stream, response org cũ không ghi vào org mới |
| Restock/follow | Danh sách nhiều page, explicit consent/email default, double click, unsubscribe/unfollow, lỗi API và rollback UI |
| Pickup | READY gating, expiry/reissue, verify không consume, check-in trùng, sai org/schedule, camera denied, guest receipt dùng một lần |
| Analytics | Window 1/366/>366 ngày, không có đơn, inventory hiện tại, completed khác paid, đổi org khi đang tải |
| Campaign | Deadline/campus timezone, snapshot price, variant hết stock, timeout rồi retry cùng request ID, payload conflict, qua deadline chưa chốt, SUCCEEDED/FAILED/CANCELLED và regular-checkout rejection |
| Luồng cũ | Login/OTP/reset, cart checkout, guest checkout, wishlist, customer cancel, organizer status/manual pickup, event publication và admin approval |

- Unit kiểm tra adapter, notification target, error normalization, expiry và request intent; component kiểm tra form/loading/error/role states; E2E kiểm tra customer → organizer và guest nhận hàng.
- Dùng backend test instance và dữ liệu riêng cho tích hợp; không tạo/hủy đơn thử trên database đang dùng chung.
- Guest QR cần email transport/test fixture có thể nhận receipt. Endpoint dev OTP chỉ trả OTP auth, không trả guest pickup receipt. Không thêm raw credential vào log để tiện test.
- Verify màn hình nhỏ 375px và desktop, keyboard/focus, accessible error summary/inline errors, modal focus và bảng thay thế chart. Camera cần môi trường browser phù hợp; manual token input vẫn sử dụng được.
- `npm run build` phải pass; bộ kiểm thử phù hợp nhóm thay đổi phải pass; không có private cache/token leak hoặc mock được dùng như dữ liệu checkout thật.
- Trước sửa mỗi symbol hiện có phải chạy GitNexus impact upstream, báo direct callers/affected flows/risk và cảnh báo HIGH/CRITICAL. Trước từng commit chạy detect-changes để đối chiếu scope. Phân tích trong plan này không thay thế impact tại thời điểm chỉnh code.

## 8. Khoảng trống hợp đồng ghi nhận lúc lập kế hoạch

| Khoảng trống đã xác minh | Phương án frontend dùng API hiện tại | Bổ sung backend nên bàn trước phần tích hợp mở rộng |
| --- | --- | --- |
| `MerchResponse` không có activeCampaignId/purchase mode; public campaign list không có variants | Discovery/detail campaign dùng được. Nếu ordinary checkout bị từ chối, hiển thị hướng sang danh sách campaign. Không coi mapping thiếu là “không có campaign”; không fan-out mọi detail cho mỗi product card | Expose purchase context theo SKU hoặc lookup/batch API xác định ACTIVE campaign, có ID/detail link |
| `OrderResponse` không có campaignId/state hay allowed actions | Customer có thể nối own reservation list với orderId. Organizer vẫn nhận rejection từ server và hiển thị thông báo; không tự khóa đơn chỉ vì trùng SKU | Thêm campaign/reservation context và allowed fulfillment actions vào order response hoặc organization-scoped lookup đáng tin cậy |
| Không có API detail campaign riêng cho Organizer; public detail yêu cầu org ACTIVE | Organizer list/create/cancel dùng được với summary. Không phụ thuộc public detail để xem org đã bị INACTIVE | Nếu cần màn quản trị detail/variants cho org INACTIVE, bổ sung endpoint own campaign detail |
| List restock/follow chỉ phân trang, không có GET state theo từng product/org | Tải own list đủ page theo nhu cầu và cache user-scoped, hoặc để UI ở trạng thái đang xác minh | Detail/batch subscription/follow state API nếu số lượng lớn khiến lookup tốn request |
| Chưa có stream-ticket, campaign-specific SSE hoặc payment acknowledgment API mới | Dùng SSE access-token contract thật, refetch campaign theo deadline/focus và hiển thị paymentStatus nguyên nghĩa | Chỉ thêm ticket/pub-sub/payment UI khi có backend contract riêng và được đưa vào scope |

P0–P4 và campaign discovery/detail/reservation page có thể tiến hành bằng API hiện có. **Đổi CTA mua trên toàn catalog và chặn trước các action của organizer một cách chính xác cần giải quyết purchase/order context trước khi phát hành đầy đủ P5.** Không thêm field giả ở frontend để che khoảng trống backend.

## 9. Thứ tự triển khai

Thực hiện P0 và notification foundation trước; sau đó lần lượt restock → QR/history/guest → following → analytics → campaigns. Mỗi nhóm hoàn tất API/type/UI/kiểm thử rồi mới chuyển nhóm tiếp theo. Chốt hợp đồng purchase/order context trước phần catalog/organizer của campaigns; phần còn lại không phụ thuộc quyết định này.

## 10. Kết quả triển khai

- P0 và P1–P5 đã tích hợp: refresh/cross-tab isolation, notification, restock, QR/history/guest tracking, following, analytics và preorder.
- Ba khoảng trống campaign được đóng bằng purchase context, customer/organizer order context và owner campaign detail. `fulfillmentAllowed` chỉ biểu thị điều kiện campaign; backend vẫn kiểm tra quyền và transition khi thực thi action.
- Các nút follow/restock đọc đủ own pages; chưa thêm batch lookup. SSE giữ hợp đồng token hiện có; không thêm stream ticket hoặc payment gateway.
- Analytics dùng bảng dữ liệu; chưa thêm chart. Dashboard tách các tab mới thành component; chưa refactor toàn bộ tab cũ.
- Test, giới hạn fixture và tình trạng Vercel/Render được ghi riêng trong [validation](2026-10-02-frontend-validation.md).
