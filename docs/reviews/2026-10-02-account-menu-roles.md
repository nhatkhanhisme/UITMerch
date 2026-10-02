# Menu tài khoản theo vai trò — 02/10/2026

Nhánh: `refactor/fe`, tiếp nối commit bố trí đặt trước `23cb468`.

“Tra cứu đơn khách” trước đây xuất hiện với mọi người trong cả menu desktop và mobile. Theo yêu cầu điều chỉnh giao diện, shortcut này giờ chỉ xuất hiện khi đăng nhập bằng tài khoản ORGANIZER.

| Người dùng | Tra cứu đơn khách trong menu | Mục tài khoản liên quan |
|---|---|---|
| Chưa đăng nhập | Ẩn | Đăng nhập / Đăng ký |
| CUSTOMER | Ẩn | Đơn hàng của tôi |
| ORGANIZER | Hiện, dẫn tới `/guest-orders` | Quản lý BTC |
| ADMIN | Ẩn | Quản trị |

Backend hiện quy định `hasRole('ORGANIZER')` cho các thao tác quản lý đơn của tổ chức tại `OrganizerOrderController`; ADMIN không có quyền mặc định sử dụng các endpoint đó. Shortcut trong menu là trang tra cứu bằng mã đơn/email, không phải màn hình quản lý đơn tổ chức.

Link được đặt trong danh sách tài khoản của ORGANIZER, dùng chung renderer cho desktop và mobile. Đã bỏ hai link luôn hiển thị trước đó. Menu guest chỉ có hành động đăng nhập/đăng ký và không còn đường phân cách dư.

Thay đổi chỉ áp dụng cho mục trong menu. Route public `/guest-orders`, link sau guest checkout và link nhận hàng gửi qua email giữ nguyên luồng tra cứu/xác minh hiện có.

## Kiểm tra và phạm vi tác động

- Production build TypeScript/Vite đạt; cảnh báo bundle lớn đã có trước đó vẫn còn.
- Browser suite cuối **45/45 đạt**, gồm 17 bài tính năng, 20 bài navbar và 8 bài khám phá đặt trước.
- Browser tests kiểm tra đủ guest/CUSTOMER/ORGANIZER/ADMIN tại 1440px và 375px; chỉ ORGANIZER có shortcut, click đi đúng route và đóng menu. CUSTOMER vẫn có mục Đơn hàng của tôi.
- Test responsive navbar đã cập nhật để xác nhận CUSTOMER không có shortcut cả trong menu mobile.
- GitNexus index đã cập nhật trước impact analysis. `TopNavBar` mức HIGH: gọi trực tiếp từ `HomeFixedChrome`, ảnh hưởng `HomePage`/`AppRouter` qua 3 module; đã báo trước sửa. `accountLinks` không có symbol trong index, đã đối chiếu trực tiếp với renderer và hai menu.
- Chỉ thay đổi frontend, tests và tài liệu. Browser suite dùng API fixtures, không gửi đơn hoặc email production.
