import { mockBrowserSession } from "./auth-fixture";
import { test, expect, type Page } from "@playwright/test";
const user = {
  id: "a",
  email: "a@uit.edu.vn",
  fullName: "Test Customer",
  role: "CUSTOMER",
  isVerified: true,
};
const jwt = (exp: number) =>
  `eyJhbGciOiJIUzI1NiJ9.${Buffer.from(JSON.stringify({ exp })).toString("base64url")}.signature`;
const session = {
  user,
  accessToken: jwt(Math.floor(Date.now() / 1000) + 3600),
  refreshToken: "refresh-old",
  tokenType: "Bearer",
};
const campaign = {
  id: "c",
  orgId: "o",
  title: "UIT Preorder",
  description: "Campaign",
  state: "ACTIVE",
  minimumQuantity: 5,
  reservedQuantity: 2,
  deadline: "2099-10-01T00:00:00Z",
  createdAt: "2026-10-01T00:00:00Z",
  variants: [
    {
      merchId: "m",
      label: "Blue M",
      unitPrice: 100000,
      availableQuantity: 8,
      available: true,
    },
  ],
};
const order = {
  id: "order",
  orgId: "o",
  orgName: "Test Org",
  status: "READY",
  paymentStatus: "UNPAID",
  totalAmount: 100000,
  createdAt: "2026-10-01T00:00:00Z",
  items: [
    {
      id: "i",
      merchId: "m",
      merchName: "Shirt",
      quantity: 1,
      unitPrice: 100000,
      subtotal: 100000,
    },
  ],
  guestName: "Guest",
  guestPhone: "0900000000",
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
async function setup(page: Page, role?: "CUSTOMER" | "ORGANIZER" | "ADMIN") {
  await mockBrowserSession(page, role ? { ...user, role } : undefined, session.accessToken);
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.startsWith("/api/v1/auth/")) return route.fallback();
    let data: unknown = [];
    if (path.endsWith("/stream")) {
      await route.fulfill({ contentType: "text/event-stream", body: "" });
      return;
    }
    if (path.endsWith("/unread-count")) data = { unreadCount: 0 };
    else if (path.includes("/profile")) data = { ...user, phone: "0900000000" };
    else if (path === "/api/v1/customer/restock-subscriptions")
      data = pageData([
        {
          merchId: "m",
          emailEnabled: true,
          subscribedAt: "2026-10-01T00:00:00Z",
        },
      ]);
    else if (path === "/api/v1/customer/following")
      data = pageData([
        {
          orgId: "o",
          notifyMerch: true,
          notifyEvents: true,
          emailEnabled: false,
          followedAt: "2026-10-01",
        },
      ]);
    else if (path === "/api/v1/public/campaigns") data = pageData([campaign]);
    else if (path === "/api/v1/public/campaigns/c") data = campaign;
    else if (path.endsWith("/campaign-context"))
      data = { fulfillmentAllowed: true };
    else if (path.endsWith("/history"))
      data = pageData([
        {
          id: "h",
          orderId: "order",
          fromStatus: "CONFIRMED",
          toStatus: "READY",
          source: "PICKUP_SCHEDULE",
          createdAt: "2026-10-01T00:00:00Z",
        },
      ]);
    else if (path === "/api/v1/customer/orders/order") data = order;
    else if (path.includes("pickup-token"))
      data = {
        orderId: "order",
        token: "x".repeat(43),
        pickupScheduleId: null,
        expiresAt: new Date(Date.now() + 1800000).toISOString(),
      };
    else if (path === "/api/v1/organizations/mine")
      data = [{ id: "o", ownerId: "a", name: "Test Org", status: "ACTIVE" }];
    else if (path.includes("pickup/verify")) data = order;
    else if (path.includes("pickup/checkin"))
      data = { ...order, status: "COMPLETED" };
    else if (path.endsWith("/analytics"))
      data = {
        from: "2026-10-01",
        to: "2026-10-02",
        orders: {
          total: 4,
          completed: 2,
          completedOrderValue: 200000,
          paidOrderValue: 0,
          cancellationRate: 0,
        },
        inventory: { availableUnits: 8 },
        dailyOrders: [
          {
            date: "2026-10-01",
            orders: 4,
            completed: 2,
            cancelled: 0,
            completedOrderValue: 200000,
          },
        ],
        topProducts: [],
        pickupWorkload: [],
      };
    await route.fulfill({ json: { success: true, message: "OK", data } });
  });
}
test("campaign discovery works for anonymous visitors", async ({ page }) => {
  await setup(page);
  await page.goto("/campaigns");
  await expect(page.getByText("UIT Preorder")).toBeVisible();
  await page.getByRole("link", { name: "Khám phá bộ sưu tập" }).click();
  await expect(
    page.getByRole("link", { name: "Đăng nhập để đặt trước" }),
  ).toBeVisible();
});
test("reservation timeout is retried with the original request ID and payload", async ({
  page,
}) => {
  await setup(page, "CUSTOMER");
  const requests: unknown[] = [];
  await page.route(
    "**/api/v1/customer/campaigns/c/reservations",
    async (route) => {
      requests.push(route.request().postDataJSON());
      if (requests.length === 1) await route.abort("failed");
      else
        await route.fulfill({
          json: { success: true, data: { reservation: { id: "r" }, order } },
        });
    },
  );
  await page.goto("/campaigns/c");
  await page.getByRole("combobox").selectOption("m");
  await page.getByRole("button", { name: "Giữ chỗ", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("combobox")).toBeDisabled();
  await page.reload();
  await expect(page.getByRole("combobox")).toHaveValue("m");
  await page.getByRole("button", { name: "Thử lại yêu cầu đang chờ" }).click();
  await expect(
    page.getByRole("link", { name: "Xem đơn hàng", exact: true }),
  ).toBeVisible();
  expect(requests).toHaveLength(2);
  expect(requests[1]).toEqual(requests[0]);
});
test("restock preferences and unsubscribe send the correct mutations", async ({
  page,
}) => {
  await setup(page, "CUSTOMER");
  let email: unknown;
  let removed = false;
  let emailEnabled = true;
  await page.route(
    "**/api/v1/customer/restock-subscriptions*",
    async (route) => {
      if (route.request().method() === "POST") {
        email = route.request().postDataJSON();
        emailEnabled = route.request().postDataJSON().emailEnabled;
        await route.fulfill({ json: { success: true, data: {} } });
      } else
        await route.fulfill({
          json: {
            success: true,
            data: pageData(
              removed
                ? []
                : [{ merchId: "m", emailEnabled, subscribedAt: "2026-10-01" }],
            ),
          },
        });
    },
  );
  await page.route(
    "**/api/v1/customer/restock-subscriptions/m",
    async (route) => {
      removed = true;
      await route.fulfill({ json: { success: true } });
    },
  );
  await page.goto("/restock-subscriptions");
  await page.getByRole("checkbox", { name: "Nhận qua email" }).uncheck();
  await expect.poll(() => email).toEqual({ merchId: "m", emailEnabled: false });
  await page.getByRole("button", { name: "Huỷ đăng ký" }).click();
  await expect(page.getByText("Bạn chưa đăng ký nhắc khi có hàng.")).toBeVisible();
});
test("following preferences are partial updates", async ({ page }) => {
  await setup(page, "CUSTOMER");
  let body: unknown;
  let emailEnabled = false;
  await page.route("**/api/v1/customer/following*", (route) =>
    route.fulfill({
      json: {
        success: true,
        data: pageData([
          {
            orgId: "o",
            notifyMerch: true,
            notifyEvents: true,
            emailEnabled,
            followedAt: "2026-10-01",
          },
        ]),
      },
    }),
  );
  await page.route("**/api/v1/customer/following/o", async (route) => {
    body = route.request().postDataJSON();
    emailEnabled = true;
    await route.fulfill({ json: { success: true, data: {} } });
  });
  await page.goto("/following");
  const email = page.getByRole("checkbox", { name: "Gửi thêm qua email", exact: true });
  await expect(email).not.toBeChecked();
  // React restores a controlled checkbox before the async optimistic update.
  await email.click();
  await expect.poll(() => body).toEqual({ emailEnabled: true });
  await expect(email).toBeChecked();
  await expect(email).toBeEnabled();
});
test("READY customer orders expose QR and an API history", async ({ page }) => {
  await setup(page, "CUSTOMER");
  await page.goto("/orders/order");
  await page.getByRole("button", { name: "Tạo mã nhận hàng" }).click();
  await expect(
    page.getByRole("img", { name: "Mã QR nhận hàng" }),
  ).toBeVisible();
  await expect(page.getByText("Đã cập nhật lịch nhận hàng")).toBeVisible();
  expect(
    await page.evaluate(() =>
      Object.values(localStorage).some((v) => v.includes("x".repeat(43))),
    ),
  ).toBe(false);
});
test("organizer verifies before completing pickup", async ({ page }) => {
  await setup(page, "ORGANIZER");
  await page.goto("/organizer?orgId=o&tab=scanner");
  await page.getByLabel("Mã nhận hàng", { exact: true }).fill("x".repeat(43));
  await expect(
    page.getByRole("button", { name: "Xác nhận giao hàng" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Kiểm tra mã" }).click();
  await page.getByRole("button", { name: "Xác nhận giao hàng" }).click();
  await expect(page.getByText("Đã giao hàng thành công.")).toBeVisible();
});
test("guest tracking and receipt request work on a narrow viewport", async ({
  page,
}) => {
  await setup(page);
  await page.setViewportSize({ width: 375, height: 812 });
  await page.route("**/api/v1/public/orders/order/tracking", (route) =>
    route.fulfill({ json: { success: true, data: order } }),
  );
  await page.goto("/guest-orders?orderId=order");
  await page.getByLabel("Email đặt hàng").fill("guest@uit.edu.vn");
  await page.getByLabel("Mã tra cứu từ email").fill("x".repeat(43));
  await page.getByRole("button", { name: "Tra cứu", exact: true }).click();
  await expect(page.getByText("Trạng thái: READY")).toBeVisible();
  await page.getByRole("button", { name: "Gửi hướng dẫn nhận hàng" }).click();
  await expect(page.getByText(/Nếu thông tin hợp lệ/)).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
});
test("wrong roles cannot access customer subscription settings", async ({
  page,
}) => {
  await setup(page, "ORGANIZER");
  await page.goto("/restock-subscriptions");
  await expect(
    page.getByRole("heading", { name: "Chức năng dành cho khách hàng" }),
  ).toBeVisible();
});
test("analytics distinguishes completed order value from paid value", async ({
  page,
}) => {
  await setup(page, "ORGANIZER");
  await page.goto("/organizer?orgId=o&tab=analytics");
  await expect(
    page.getByRole("heading", { name: "Thống kê tổ chức" }),
  ).toBeVisible();
  await expect(
    page.getByText("Giá trị đơn hoàn thành", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Giá trị đã thanh toán", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("Từ ngày").fill("2020-01-01");
  await expect(page.getByRole("alert")).toContainText("366 ngày");
});
test("two tabs serialize cookie rotation through Web Locks without persisted tokens", async ({
  page,
  context,
}) => {
  await setup(page, "CUSTOMER");
  await page.goto("/following");
  const second = await context.newPage();
  await setup(second, "CUSTOMER");
  await second.goto("/following");
  // Finish both initial bootstraps before replacing routes with expired-token responses.
  for (const tab of [page, second])
    await expect(tab.getByRole("checkbox", { name: "Gửi thêm qua email", exact: true })).toBeVisible();
  let rotations = 0;
  let activeRotations = 0, maximumActive = 0;
  const receivedCookies: string[] = [];
  for(const tab of [page,second]) {
    await tab.unroute("**/api/v1/auth/**");
    await tab.route("**/api/v1/auth/csrf",route=>route.fulfill({json:{success:true,data:{csrfToken:"test-csrf"}}}));
  }
  await context.route("**/api/v1/auth/refresh", async (route) => {
    rotations++;
    activeRotations++; maximumActive=Math.max(maximumActive,activeRotations);
    receivedCookies.push(route.request().headers().cookie ?? "");
    expect(route.request().headers()["x-csrf-token"]).toBe("test-csrf");
    await new Promise((r) => setTimeout(r, 100));
    activeRotations--;
    await route.fulfill({
      headers:{"Set-Cookie":`uitmerch-refresh=rotated-${rotations}; Path=/api/v1/auth; HttpOnly; SameSite=Lax`},
      json: {
        success: true,
        data: {
          token: jwt(Math.floor(Date.now() / 1000) + 7200),
          userId: "a",
          email: user.email,
          fullName: user.fullName,
          role: "CUSTOMER",
          isVerified: true,
          tokenType: "Bearer",
        },
      },
    });
  });
  for (const tab of [page, second])
    await tab.route("**/api/v1/customer/following*", async (route) => {
      if (
        route.request().headers().authorization ===
        `Bearer ${session.accessToken}`
      )
        await route.fulfill({
          status: 401,
          json: { success: false, message: "Expired" },
        });
      else
        await route.fulfill({
          json: {
            success: true,
            data: pageData([
              {
                orgId: "o",
                notifyMerch: true,
                notifyEvents: true,
                emailEnabled: false,
                followedAt: "2026-10-01",
              },
            ]),
          },
        });
    });
  await Promise.all([page.reload(), second.reload()]);
  await expect(
    page.getByRole("checkbox", { name: "Gửi thêm qua email", exact: true }),
  ).toBeVisible();
  await expect(
    second.getByRole("checkbox", { name: "Gửi thêm qua email", exact: true }),
  ).toBeVisible();
  expect(rotations).toBe(2);
  expect(maximumActive).toBe(1);
  expect(receivedCookies[1]).toContain("uitmerch-refresh=rotated-1");
  expect(await second.evaluate(() => localStorage.getItem("uitmerch-auth"))).toBeNull();
  expect((await context.cookies()).find(cookie => cookie.name === "uitmerch-refresh")?.httpOnly).toBe(true);
});

test("ordinary guest checkout keeps server order IDs for tracking", async ({
  page,
}) => {
  await setup(page);
  const product = {
    id: "m",
    orgId: "o",
    name: "Shirt",
    stock: 8,
    price: 100000,
    status: "PUBLISHED",
    images: [],
  };
  await page.route("**/api/v1/public/merch/m", (route) =>
    route.fulfill({ json: { success: true, data: product } }),
  );
  await page.route("**/api/v1/public/organizations/o", (route) =>
    route.fulfill({
      json: {
        success: true,
        data: { id: "o", name: "Test Org", status: "ACTIVE" },
      },
    }),
  );
  await page.route("**/api/v1/public/merch/m/purchase-context", (route) =>
    route.fulfill({
      json: { success: true, data: { reservationRequired: false } },
    }),
  );
  await page.route("**/api/v1/public/checkout/challenge",route=>route.fulfill({json:{success:true,data:{challengeId:"test-challenge"}}}));
  await page.route("**/api/v1/public/checkout/verify",route=>route.fulfill({json:{success:true,data:{guestCheckoutToken:"x".repeat(43)}}}));
  let body: unknown;
  await page.route("**/api/v1/public/orders", async (route) => {
    body = route.request().postDataJSON();
    await route.fulfill({
      json: { success: true, data: [{ ...order, status: "PENDING" }] },
    });
  });
  await page.goto("/merch/m");
  await page.getByRole("button", { name: "Đặt hàng", exact: true }).click();
  await page.getByPlaceholder("Nhập tên người nhận").fill("Guest");
  await page.getByPlaceholder("Nhập số điện thoại liên lạc").fill("0900000000");
  await page
    .getByPlaceholder("Để nhận thông báo cập nhật đơn")
    .fill("guest@uit.edu.vn");
  await expect(page.getByRole("button",{name:"Xác nhận đặt hàng",exact:true})).toBeDisabled();
  await page.getByRole("button",{name:"Gửi mã xác minh email"}).click();
  await page.getByRole("textbox",{name:"Mã xác minh 6 số"}).fill("123456");
  await page.getByRole("button",{name:"Xác minh",exact:true}).click();
  await page
    .getByRole("button", { name: "Xác nhận đặt hàng", exact: true })
    .click();
  await expect(
    page.getByRole("link", { name: "Tra cứu đơn và nhận hàng" }),
  ).toHaveAttribute("href", "/guest-orders?orderId=order");
  expect(body).toMatchObject({
    guestEmail: "guest@uit.edu.vn",
    items: [{ merchId: "m", quantity: 1 }],
  });
});
test("ordinary cart checkout still submits account shipping details", async ({
  page,
}) => {
  await setup(page, "CUSTOMER");
  const product = {
    id: "m",
    orgId: "o",
    name: "Shirt",
    stock: 8,
    price: 100000,
    status: "PUBLISHED",
    images: [],
  };
  await page.route("**/api/v1/customer/cart", (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          id: "cart",
          userId: "a",
          items: [
            { id: "item", merch: product, quantity: 1, subtotal: 100000 },
          ],
          totalAmount: 100000,
        },
      },
    }),
  );
  await page.route("**/api/v1/public/merch/m/purchase-context", (route) =>
    route.fulfill({
      json: { success: true, data: { reservationRequired: false } },
    }),
  );
  let body: unknown;
  await page.route("**/api/v1/customer/cart/checkout", async (route) => {
    body = route.request().postDataJSON();
    await route.fulfill({
      json: { success: true, data: [{ ...order, status: "PENDING" }] },
    });
  });
  await page.goto("/cart");
  await page.getByRole("button", { name: "Đặt hàng", exact: true }).click();
  await page
    .getByRole("button", { name: "Xác nhận đặt hàng", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Đặt hàng thành công!" }),
  ).toBeVisible();
  expect(body).toMatchObject({
    shippingName: user.fullName,
    shippingPhone: "0900000000",
  });
});
test("failed purchase context never enables ordinary checkout", async ({
  page,
}) => {
  await setup(page);
  await page.route("**/api/v1/public/merch/m", (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          id: "m",
          orgId: "o",
          name: "Shirt",
          stock: 8,
          price: 100000,
          status: "PUBLISHED",
          images: [],
        },
      },
    }),
  );
  await page.route("**/api/v1/public/merch/m/purchase-context", (route) =>
    route.fulfill({
      status: 500,
      json: { success: false, message: "Offline" },
    }),
  );
  await page.goto("/merch/m");
  await expect(page.getByRole("alert")).toContainText("Offline");
  await expect(
    page.getByRole("button", { name: "Đặt hàng", exact: true }),
  ).toHaveCount(0);
});
test("camera denial preserves manual pickup entry", async ({ page }) => {
  await setup(page, "ORGANIZER");
  await page.addInitScript(() => {
    Object.defineProperty(navigator.mediaDevices, "getUserMedia", {
      value: () =>
        Promise.reject(
          new DOMException("Permission denied", "NotAllowedError"),
        ),
    });
  });
  await page.goto("/organizer?orgId=o&tab=scanner");
  await page.getByRole("button", { name: "Mở camera" }).click();
  await expect(page.getByRole("alert")).toContainText("Hãy nhập mã thủ công");
  await expect(page.getByLabel("Mã nhận hàng", { exact: true })).toBeEnabled();
});
test("customer feature settings fit a mobile viewport", async ({ page }) => {
  await setup(page, "CUSTOMER");
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/following");
  await expect(
    page.getByRole("checkbox", { name: "Gửi thêm qua email", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: "test-results/following-mobile.png",
    fullPage: true,
  });
});

test("account product checkout links to the private order and uses the server total", async ({
  page,
}) => {
  await setup(page, "CUSTOMER");
  await page.route("**/api/v1/public/merch/m", (r) =>
    r.fulfill({
      json: {
        success: true,
        data: {
          id: "m",
          orgId: "o",
          name: "Shirt",
          stock: 8,
          price: 100000,
          status: "PUBLISHED",
          images: [],
        },
      },
    }),
  );
  await page.route("**/api/v1/public/merch/m/purchase-context", (r) =>
    r.fulfill({
      json: { success: true, data: { reservationRequired: false } },
    }),
  );
  await page.route("**/api/v1/public/orders", (r) =>
    r.fulfill({
      json: {
        success: true,
        data: [
          { ...order, userId: "a", totalAmount: 90000, status: "PENDING" },
        ],
      },
    }),
  );
  await page.goto("/merch/m");
  await page.getByRole("button", { name: "Đặt hàng", exact: true }).click();
  await page
    .getByRole("button", { name: "Xác nhận đặt hàng", exact: true })
    .click();
  await expect(
    page.getByRole("link", { name: "Xem đơn hàng", exact: true }),
  ).toHaveAttribute("href", "/orders/order");
  await expect(page.getByText(/^90[.,]000\s*₫$/)).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Tra cứu đơn và nhận hàng" }),
  ).toHaveCount(0);
});

test("mobile notification panel fits the viewport and respects the backend read flag", async ({
  page,
}) => {
  await setup(page, "CUSTOMER");
  await page.setViewportSize({ width: 375, height: 812 });
  let writes = 0;
  await page.route("**/api/v1/customer/notifications*", async (r) => {
    if (r.request().method() !== "GET") writes++;
    await r.fulfill({
      json: {
        success: true,
        data: pageData([
          {
            id: "n",
            title: "Already read",
            message: "Notice",
            read: true,
            createdAt: "2026-10-01T00:00:00Z",
          },
        ]),
      },
    });
  });
  await page.goto("/following");
  await page.getByRole("button", { name: "Thông báo", exact: true }).click();
  const notice = page.getByRole("button", { name: /Already read/ });
  await expect(notice).toBeVisible();
  const box = await notice.locator("xpath=ancestor::section").boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(375);
  await notice.click();
  expect(writes).toBe(0);
});
