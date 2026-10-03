# Làm rõ giao diện mua hàng và quản trị — 03/10/2026

Đã triển khai trên nhánh `refactor/fe`. Backend Docker tại `http://localhost:8080` đã build lại, khởi động lại và healthy. Frontend tại `http://localhost:5173` đã kiểm tra với dữ liệu demo trên database đang cấu hình.

## Thay đổi

| Khu vực | Kết quả |
| --- | --- |
| Đơn hàng | Card nền trắng, chữ và số tiền rõ hơn, phân tách sản phẩm/tổng tiền, nút chi tiết dễ nhận ra, bộ lọc có trạng thái chọn rõ. Lịch nhận và lý do hủy có vùng riêng. |
| Thông báo | Đánh dấu tất cả cập nhật trạng thái đã đọc và số chưa đọc trong cache. Có thông báo thành công trong panel và toast; nút đổi thành “Đã đọc tất cả” và bị vô hiệu hóa khi không còn thông báo chưa đọc. Lỗi giữ nguyên trạng thái để thử lại. |
| Navbar | Mục hiện tại nền tối, chữ trắng; trang chi tiết sự kiện thuộc Sự kiện, bộ sưu tập thuộc Vật phẩm. Trang tài khoản/đơn hàng/quản trị có viền và nền riêng ở nút tài khoản. |
| Đặt trước | Tiêu đề “Bộ sưu tập mở đặt trước”, nút “Khám phá bộ sưu tập”, có thanh tiến độ. Giữ vị trí giới thiệu trên homepage/catalog, ngoài navbar. |
| Sự kiện | Banner giữ tỷ lệ, tiêu đề vừa phải, thời gian và tổ chức thành các mục rõ, nội dung dễ đọc, vật phẩm liên quan ở panel riêng. Nhãn sắp diễn ra/đang diễn ra/đã kết thúc dựa vào thời gian, không coi mọi sự kiện PUBLISHED là đang diễn ra. |
| Theo dõi | Danh sách hiển thị tên/logo tổ chức, tùy chọn nhận cập nhật được nhóm và giải thích. Số người theo dõi xuất hiện ở card và trang chi tiết tổ chức. |
| Có hàng trở lại | Đổi thành “Nhắc khi có hàng”; danh sách hiển thị tên vật phẩm, tên tổ chức và khả năng mua hiện tại. |
| Organizer | Dashboard có nền ổn định, card/field/bảng rõ hơn, nút xóa ảnh luôn nhìn thấy. Hồ sơ tiếng Việt, bố cục đồng bộ customer, chọn đúng tổ chức trước khi sửa. Có thể xóa mô tả/logo/ảnh bìa bằng giá trị rỗng. |
| Admin | Phân trang theo API, lọc vai trò/trạng thái, tìm trong trang hiện tại, bảng rõ hơn. Phân biệt hoạt động với xác minh email. Đổi vai trò/kích hoạt/vô hiệu hóa có hộp xác nhận. Tổ chức đang hoạt động chỉ có thao tác tạm ngừng; tổ chức chờ duyệt có duyệt/từ chối. Đơn dùng trạng thái READY/COMPLETED đúng với backend. |

## Hai nguyên nhân lỗi đã xác nhận

**Giá 0đ:** DTO và PostgreSQL cho phép giá bằng 0. Không có constraint bắt buộc giá lớn hơn 0 trên database đang cấu hình. Nút giảm giá cũ đổi 0 thành chuỗi rỗng; payload bỏ trường giá, dẫn tới lỗi validation lúc tạo và không cập nhật giá lúc sửa. Form hiện giữ `"0"`, gửi số `0` cho cả tạo/sửa, bắt buộc giá không âm, thêm lựa chọn “Miễn phí (0đ)”. Test PostgreSQL xác nhận tạo/sửa giá 0 thành công và giá âm bị từ chối.

**Admin hiển thị mọi tài khoản vô hiệu/chưa xác minh:** Java response trả `active` và `verified`, trong khi frontend đọc `isActive` và `isVerified`. API adapter hiện chuẩn hóa cả hai dạng, giữ đúng giá trị `false`. Kích hoạt tài khoản không đồng nghĩa với xác minh email.

## Dữ liệu mẫu và cách thử

Đã nạp 4 tài khoản riêng, 3 tổ chức, 6 vật phẩm, 2 sự kiện, 3 đợt đặt trước, 7 đơn, 2 lượt theo dõi, 1 đăng ký nhắc có hàng và 3 thông báo. Dữ liệu mang nhãn Demo và ID riêng. Không gửi email demo, không upload Storage; ảnh mẫu dùng URL placeholder.

| Email | Vai trò | Màn hình nên thử |
| --- | --- | --- |
| `demo.customer@uitmerch.test` | CUSTOMER, hoạt động/đã xác minh | `/orders`, `/following`, `/restock-subscriptions`, `/campaigns`; mở đơn sẵn sàng nhận để tạo QR; đánh dấu thông báo đã đọc. |
| `demo.organizer@uitmerch.test` | ORGANIZER, hoạt động/đã xác minh | `/profile/organizer`, `/organizer`; sửa giá thành 0, đổi tổ chức, quản lý đợt đặt trước/lịch nhận. |
| `demo.admin@uitmerch.test` | ADMIN, hoạt động/đã xác minh | `/admin`; đối chiếu trạng thái, thử xác nhận/hủy thao tác, duyệt tổ chức demo, lọc đơn. |
| `demo.inactive@uitmerch.test` | CUSTOMER, vô hiệu/chưa xác minh | Dùng trong danh sách admin để đối chiếu trạng thái; không dùng để đăng nhập. |

Mật khẩu ngẫu nhiên nằm trong **`backend/.demo-credentials.json`**, file riêng quyền `0600`, được `.gitignore` bảo vệ. Ba tài khoản đăng nhập dùng cùng mật khẩu trong file này. Không đưa mật khẩu vào tài liệu hoặc commit.

Từ root dự án, để nạp vào database trong `backend/.env`:

```sh
python3 backend/scripts/seed-feature-demo.py --database configured
```

Hoặc dùng kết nối `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, `PGSSLMODE` của database thử nghiệm:

```sh
python3 backend/scripts/seed-feature-demo.py --database pg-env --credentials /tmp/uitmerch-demo-credentials.json
```

Runner dùng `psql` và `pgcrypto` đã có trong database. Dữ liệu mẫu ngoài Flyway, trong một transaction. Chạy lại chỉ thêm dòng thiếu, giữ nguyên chỉnh sửa/trạng thái đã đọc và không nhân đôi bản ghi. Nếu tài khoản demo đã tồn tại nhưng mất file mật khẩu, runner dừng để tránh âm thầm đổi mật khẩu. Hạn các đợt ACTIVE được tính từ lần nạp đầu; chạy lại không kéo dài hạn.

Đợt Campus Stories có hai phiên bản M/L và 2/10 áo đã giữ chỗ; Canvas Days đang nhận đặt trước; UIT Notes đã đạt mục tiêu và có đơn để organizer xác nhận. Có sticker miễn phí, túi hết hàng, sự kiện tương lai và sự kiện đã kết thúc. Các đơn thường gồm đủ PENDING, CONFIRMED, READY, COMPLETED và CANCELLED.

## Kiểm tra

- Backend: **269 test pass**, 0 failure/error/skip, PostgreSQL 17 riêng. Bốn test mới kiểm tra dữ liệu theo dõi theo tài khoản, follower count, tên/trạng thái nhắc có hàng, giá 0đ và response admin.
- Frontend: **42 unit test pass**, production build pass. Full browser suite 69 test pass; management suite mở rộng 18 test pass, tổng bộ test hiện có 73 case. Có kiểm tra 1440/375/320px, lỗi đánh dấu đã đọc, sửa đúng tổ chức được chọn và bảng admin trên mobile.
- Docker: build image và recreate container thành công; healthcheck healthy. Gemini tiếp tục để trống theo yêu cầu.
- API thật: 15 endpoint thành công, đăng nhập đủ CUSTOMER/ORGANIZER/ADMIN; tên theo dõi/nhắc có hàng, follower count và trạng thái các đơn demo được đối chiếu.
- Frontend thật: 9 màn hình tải được qua Vite và backend Docker, không lỗi API/JavaScript, không tràn ngang ở viewport kiểm tra.

Ảnh desktop dùng dữ liệu demo thật; ảnh admin/mobile/thông báo dùng fixture tổng hợp. Chi tiết kiểm tra tại [validation JSON](2026-10-03-app-management-validation.json). Ảnh trong [thư mục artifacts](artifacts/2026-10-03-management).

Build còn cảnh báo bundle ShaderBackground lớn hơn 500 kB của phần Three.js hiện có. Các màn hình vừa sửa dùng nền CSS ổn định, nhưng những trang khác vẫn có bundle này. Kiểm tra chạy local; chưa triển khai bản thay đổi này lên Vercel/Render.

GitNexus impact đã chạy trước các thay đổi; tổng phạm vi customer/organizer/admin được cảnh báo CRITICAL. Rà soát các luồng bị ảnh hưởng và chạy detect_changes theo từng nhóm commit. Phân tích graph không thay thế test PostgreSQL hoặc kiểm tra API thật.
