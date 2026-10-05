# Supabase live read-only audit

Project ref: `aubfixpblwlpsfgmwmza`; kiểm tra qua MCP sau reconnect ngày 2026-10-05. Connector list_projects còn chỉ thấy Foodya/Travelako, nhưng execute_sql và security advisors truy cập được đúng ref đang cấu hình trong UITMerch. Không đọc dữ liệu cá nhân, secret hay thay đổi production.

- 30 bảng public hiện không cấp SELECT/INSERT/UPDATE/DELETE cho anon/authenticated. RLS chưa bật trên các bảng này; quyền bị revoke bảo vệ đường trực tiếp hiện tại, nhưng cần defense in depth và kiểm tra grants/function/default privilege cho migration mới.
- Storage objects có RLS, nhưng hai policy INSERT `Anon upload avatars` và `Anon upload org logo/cover` cho phép anon upload vào toàn bộ bucket avatars/org-assets, không kiểm tra owner, type hoặc quota backend. Đây là đường bypass upload hardening đã làm local. Cần gỡ hai policy đồng bộ với backend/frontend upload cutover.
- Bốn bucket merch-images/org-logos/avatars/org-assets public, không file_size_limit hoặc allowed_mime_types. Public media read là chủ đích hiện có; cần rà soát nội dung đã upload và cấu hình giới hạn bucket riêng, không tự chuyển private gây hỏng URL ảnh.
- Flyway live mới tới V43; V44–V47 chưa được triển khai. Local hardening chưa bảo vệ app đang chạy.
- Security advisor báo vector extension trong public. Không tự di chuyển khi chưa đánh giá tác động truy vấn vector/search_path. Remediation: https://supabase.com/docs/guides/database/database-linter?lint=0014_extension_in_public

SQL gỡ hai policy nằm trong `storage-upload-cutover.sql`, chưa chạy. Cần deploy API upload và frontend chuyển qua backend trước hoặc chấp nhận gián đoạn upload trong thời gian khóa policy. Đọc public không bị thay đổi. Sau cutover kiểm chứng anon upload bị từ chối và backend owner upload vẫn thành công.

## Sau rollout 2026-10-06

V48 đã chạy; 34 bảng public bật RLS. Hai policy anonymous INSERT đã gỡ và cả hai bucket từ chối upload anon thực. Bucket giới hạn 10 MiB và MIME ảnh; backend upload hợp lệ đã pass. Anonymous Data API trả 401 cho các bảng nhạy cảm được thử. Advisor hiện có INFO RLS enabled/no policy (deny browser roles theo kiến trúc backend) và WARN vector/pg_trgm trong public. Xem [deployment evidence](DEPLOYMENT-2026-10-06.md).
