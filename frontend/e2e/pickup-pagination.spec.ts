import { test, expect } from "@playwright/test";

for (const width of [1440, 375]) {
  test(`organizer reaches and checks in the final pickup page at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.addInitScript(() => {
      localStorage.setItem("uitmerch-auth", JSON.stringify({ version: 0, state: {
        user: { id: "perf-owner", email: "owner@example.test", fullName: "Organizer", role: "ORGANIZER", isVerified: true },
        accessToken: `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 7200 }))}.signature`,
        refreshToken: "test-refresh", tokenType: "Bearer",
      } }));
    });
    const org = { id: "perf-org", ownerId: "perf-owner", name: "Tổ chức UIT", status: "ACTIVE", totalMerch: 0 };
    const last = { id: "99999999-last", guestName: "Khách cuối danh sách", status: "READY" };
    let completed = false;
    const requestedPages: number[] = [];
    await page.route("**/api/v1/**", async route => {
      const request = route.request();
      const url = new URL(request.url());
      const path = url.pathname;
      let data: unknown = [];
      let meta: unknown;
      if (path.endsWith("/stream")) return route.fulfill({ contentType: "text/event-stream", body: "" });
      if (path === "/api/v1/organizations/mine") data = [org];
      else if (path.endsWith("/pickup-schedules")) data = [{ id: "schedule", orgId: org.id, pickupDate: "2026-10-05", pickupTimeSlot: "09:00–11:00", location: "UIT", orderCount: 21 }];
      else if (path.endsWith("/orders/page")) {
        expect(request.headers().authorization).toMatch(/^Bearer /);
        expect(url.searchParams.get("size")).toBe("20");
        const index = Number(url.searchParams.get("page")); requestedPages.push(index);
        data = index === 0 ? Array.from({ length: 20 }, (_, i) => ({ id: `${String(i + 1).padStart(8, "0")}-order`, guestName: `Khách ${i + 1}`, status: "READY" })) : [{ ...last, status: completed ? "COMPLETED" : "READY" }];
        meta = { page: index, pageSize: 20, totalElements: 21, totalPages: 2, hasNext: index === 0, hasPrevious: index > 0 };
      } else if (path.endsWith(`/orders/${last.id}/checkin`)) {
        expect(request.method()).toBe("PATCH"); completed = true; data = { ...last, status: "COMPLETED" };
      } else if (path.endsWith("/unread-count")) data = { unreadCount: 0 };
      else if (path.endsWith("/notifications")) data = { content: [], totalElements: 0, totalPages: 0 };
      await route.fulfill({ contentType: "application/json", body: JSON.stringify({ success: true, message: "OK", data, meta }) });
    });
    await page.goto("/organizer?orgId=perf-org&tab=pickup");
    await page.getByRole("button", { name: "▼ Xem danh sách check-in" }).click();
    await expect(page.getByText("Trang 1 / 2 · 21 đơn")).toBeVisible();
    await page.getByRole("button", { name: "Trang sau" }).click();
    await expect(page.getByText("Khách cuối danh sách")).toBeVisible();
    await expect(page.getByRole("button", { name: "Trang sau" })).toBeDisabled();
    await page.getByRole("button", { name: "Check-in đơn 99999999" }).click();
    await expect(page.getByText("✓ Đã nhận")).toBeVisible();
    await expect(page.getByText("Trang 2 / 2 · 21 đơn")).toBeVisible();
    expect(requestedPages).toContain(0); expect(requestedPages).toContain(1);
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBe(true);
    await page.screenshot({ path: `test-results/pickup-pages-${width}.png`, fullPage: true });
  });
}
