**Follow-up:** configuration, migration and order-mapping fixes are recorded in [the fix review](2026-10-02-backend-environment-fixes.md). The findings below describe the pre-fix state.

# Backend environment/profile audit — 02/10/2026

Nhánh `refactor/fe`, HEAD `f160346`. Kiểm tra chỉ đọc; không sửa schema, lịch sử Flyway, gửi email hoặc upload/delete object trên dịch vụ thật. Giá trị secret không nằm trong báo cáo.

## Biến và credentials

- Compose đọc đủ **17 biến** từ `backend/.env`: 15 có giá trị, `GEMINI_API_KEY`/`GEMINI_API_KEYS` trống. AI Gemini chưa được cấu hình.
- Profile mặc định, `docker` và `prod`: cả 17 thuộc tính Spring resolve đúng giá trị trong `.env`. Mô phỏng OS environment từ Compose cũng đạt 17/17, không cần file `.env` tại working directory.
- Database thật: kết nối và `SELECT 1` trong chế độ read-only **đạt**. SMTP: kết nối/xác thực không gửi mail **đạt**. Supabase S3: xác thực `listBuckets` chỉ đọc **đạt**; không lưu danh sách bucket.
- JAR vừa build không chứa `.env` hay literal của các secret DB/JWT/mail/S3 đã kiểm tra. Dockerfile chỉ copy `pom.xml` và `src`; Compose cung cấp biến lúc chạy qua `env_file`, không cần bake secrets vào image.

## Profile

| Profile | Cấu hình và lượt khởi động |
|---|---|
| `dev` từ backend working directory | Đọc `.env` nhưng dùng H2, JWT mẫu, `DevEmailService`/`DevStorageService`, bật Swagger. Backend khởi động; events và OpenAPI trả 200. Không kiểm tra migrations PostgreSQL trong lượt H2. |
| mặc định | Dùng PostgreSQL/SMTP/Supabase; Swagger đang tắt trong `.env`. |
| `docker` | Dùng PostgreSQL nhưng chọn mail/storage giả và demo initializer. Không tương đương profile production. |
| `prod` | Không có `application-prod.yaml`; kế thừa cấu hình mặc định, mail/storage thật. |

Context mặc định/docker/prod đã khởi động với DB thật ở chế độ chỉ đọc. Flyway, seed, backfill và scheduling được tắt **chỉ trong harness kiểm tra** để tránh tác động DB. Events trả 500 vì DB chưa có cột `events.publication_announced`; OpenAPI trả 401 vì Swagger đang tắt. Đây không phải một lượt khởi động production nguyên trạng thành công.

Lưu ý: nếu export hoặc inject `SPRING_DATASOURCE_*` vào OS environment rồi chọn `dev`, URL PostgreSQL ghi đè URL H2 trong YAML, trong khi driver vẫn là H2. Dùng env dành riêng cho dev, tránh dùng nguyên env production cho container dev.

## Các vấn đề chặn chạy thực tế

1. **Docker thiếu dung lượng:** ổ `/` đầy. Multi-stage build chạy được Maven package nhưng thất bại ở layer copy/export JAR với `no space left on device`. Việc tạo container cũng bị chặn; chưa xác minh runtime Docker hiện tại. Image `backend-backend:latest` sẵn có tạo từ 31/07 và còn healthcheck `/actuator/health`, khác Dockerfile hiện tại.
2. **Flyway validate thất bại trên DB cấu hình thật:** V16, V33 và V34 có `CHECKSUM_MISMATCH`. DB mới đến V34; V35–V41 còn pending. Khởi động nguyên trạng với Flyway bật sẽ bị chặn. Đối chiếu script đã áp dụng và schema thực tế trước khi sửa lịch sử migration; không repair tự động.
3. **Compose cố định port 8080:** `ports` và healthcheck vẫn dùng 8080 dù ứng dụng ưu tiên `PORT`, sau đó `SERVER_PORT`. Fixture PORT=19090/SERVER_PORT=18080 chứng minh container env đổi mà mapping/healthcheck không đổi. Cần đồng bộ port mapping, env interpolation và healthcheck.
4. **ENV_SETUP.md lỗi thời:** mô tả Compose có DB container, dùng mocks và không cần `.env`, trong khi Compose hiện có một backend service, dùng DB bên ngoài và profile mặc định. `backend/README.md` mô tả đúng hơn.

## Bước tiếp theo

- Giải phóng dung lượng ổ hệ thống trước khi build lại. Cache mới do lượt build audit tạo cần thu hồi khi Docker ghi metadata được trở lại; cleanup theo ID hiện bị ENOSPC, không prune images/volumes của người dùng.
- Đối chiếu V16/V33/V34 với schema/history và chọn cách khôi phục scripts hoặc remediation có kiểm chứng; sau đó áp dụng V35–V41.
- Đồng bộ Compose port/healthcheck và tài liệu, rồi chạy lại container trên DB test trước khi kiểm tra môi trường thật.

Kết quả chi tiết (chỉ keys, booleans, HTTP status và metadata migration): [environment audit JSON](2026-10-02-backend-environment-audit.json).

Tham khảo: [Compose env precedence](https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/), [Supabase S3 authentication](https://supabase.com/docs/guides/storage/s3/authentication).
