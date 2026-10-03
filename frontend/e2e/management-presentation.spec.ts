import { test, expect, type Page } from "@playwright/test";
const org = {
  id: "demo-org",
  ownerId: "demo-organizer",
  name: "Câu lạc bộ Sáng tạo UIT",
  description: "Cộng đồng sinh viên cùng sáng tạo vật phẩm và sự kiện.",
  status: "ACTIVE",
  followerCount: 12,
  totalMerch: 3,
  logoUrl: "/assets/figma/logo-header.svg",
  coverUrl: "/assets/figma/logo-header.svg",
  createdAt: "2026-10-01T00:00:00Z",
  updatedAt: "2026-10-01T00:00:00Z",
};
const product = {
  id: "demo-merch",
  orgId: org.id,
  name: "Túi vải UIT",
  description: "Vật phẩm demo",
  stock: 10,
  price: 70000,
  status: "PUBLISHED",
  images: ["/assets/figma/logo-header.svg"],
};
const free = {
  ...product,
  id: "free-merch",
  name: "Sticker UIT miễn phí",
  price: 0,
};
const order = {
  id: "0943ebb5-1111-4222-a333-000000000000",
  orgId: org.id,
  userId: "demo-customer",
  guestName: "Nguyễn Minh An",
  guestPhone: "0900000000",
  status: "READY",
  totalAmount: 140000,
  createdAt: "2026-10-01T00:00:00Z",
  paymentMethod: "CASH_ON_DELIVERY",
  paymentStatus: "PENDING",
  items: [
    {
      id: "line",
      merchId: product.id,
      merchName: product.name,
      quantity: 2,
      unitPrice: 70000,
      subtotal: 140000,
    },
  ],
  pickupSchedule: {
    id: "schedule",
    pickupDate: "2026-10-05",
    pickupTimeSlot: "09:00–11:00",
    location: "Phòng B101",
  },
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
async function setup(page: Page, role = "CUSTOMER") {
  const user = {
    id: `demo-${role.toLowerCase()}`,
    fullName: "Nguyễn Minh An",
    email: "demo@example.test",
    role,
    isVerified: true,
    avatarUrl: "/assets/figma/logo-header.svg",
  };
  await page.addInitScript((user) => {
    localStorage.setItem(
      "uitmerch-auth",
      JSON.stringify({
        state: {
          user,
          accessToken: `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 7200 }))}.signature`,
          refreshToken: "demo-refresh",
          tokenType: "Bearer",
        },
        version: 0,
      }),
    );
  }, user);
  let read = false;
  let active = true;
  let follows = true;
  const writes: { path: string; method: string; body: any; url: string }[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname;
    let data: unknown = [];
    if (path.endsWith("/stream"))
      return route.fulfill({ contentType: "text/event-stream", body: "" });
    if (["POST", "PATCH", "DELETE"].includes(req.method()))
      writes.push({
        path,
        method: req.method(),
        body: req.postData() ? req.postDataJSON() : null,
        url: req.url(),
      });
    if (path.endsWith("/read-all")) {
      read = true;
      data = null;
    } else if (path.endsWith("/unread-count"))
      data = { unreadCount: read ? 0 : 2 };
    else if (path.endsWith("/notifications"))
      data = spring(
        [0, 1].map((i) => ({
          id: `notice-${i}`,
          title: "Đơn hàng đã được xác nhận",
          message: "Tổ chức đã xác nhận đơn hàng của bạn.",
          read,
          createdAt: "2026-10-01T00:00:00Z",
        })),
      );
    else if (path === "/api/v1/customer/orders")
      data = [
        order,
        {
          ...order,
          id: "cancelled-order",
          status: "CANCELLED",
          cancelReason: "Tôi không còn nhu cầu nữa",
        },
      ].filter(
        (o) =>
          !url.searchParams.get("status") ||
          o.status === url.searchParams.get("status"),
      );
    else if (path === "/api/v1/public/events/event-demo")
      data = {
        id: "event-demo",
        orgId: org.id,
        title: "Ngày hội Sáng tạo UIT — Gặp gỡ, trải nghiệm và nhận quà",
        description:
          "Cùng gặp gỡ cộng đồng sinh viên và khám phá những ý tưởng mới.\nĐịa điểm: phòng B101.",
        status: "PUBLISHED",
        startsAt: "2030-10-05T09:00:00",
        endsAt: "2030-10-05T16:00:00",
        coverUrl: "/assets/figma/logo-header.svg",
        merch: [free, product],
      };
    else if (path === `/api/v1/public/organizations/${org.id}`) data = org;
    else if (path === "/api/v1/customer/following")
      data = spring(
        follows
          ? [
              {
                orgId: org.id,
                orgName: org.name,
                orgStatus: "ACTIVE",
                logoUrl: org.logoUrl,
                notifyMerch: true,
                notifyEvents: true,
                emailEnabled: false,
                followedAt: "2026-10-01T00:00:00Z",
              },
            ]
          : [],
      );
    else if (
      path.startsWith("/api/v1/customer/following/") &&
      req.method() === "DELETE"
    ) {
      follows = false;
      data = null;
    } else if (path === "/api/v1/customer/restock-subscriptions")
      data = spring([
        {
          merchId: product.id,
          merchName: product.name,
          orgName: org.name,
          available: false,
          emailEnabled: false,
          subscribedAt: "2026-10-01T00:00:00Z",
        },
      ]);
    else if (path === "/api/v1/organizations/mine")
      data = [org, { ...org, id: "second-org", name: "CLB Nghệ thuật UIT" }];
    else if (
      path === `/api/v1/organizations/${org.id}` ||
      path === "/api/v1/organizations/second-org"
    )
      data = { ...org, id: path.split("/").at(-1), ...req.postDataJSON() };
    else if (path === `/api/v1/organizations/${org.id}/merchs`)
      data =
        req.method() === "POST"
          ? { ...product, ...req.postDataJSON() }
          : [product];
    else if (path === `/api/v1/organizations/${org.id}/merchs/${product.id}`)
      data = { ...product, ...req.postDataJSON() };
    else if (path === "/api/v1/admin/users")
      data = [
        {
          id: "customer-demo",
          fullName: "Khách hàng Demo",
          email: "customer@example.test",
          role: "CUSTOMER",
          active,
          verified: true,
        },
        {
          id: "inactive-demo",
          fullName: "Tài khoản chưa xác minh",
          email: "inactive@example.test",
          role: "ORGANIZER",
          active: false,
          verified: false,
        },
      ];
    else if (path === "/api/v1/admin/users/customer-demo/active") {
      active = url.searchParams.get("active") === "true";
      data = {
        id: "customer-demo",
        fullName: "Khách hàng Demo",
        email: "customer@example.test",
        role: "CUSTOMER",
        active,
        verified: true,
      };
    } else if (path === "/api/v1/admin/organizations")
      data = [
        org,
        {
          ...org,
          id: "pending-org",
          name: "Tổ chức chờ duyệt",
          status: "PENDING",
        },
      ];
    else if (path === "/api/v1/admin/orders") data = [order];
    else if (path.includes("/admin/organizations/") && path.endsWith("/status"))
      data = { ...org, ...req.postDataJSON() };
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
  test(`order list cards and status filters are readable at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await setup(page);
    await page.goto("/orders");
    const cards = page.locator(".order-list-card");
    await expect(cards).toHaveCount(2);
    expect(
      await cards
        .first()
        .evaluate((el) => getComputedStyle(el).backgroundColor),
    ).toBe("rgb(255, 255, 255)");
    await page
      .getByRole("button", { name: "Sẵn sàng nhận", exact: true })
      .click();
    await expect(cards).toHaveCount(1);
    await expect(
      page.getByRole("button", { name: "Sẵn sàng nhận", exact: true }),
    ).toHaveAttribute("aria-pressed", "true");
    await fits(page);
    await page.screenshot({
      path: `test-results/orders-list-${width}.png`,
      fullPage: true,
    });
  });
  test(`event detail fits and retains free product at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await setup(page);
    await page.goto("/event/event-demo");
    await expect(
      page.getByRole("heading", { name: /Ngày hội Sáng tạo/ }),
    ).toBeVisible();
    await expect(page.getByText("Miễn phí", { exact: true })).toBeVisible();
    await expect(page.getByText("Sắp diễn ra", { exact: true })).toBeVisible();
    if (width === 1440)
      await expect(
        page
          .getByRole("navigation", { name: "Điều hướng chính", exact: true })
          .getByRole("link", { name: "Sự kiện" }),
      ).toHaveAttribute("aria-current", "page");
    await fits(page);
    await page.screenshot({
      path: `test-results/event-detail-${width}.png`,
      fullPage: true,
    });
  });
  test(`organizer profile edit is localized and clears media at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    const writes = await setup(page, "ORGANIZER");
    await page.goto("/profile/organizer");
    await page
      .getByRole("button", { name: "Chỉnh sửa hồ sơ", exact: true })
      .click();
    await page
      .getByRole("textbox", { name: "Tên tổ chức" })
      .fill("CLB Sáng tạo mới");
    await page.getByRole("textbox", { name: "Giới thiệu tổ chức" }).fill("");
    await page.getByRole("button", { name: "Bỏ logo", exact: true }).click();
    await page
      .getByRole("button", { name: "Lưu thay đổi", exact: true })
      .click();
    await expect(
      page.getByRole("heading", { name: "CLB Sáng tạo mới" }),
    ).toBeVisible();
    expect(
      writes.find((w) => w.path === `/api/v1/organizations/${org.id}`)?.body,
    ).toMatchObject({ name: "CLB Sáng tạo mới", description: "", logoUrl: "" });
    await fits(page);
    await page.screenshot({
      path: `test-results/organizer-profile-${width}.png`,
      fullPage: true,
    });
  });
}
test("mark all read confirms completion and disables redundant writes", async ({
  page,
}) => {
  const writes = await setup(page);
  await page.goto("/orders");
  await page.getByRole("button", { name: "Thông báo", exact: true }).click();
  await page
    .getByRole("button", { name: "Đánh dấu tất cả đã đọc", exact: true })
    .click();
  await expect(
    page
      .getByRole("status")
      .filter({ hasText: "Đã đánh dấu tất cả là đã đọc." }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Đánh dấu tất cả đã đọc", exact: true }),
  ).toBeDisabled();
  await expect(page.getByLabel("Chưa đọc", { exact: true })).toHaveCount(0);
  expect(writes.filter((w) => w.path.endsWith("/read-all"))).toHaveLength(1);
  await page.screenshot({
    path: "test-results/notifications-read-confirmation.png",
  });
});
test("following and stock reminders identify each organization and product", async ({
  page,
}) => {
  const writes = await setup(page);
  await page.goto("/following");
  await expect(
    page.getByRole("link", { name: org.name, exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("group", { name: "Nhận cập nhật về" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Bỏ theo dõi", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Bạn chưa theo dõi tổ chức nào." }),
  ).toBeVisible();
  expect(writes.some((w) => w.method === "DELETE")).toBe(true);
  await page.goto("/restock-subscriptions");
  await expect(
    page.getByRole("heading", { name: "Nhắc khi có hàng", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: product.name, exact: true }),
  ).toBeVisible();
  await expect(page.getByText(org.name, { exact: true })).toBeVisible();
});
test("organizer sends explicit zero for create and update", async ({
  page,
}) => {
  const writes = await setup(page, "ORGANIZER");
  await page.goto(`/organizer?orgId=${org.id}`);
  await page.getByRole("button", { name: "Sửa", exact: true }).click();
  await page
    .getByRole("button", { name: "Miễn phí (0đ)", exact: true })
    .click();
  await page.getByRole("button", { name: "Lưu", exact: true }).click();
  expect(
    writes.find(
      (w) => w.method === "PATCH" && w.path.endsWith(`/merchs/${product.id}`),
    )?.body.price,
  ).toBe(0);
  await page
    .getByRole("button", { name: "+ Thêm vật phẩm", exact: true })
    .click();
  await page
    .locator(".organizer-edit-form input[required]")
    .first()
    .fill("Sticker miễn phí");
  await page.getByLabel("Giá bán (VNĐ) *", { exact: true }).fill("0");
  await page.getByRole("button", { name: "Lưu", exact: true }).click();
  expect(
    writes.find((w) => w.method === "POST" && w.path.endsWith("/merchs"))?.body
      .price,
  ).toBe(0);
  await fits(page);
});
test("admin reads Java booleans and confirms activation changes", async ({
  page,
}) => {
  const writes = await setup(page, "ADMIN");
  await page.goto("/admin");
  const row = page.getByRole("row").filter({ hasText: "Khách hàng Demo" });
  await expect(row.getByText("Hoạt động", { exact: true })).toBeVisible();
  await expect(row.getByText("Đã xác minh", { exact: true })).toBeVisible();
  await row.getByRole("button", { name: "Vô hiệu hóa", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  expect(writes).toHaveLength(0);
  await page.getByRole("button", { name: "Quay lại", exact: true }).click();
  expect(writes).toHaveLength(0);
  await row.getByRole("button", { name: "Vô hiệu hóa", exact: true }).click();
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(row.getByText("Đã vô hiệu hóa", { exact: true })).toBeVisible();
  expect(writes[0].url).toContain("active=false");
  await page.screenshot({
    path: "test-results/admin-accounts.png",
    fullPage: true,
  });
});
test("admin organization transitions and canonical order statuses", async ({
  page,
}) => {
  const writes = await setup(page, "ADMIN");
  await page.goto("/admin");
  await page.getByRole("button", { name: "Tổ chức", exact: true }).click();
  const card = page.locator(".admin-org-card").filter({ hasText: org.name });
  await expect(
    card.getByRole("button", { name: "Tạm ngừng hoạt động", exact: true }),
  ).toBeVisible();
  await expect(card.getByRole("button", { name: /Chờ duyệt/ })).toHaveCount(0);
  await card
    .getByRole("button", { name: "Tạm ngừng hoạt động", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toContainText(
    "Vật phẩm đang công bố sẽ bị ẩn",
  );
  await page.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(writes[0].body).toEqual({ status: "INACTIVE" });
  await page.getByRole("button", { name: "Đơn hàng", exact: true }).click();
  const request = page.waitForRequest(
    (r) =>
      r.url().includes("/admin/orders") &&
      new URL(r.url()).searchParams.get("status") === "READY",
  );
  await page
    .getByRole("button", { name: "Sẵn sàng nhận", exact: true })
    .click();
  await request;
  await expect(
    page.getByRole("cell", { name: "Sẵn sàng nhận", exact: true }),
  ).toBeVisible();
  await page.getByText("Xem đơn", { exact: true }).click();
  await expect(page.getByText(/Túi vải UIT × 2/)).toBeVisible();
  await page.screenshot({
    path: "test-results/admin-orders.png",
    fullPage: true,
  });
});

test("failed mark all read retains unread state and permits retry", async ({ page }) => {
  await setup(page);
  await page.route("**/api/v1/customer/notifications/read-all", route => route.fulfill({ status: 500, json: { success:false, message:"Không thể cập nhật thông báo." } }));
  await page.goto("/orders");
  await page.getByRole("button",{name:"Thông báo",exact:true}).click();
  const button = page.getByRole("button",{name:"Đánh dấu tất cả đã đọc",exact:true});
  await button.click();
  await expect(page.getByRole("alert").filter({hasText:"Không thể cập nhật thông báo."})).toBeVisible();
  await expect(button).toBeEnabled();
  await expect(page.locator(".notification-item.is-unread")).toHaveCount(2);
  await expect(page.getByText("Đã đánh dấu tất cả là đã đọc.",{exact:true})).toHaveCount(0);
});

test("organizer profile edits the selected organization", async ({ page }) => {
  const writes = await setup(page,"ORGANIZER");
  await page.goto("/profile/organizer");
  await page.getByLabel("Tổ chức đang chỉnh sửa").selectOption("second-org");
  await expect(page.getByRole("heading",{name:"CLB Nghệ thuật UIT",exact:true})).toBeVisible();
  await page.getByRole("button",{name:"Chỉnh sửa hồ sơ",exact:true}).click();
  await page.getByRole("textbox",{name:"Tên tổ chức"}).fill("Tổ chức thứ hai đã sửa");
  await page.getByRole("button",{name:"Lưu thay đổi",exact:true}).click();
  await expect(page.getByRole("heading",{name:"Tổ chức thứ hai đã sửa",exact:true})).toBeVisible();
  expect(writes.find(w => w.path === "/api/v1/organizations/second-org")?.body.name).toBe("Tổ chức thứ hai đã sửa");
  expect(writes.some(w => w.path === `/api/v1/organizations/${org.id}`)).toBe(false);
});

for (const width of [375,320]) {
  test(`admin management screens stay within ${width}px`, async ({ page }) => {
    await page.setViewportSize({width,height:900});
    await setup(page,"ADMIN");
    await page.goto("/admin");
    await expect(page.getByRole("table")).toBeVisible();
    await fits(page);
    await page.getByRole("button",{name:"Tổ chức",exact:true}).click();
    await expect(page.locator(".admin-org-card")).toHaveCount(2);
    await fits(page);
    await page.getByRole("button",{name:"Đơn hàng",exact:true}).click();
    await expect(page.getByRole("table")).toBeVisible();
    await fits(page);
    await page.screenshot({path:`test-results/admin-orders-${width}.png`,fullPage:true});
  });
}
