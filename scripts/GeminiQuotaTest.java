import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Standalone test: do 2 Gemini API keys from different projects share quota?
 *
 * Compile: javac GeminiQuotaTest.java
 * Run:     java GeminiQuotaTest
 */
public class GeminiQuotaTest {

    // ── Paste your two API keys here ──────────────────────────────────────────
    private static final String KEY1 = "AIzaSyDLzfkhTkvIxvQ2oFYDaac8WBjENGxYifE";
    private static final String KEY2 = "AIzaSyBAol4JVKrWCP-LnD-Po--JYy_qv6MFb1Q";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=";

    private static final String BODY =
        "{\"model\":\"models/gemini-embedding-001\",\"content\":{\"parts\":[{\"text\":\"test\"}]}}";

    private static final int MAX_REQUESTS = 2000;

    public static void main(String[] args) throws Exception {
        if (KEY1.startsWith("YOUR_") || KEY2.startsWith("YOUR_")) {
            System.err.println("[ERROR] Hãy điền KEY1 và KEY2 vào file trước khi chạy.");
            System.exit(1);
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        System.out.println("=== GEMINI QUOTA INDEPENDENCE TEST ===");
        System.out.println("Đang gửi request bằng KEY1 cho đến khi gặp 429...\n");

        int limitAt = -1;

        for (int i = 1; i <= MAX_REQUESTS; i++) {
            HttpRequest req = buildRequest(BASE_URL + KEY1);
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            int status = res.statusCode();
            System.out.printf("[KEY1] Request #%d → HTTP %d%n", i, status);

            if (status == 429) {
                limitAt = i;
                System.out.printf("%n>>> KEY1 bị rate-limit (429) tại request #%d%n", i);
                System.out.println("Response body: " + res.body());
                break;
            }

            if (status != 200) {
                System.out.printf("[KEY1] Request #%d gặp lỗi không mong đợi: HTTP %d%n", i, status);
                System.out.println("Body: " + res.body());
                break;
            }
        }

        if (limitAt == -1) {
            System.out.printf("%n>>> KEY1 không bị 429 sau %d request. Tăng MAX_REQUESTS nếu cần.%n", MAX_REQUESTS);
        }

        System.out.println("\n--- Ngay lập tức kiểm tra KEY2 ---");
        HttpRequest req2 = buildRequest(BASE_URL + KEY2);
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        int status2 = res2.statusCode();
        System.out.printf("[KEY2] 1 request → HTTP %d%n", status2);
        System.out.println("Body: " + res2.body());

        System.out.println("\n=== KẾT LUẬN ===");
        if (limitAt > 0) {
            System.out.printf("KEY1 bị giới hạn tại request #%d.%n", limitAt);
        } else {
            System.out.println("KEY1 không bị giới hạn trong phạm vi test.");
        }

        if (status2 == 200) {
            System.out.println("KEY2 thành công (HTTP 200).");
            System.out.println("→ Quota ĐỘC LẬP: 2 key từ 2 project khác nhau có quota riêng biệt.");
        } else if (status2 == 429) {
            System.out.println("KEY2 cũng bị 429.");
            System.out.println("→ Quota DÙNG CHUNG: 2 key bị tính chung quota (có thể theo Google account).");
        } else {
            System.out.printf("KEY2 trả về HTTP %d — kiểm tra thủ công.%n", status2);
        }
    }

    private static HttpRequest buildRequest(String url) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(BODY))
            .timeout(Duration.ofSeconds(15))
            .build();
    }
}
