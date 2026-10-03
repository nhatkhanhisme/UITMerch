# Làm rõ đặt trước, thông báo và số người theo dõi

Bộ sưu tập dùng card nền trắng, viền nhẹ và điểm nhấn vàng/cyan theo concept UITMerch. Tiêu đề, trạng thái, mục tiêu tối thiểu và hạn đặt có vùng riêng; thanh tiến độ thể hiện số sản phẩm đã giữ chỗ. Nút “Khám phá bộ sưu tập” dễ nhận ra trên desktop và điện thoại.

Trang chi tiết chỉ hiển thị tiêu đề một lần. Tóm tắt mục tiêu và hạn đặt nằm trước phần giới thiệu và form giữ chỗ. Hướng dẫn ba bước giải thích chọn phiên bản, cùng đạt mục tiêu và nhận cập nhật lịch nhận. Form có giá trị dự kiến theo phiên bản/số lượng và ghi rõ chưa thu tiền ở bước giữ chỗ. Đợt đã đóng hiển thị trạng thái và đường dẫn xem đơn; yêu cầu chưa rõ kết quả vẫn có nút thử lại với request ID cũ.

Thông báo có nhãn “Chưa đọc”/“Đã đọc”, màu nền và dấu nhận biết riêng. Nút dấu tick đánh dấu một thông báo mà giữ nguyên trang; mở nội dung sẽ đánh dấu và đi đến chi tiết liên quan. “Đọc tất cả” chuyển thành “Đã đọc tất cả” khi hoàn tất. Panel xác nhận thành công, cập nhật bộ đếm và giữ nguyên trạng thái khi API lỗi. Các nút đánh dấu riêng có vùng bấm 44px, hỗ trợ bàn phím; danh sách cuộn trong panel trên màn hình nhỏ.

Số người theo dõi nằm ngay cạnh thao tác theo dõi trên trang tổ chức, luôn hiện cả khi chưa theo dõi hoặc chưa đăng nhập, kể cả 0. Theo dõi/bỏ theo dõi cập nhật số lượng. Dữ liệu lấy từ `followerCount` của API công khai.

## Kiểm tra

- `npm test`: **42/42 unit test pass**.
- `npm run test:e2e -- --workers=2`: **90/90 browser case pass** trong lượt chạy cuối, gồm 17 case mới.
- `npm run build`: pass; còn cảnh báo bundle Three.js/ShaderBackground lớn hơn 500 kB đã có trước thay đổi này.
- Docker `uitmerch-backend`: healthy. API công khai campaigns/organization trả HTTP 200.
- Vite + API thật: ba card đặt trước và ba vùng mục tiêu tải được; guest thấy **1 người theo dõi** tại tổ chức Art Studio demo, cùng đường dẫn đăng nhập để theo dõi.

Bản cập nhật là frontend; không chạy lại backend test suite trong lượt này. Browser suite dùng API fixture; smoke test trên Vite dùng API thật và chỉ đọc dữ liệu. Kiểm tra thực hiện local, chưa deploy bản này lên Vercel/Render. Chi tiết: [validation JSON](preorder-ui-polish-validation.json).

Các ca browser mới kiểm tra card/giữ chỗ/thông báo ở 1440, 375 và 320px; follower 0/17 cho guest/customer chưa theo dõi; cập nhật số khi theo dõi/bỏ theo dõi; thông tin đợt đã đóng; đánh dấu riêng thành công/thất bại. Ca kiểm tra cuộn thông báo cũ chọn đúng nút mở nội dung sau khi thêm nút đánh dấu riêng.

GitNexus impact trước khi sửa các component là LOW. Detect changes đánh dấu phạm vi tổng HIGH (27 symbol, 12 luồng), do chuông thông báo dùng chung trên navbar và tính cả fixture test. Đã rà phạm vi sáu file frontend và chạy toàn bộ browser suite, gồm kiểm tra phối hợp refresh token giữa hai tab và thử lại giữ chỗ. Graph có giới hạn số execution flow, nên kết quả được đối chiếu bằng code và browser test.

## Ảnh kiểm tra

Ảnh card/giữ chỗ/thông báo dùng API fixture tổng hợp. Ảnh tổ chức dùng dữ liệu demo công khai qua Vite và backend Docker thật.

- [Bộ sưu tập — desktop](artifacts/preorder-ui-polish/collections-polished-1440.png)
- [Bộ sưu tập — điện thoại](artifacts/preorder-ui-polish/collections-polished-375.png)
- [Giữ chỗ — desktop](artifacts/preorder-ui-polish/reservation-polished-1440.png)
- [Giữ chỗ — điện thoại](artifacts/preorder-ui-polish/reservation-polished-375.png)
- [Thông báo — desktop](artifacts/preorder-ui-polish/notifications-polished-1440.png)
- [Thông báo — điện thoại 320px](artifacts/preorder-ui-polish/notifications-polished-320.png)
- [Follower công khai — guest, demo thật](artifacts/preorder-ui-polish/public-followers-live-demo.png)
