import { test, expect, type Page } from "@playwright/test";
const campaign = {
  id: "collection",
  orgId: "public-org",
  title: "Campus Stories — Mặc dấu ấn UIT",
  description: "Bộ sưu tập áo dành cho cộng đồng sinh viên UIT.",
  minimumQuantity: 10,
  reservedQuantity: 2,
  deadline: "2099-10-17T10:00:00Z",
  state: "ACTIVE",
  createdAt: "2026-10-03T00:00:00Z",
  variants: [
    {
      merchId: "shirt",
      label: "Áo Campus — M",
      unitPrice: 150000,
      availableQuantity: 20,
      available: true,
    },
  ],
};
const spring = (content: unknown[]) => ({
  content,
  number: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  last: true,
  first: true,
});
async function setup(
  page: Page,
  options: { role?: string; followerCount?: number; following?: boolean } = {},
) {
  if (options.role)
    await page.addInitScript(
      (role) =>
        localStorage.setItem(
          "uitmerch-auth",
          JSON.stringify({
            state: {
              user: {
                id: "test-account",
                email: "test@example.test",
                fullName: "Người dùng Demo",
                role,
                isVerified: true,
              },
              accessToken: `h.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))}.s`,
              refreshToken: "test-refresh",
              tokenType: "Bearer",
            },
            version: 0,
          }),
        ),
      options.role,
    );
  let following = options.following ?? false;
  const notices = [
    {
      id: "notice-one",
      title: "Đơn hàng đã được xác nhận",
      message: "Tổ chức đã xác nhận đơn của bạn. Hãy theo dõi lịch nhận.",
      read: false,
      createdAt: "2026-10-03T02:00:00Z",
      relatedOrderId: "order-demo",
    },
    {
      id: "notice-two",
      title: "Vật phẩm mới từ UIT",
      message: "Khám phá bộ sưu tập mới của cộng đồng UIT.",
      read: false,
      createdAt: "2026-10-03T01:00:00Z",
    },
    {
      id: "notice-read",
      title: "Lịch nhận đã cập nhật",
      message: "Thông báo bạn đã đọc trước đó.",
      read: true,
      createdAt: "2026-10-02T01:00:00Z",
    },
  ];
  const writes: string[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname;
    if (path.endsWith("/stream"))
      return route.fulfill({ contentType: "text/event-stream", body: "" });
    if (req.method() !== "GET") writes.push(path);
    let data: unknown = [];
    if (path === "/api/v1/public/campaigns")
      data = spring([
        campaign,
        {
          ...campaign,
          id: "closed",
          title: "UIT Notes — Sẵn sàng trao tay",
          state: "SUCCEEDED",
          reservedQuantity: 10,
        },
      ]);
    else if (path === "/api/v1/public/campaigns/collection") data = campaign;
    else if (path === "/api/v1/public/campaigns/closed")
      data = {
        ...campaign,
        id: "closed",
        title: "UIT Notes — Sẵn sàng trao tay",
        state: "SUCCEEDED",
        reservedQuantity: 10,
      };
    else if (path === "/api/v1/public/organizations/public-org")
      data = {
        id: "public-org",
        ownerId: "organizer",
        name: "CLB Sáng tạo UIT",
        status: "ACTIVE",
        description: "Tổ chức của cộng đồng sinh viên.",
        followerCount: options.followerCount ?? 17,
        totalMerch: 0,
      };
    else if (path === "/api/v1/customer/following")
      data = spring(
        following
          ? [
              {
                orgId: "public-org",
                notifyMerch: true,
                notifyEvents: true,
                emailEnabled: false,
              },
            ]
          : [],
      );
    else if (path === "/api/v1/customer/following/public-org") {
      following = req.method() === "POST";
      data = { orgId: "public-org", ...req.postDataJSON() };
    } else if (path.endsWith("/notifications/read-all")) {
      notices.forEach((n) => (n.read = true));
      data = null;
    } else if (path.endsWith("/notice-one/read")) {
      notices[0].read = true;
      data = null;
    } else if (path.endsWith("/unread-count"))
      data = { unreadCount: notices.filter((n) => !n.read).length };
    else if (path.endsWith("/notifications")) data = spring(notices);
    await route.fulfill({
      json: {
        success: true,
        data,
        meta: {
          page: 0,
          pageSize: 20,
          totalElements: Array.isArray(data) ? data.length : 1,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
    });
  });
  return writes;
}
async function fits(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
}
for (const width of [1440, 375, 320]) {
  test(`collection cards highlight goals and deadline at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await setup(page);
    await page.goto("/campaigns");
    const card = page.getByRole("article", {
      name: campaign.title,
      exact: true,
    });
    await expect(
      card.getByRole("heading", { name: campaign.title, exact: true }),
    ).toBeVisible();
    await expect(
      card.getByRole("region", { name: "Mục tiêu đặt trước" }),
    ).toContainText("10 sản phẩm");
    await expect(
      card.getByRole("region", { name: "Hạn đặt trước" }),
    ).toContainText("17/10/2099");
    await expect(
      card.getByRole("progressbar", { name: "Tiến độ đặt trước" }),
    ).toHaveAttribute("value", "2");
    await expect(
      card.getByRole("link", { name: "Khám phá bộ sưu tập" }),
    ).toBeVisible();
    await fits(page);
    await page.screenshot({
      path: `test-results/collections-polished-${width}.png`,
      fullPage: true,
    });
  });
  test(`reservation information and cost are clear at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await setup(page, { role: "CUSTOMER" });
    await page.goto("/campaigns/collection");
    await expect(
      page.getByRole("heading", { name: campaign.title, exact: true }),
    ).toHaveCount(1);
    await page.getByRole("combobox").selectOption("shirt");
    await page.getByRole("spinbutton", { name: "Số lượng" }).fill("2");
    await expect(page.locator(".campaign-cost")).toContainText("300.000");
    await expect(
      page.getByRole("button", { name: "Giữ chỗ", exact: true }),
    ).toBeEnabled();
    await expect(
      page.getByRole("heading", { name: "Đặt trước hoạt động thế nào?" }),
    ).toBeVisible();
    await fits(page);
    await page.evaluate(() => scrollTo(0, 0));
    await page.screenshot({
      path: `test-results/reservation-polished-${width}.png`,
      fullPage: true,
    });
  });
  test(`read controls remain visible and notification list scrolls at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 812 });
    await setup(page, { role: "CUSTOMER" });
    await page.goto("/campaigns");
    await page.getByRole("button", { name: "Thông báo", exact: true }).click();
    await expect(page.locator(".notification-read-state.unread")).toHaveCount(
      2,
    );
    await expect(page.locator(".notification-read-state.read")).toHaveCount(1);
    const panel = page.getByRole("region", { name: "Danh sách thông báo" });
    const box = await panel.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(width);
    expect(box!.y + box!.height).toBeLessThanOrEqual(812);
    await page.screenshot({
      path: `test-results/notifications-polished-${width}.png`,
    });
  });
}
for (const count of [0, 17]) {
  for (const role of [undefined, "CUSTOMER"]) {
    test(`${role ?? "guest"} sees ${count} followers without following`, async ({
      page,
    }) => {
      await setup(page, { role, followerCount: count });
      await page.goto("/organization/public-org");
      await expect(page.getByLabel("Số người theo dõi tổ chức")).toContainText(
        `${count} người theo dõi`,
      );
      if (role)
        await expect(
          page.getByRole("button", { name: "Theo dõi tổ chức", exact: true }),
        ).toBeVisible();
      else
        await expect(
          page.getByRole("link", { name: "Đăng nhập để theo dõi" }),
        ).toBeVisible();
    });
  }
}
test("following toggles update the public count", async ({ page }) => {
  await setup(page, { role: "CUSTOMER", followerCount: 17 });
  await page.goto("/organization/public-org");
  await page
    .getByRole("button", { name: "Theo dõi tổ chức", exact: true })
    .click();
  await expect(page.getByLabel("Số người theo dõi tổ chức")).toContainText(
    "18 người theo dõi",
  );
  await page
    .getByRole("button", { name: "Đang theo dõi", exact: true })
    .click();
  await expect(page.getByLabel("Số người theo dõi tổ chức")).toContainText(
    "17 người theo dõi",
  );
});
test("closed collections explain status and show an order link instead of a disabled form", async ({
  page,
}) => {
  await setup(page, { role: "CUSTOMER" });
  await page.goto("/campaigns/closed");
  await expect(
    page.getByRole("heading", {
      name: "Bộ sưu tập đã đạt mục tiêu",
      exact: true,
    }),
  ).toBeVisible();
  await expect(page.getByRole("combobox")).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "Xem đơn giữ chỗ", exact: true }),
  ).toHaveAttribute("href", "/reservations");
});
test("marking one notification read stays on the page and updates the counter", async ({
  page,
}) => {
  const writes = await setup(page, { role: "CUSTOMER" });
  await page.goto("/campaigns");
  await page.getByRole("button", { name: "Thông báo", exact: true }).click();
  await page
    .getByRole("button", {
      name: "Đánh dấu đã đọc: Đơn hàng đã được xác nhận",
      exact: true,
    })
    .click();
  await expect(page).toHaveURL(/\/campaigns$/);
  await expect(
    page
      .getByRole("status")
      .filter({ hasText: "Đã đánh dấu thông báo là đã đọc." }),
  ).toBeVisible();
  await expect(page.locator(".notification-item.is-unread")).toHaveCount(1);
  await expect(page.locator(".notification-item.is-read")).toHaveCount(2);
  await expect(page.getByLabel("Chưa đọc", { exact: true })).toHaveText("1");
  expect(writes).toEqual(["/api/v1/customer/notifications/notice-one/read"]);
});
test("failed single read leaves the unread indicator and counter unchanged", async ({
  page,
}) => {
  await setup(page, { role: "CUSTOMER" });
  await page.route("**/api/v1/customer/notifications/notice-one/read", (r) =>
    r.fulfill({
      status: 500,
      json: { success: false, message: "Không thể đánh dấu đã đọc." },
    }),
  );
  await page.goto("/campaigns");
  await page.getByRole("button", { name: "Thông báo", exact: true }).click();
  const button = page.getByRole("button", {
    name: "Đánh dấu đã đọc: Đơn hàng đã được xác nhận",
    exact: true,
  });
  await button.click();
  await expect(page.getByRole("alert")).toContainText(
    "Không thể đánh dấu đã đọc.",
  );
  await expect(button).toBeEnabled();
  await expect(page.locator(".notification-item.is-unread")).toHaveCount(2);
  await expect(page.getByLabel("Chưa đọc", { exact: true })).toHaveText("2");
});
