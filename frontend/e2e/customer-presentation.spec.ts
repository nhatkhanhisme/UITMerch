import { mockBrowserSession } from "./auth-fixture";
import { test, expect, type Page } from "@playwright/test";

const customer = {
  id: "ui-customer",
  fullName: "Nguyễn Minh An",
  email: "minhan@example.test",
  role: "CUSTOMER",
  isVerified: true,
};
const profile = {
  ...customer,
  phone: "0900000000",
  address: "Ký túc xá khu B, Đại học Quốc gia TP. Hồ Chí Minh",
  createdAt: "2026-05-16T00:00:00Z",
};
const order = {
  id: "0943ebb5-1111-4222-a333-000000000000",
  orgId: "ui-org",
  status: "READY",
  totalAmount: 250000,
  paymentMethod: "CASH_ON_DELIVERY",
  paymentStatus: "PENDING",
  guestName: customer.fullName,
  guestPhone: profile.phone,
  createdAt: "2026-10-03T00:00:00Z",
  items: [
    {
      id: "ui-item",
      merchId: "ui-merch",
      merchName: "Áo khoa Hệ thống Thông tin",
      unitPrice: 125000,
      quantity: 2,
      subtotal: 250000,
    },
  ],
  pickupSchedule: {
    id: "ui-schedule",
    pickupDate: "2026-10-05",
    pickupTimeSlot: "09:00–11:00",
    location: "Phòng B101, trường Đại học Công nghệ Thông tin",
  },
};
const pageData = (content: unknown[]) => ({
  content,
  number: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  last: true,
  first: true,
});
async function setup(page: Page, notifications = 0) {
  await mockBrowserSession(page, customer);
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.startsWith("/api/v1/auth/")) return route.fallback();
    if (path.endsWith("/stream"))
      return route.fulfill({ contentType: "text/event-stream", body: "" });
    let data: unknown = [];
    if (path.endsWith("/unread-count")) data = { unreadCount: notifications };
    else if (path === "/api/v1/customer/profile")
      data =
        route.request().method() === "PATCH"
          ? { ...profile, ...route.request().postDataJSON() }
          : profile;
    else if (path.endsWith("/pickup-token"))
      data = {
        orderId: order.id,
        token: "x".repeat(43),
        pickupScheduleId: "ui-schedule",
        expiresAt: new Date(Date.now() + 600000).toISOString(),
      };
    else if (path.endsWith("/campaign-context"))
      data = {
        campaignId: null,
        campaignState: null,
        fulfillmentAllowed: true,
      };
    else if (path.endsWith("/history")) data = pageData([]);
    else if (path === `/api/v1/customer/orders/${order.id}`) data = order;
    else if (
      path === "/api/v1/customer/following" ||
      path === "/api/v1/customer/restock-subscriptions"
    )
      data = pageData([]);
    else if (path === "/api/v1/customer/notifications")
      data = pageData(
        Array.from({ length: notifications }, (_, i) => ({
          id: `notice-${i}`,
          title: "Lịch nhận hàng đã được cập nhật",
          message:
            "Đơn hàng của bạn đã sẵn sàng. Vui lòng đến địa điểm nhận hàng trong khung giờ đã thông báo.",
          isRead: false,
          createdAt: "2026-10-03T00:00:00Z",
        })),
      );
    await route.fulfill({ json: { success: true, data } });
  });
}
async function fits(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
}
for (const width of [1440, 375, 320]) {
  test(`order pickup and long credentials fit at ${width}px`, async ({
    page,
  }) => {
    await setup(page);
    await page.setViewportSize({ width, height: 900 });
    await page.goto(`/orders/${order.id}`);
    await expect(
      page.getByRole("heading", { name: "Lịch nhận hàng" }),
    ).toBeVisible();
    await expect(
      page.getByText("Chưa có cập nhật nào cho đơn hàng này."),
    ).toBeVisible();
    await page.getByRole("button", { name: "Tạo mã nhận hàng" }).click();
    const qr = page.getByRole("img", { name: "Mã QR nhận hàng" });
    await expect(qr).toBeVisible();
    const box = await qr.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(width);
    await page.getByText("Xem mã để nhập thủ công").click();
    await expect(page.getByText("x".repeat(43), { exact: true })).toBeVisible();
    await fits(page);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: `test-results/customer-order-${width}.png`,
      fullPage: true,
    });
  });
  test(`profile edit preserves values and fits at ${width}px`, async ({
    page,
  }) => {
    await setup(page);
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/profile/customer");
    await expect(
      page.getByRole("heading", { name: customer.fullName }),
    ).toBeVisible();
    await expect(page.getByText(profile.address)).toBeVisible();
    await page.screenshot({
      path: `test-results/customer-profile-${width}.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Chỉnh sửa hồ sơ" }).click();
    await expect(page.getByLabel("Họ và tên")).toHaveValue(customer.fullName);
    await page.getByLabel("Họ và tên").fill("Nguyễn Minh Anh");
    await page.getByRole("button", { name: "Lưu thay đổi" }).click();
    await expect(
      page.getByRole("heading", { name: "Nguyễn Minh Anh", exact: true }),
    ).toBeVisible();
    await fits(page);
  });
}
for (const viewport of [
  { width: 1440, height: 900 },
  { width: 375, height: 812 },
  { width: 667, height: 375 },
]) {
  test(`notifications scroll within ${viewport.width}x${viewport.height}`, async ({
    page,
  }) => {
    await setup(page, 12);
    await page.setViewportSize(viewport);
    await page.goto("/following");
    await page.getByRole("button", { name: "Thông báo", exact: true }).click();
    const panel = page.getByRole("region", { name: "Danh sách thông báo" });
    await expect(panel).toBeVisible();
    const box = await panel.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(viewport.width);
    expect(box!.y + box!.height).toBeLessThanOrEqual(viewport.height);
    await expect(
      panel
        .locator(".notification-open")
        .filter({ hasText: "Lịch nhận hàng đã được cập nhật" }),
    ).toHaveCount(12);
    expect(
      await panel
        .locator(".notification-list")
        .evaluate((el) => el.scrollHeight > el.clientHeight),
    ).toBe(true);
    await page.screenshot({
      path: `test-results/customer-notifications-${viewport.width}.png`,
    });
    await page.keyboard.press("Escape");
    await expect(panel).toHaveCount(0);
    await fits(page);
  });
}
test("empty following and restock lists offer relevant discovery actions", async ({
  page,
}) => {
  await setup(page);
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/following");
  await expect(
    page.getByRole("heading", { name: "Bạn chưa theo dõi tổ chức nào." }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Khám phá tổ chức" }),
  ).toHaveAttribute("href", "/organization");
  await page.screenshot({
    path: "test-results/customer-following-empty.png",
    fullPage: true,
  });
  await fits(page);
  await page.goto("/restock-subscriptions");
  await expect(
    page.getByRole("link", { name: "Khám phá vật phẩm" }),
  ).toHaveAttribute("href", "/merch");
  await fits(page);
});
