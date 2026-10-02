import { test, expect, type Page } from "@playwright/test";

async function setupNav(page: Page, role?: "CUSTOMER" | "ORGANIZER" | "ADMIN") {
  if (role)
    await page.addInitScript((role) => {
      const user = {
        id: "nav-user",
        role,
        email: "nav@uit.edu.vn",
        fullName: "Nguyễn Thị Khánh Linh tên dài để kiểm tra điều hướng",
        isVerified: true,
        avatarUrl: "/assets/figma/account-icon.svg",
      };
      localStorage.setItem(
        "uitmerch-auth",
        JSON.stringify({
          state: {
            user,
            accessToken: "test-token",
            refreshToken: "test-refresh",
            tokenType: "Bearer",
          },
          version: 0,
        }),
      );
    }, role);
  await page.route("**/api/v1/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/stream"))
      return route.fulfill({ contentType: "text/event-stream", body: "" });
    if (path.startsWith("/api/v1/public/campaigns/"))
      return route.fulfill({
        status: 404,
        json: { success: false, message: "Campaign not found" },
      });
    return route.fulfill({
      json: {
        success: true,
        message: "OK",
        data: path.endsWith("unread-count")
          ? { unreadCount: 2 }
          : path === "/api/v1/public/campaigns"
            ? {
                content: [],
                number: 0,
                size: 20,
                totalPages: 0,
                totalElements: 0,
                first: true,
                last: true,
              }
            : [],
      },
    });
  });
  await page.goto("/campaigns");
}

for (const width of [1440, 1280, 1024, 768, 375, 320]) {
  test(`navigation fits at ${width}px without shrinking its controls`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await setupNav(page, "CUSTOMER");
    const nav = page.getByRole("navigation", {
      name: "Điều hướng chính",
      exact: true,
    });
    await expect(nav).toBeVisible();
    const bounds = await nav.boundingBox();
    expect(bounds!.x).toBeGreaterThanOrEqual(0);
    expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width);
    const logo = await nav
      .getByRole("link", { name: "UITMerch — về trang chủ" })
      .boundingBox();
    expect(logo!.y).toBeGreaterThanOrEqual(bounds!.y);
    expect(logo!.y + logo!.height).toBeLessThanOrEqual(
      bounds!.y + bounds!.height,
    );
    const controls = nav.getByRole("button");
    for (const button of await controls.all()) {
      if (!(await button.isVisible())) continue;
      const box = await button.boundingBox();
      expect(box!.height).toBeGreaterThanOrEqual(44);
      expect(box!.x).toBeGreaterThanOrEqual(logo!.x + logo!.width);
      expect(box!.x + box!.width).toBeLessThanOrEqual(
        bounds!.x + bounds!.width,
      );
    }
    if (width >= 1280) {
      const first = await nav
        .getByRole("link", { name: "Trang chủ", exact: true })
        .boundingBox();
      expect(first!.x).toBeGreaterThan(logo!.x + logo!.width);
      await expect(
        nav.getByRole("link", { name: "Tra cứu đơn khách" }),
      ).toHaveCount(0);
    } else {
      await nav.getByRole("button", { name: "Mở điều hướng" }).click();
      const compact = page.getByRole("navigation", {
        name: "Điều hướng thu gọn",
        exact: true,
      });
      await expect(
        compact.getByRole("link", { name: "Vật phẩm", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: "Tra cứu đơn khách", exact: true }),
      ).toBeVisible();
      await page.keyboard.press("Escape");
      await expect(
        nav.getByRole("button", { name: "Mở điều hướng" }),
      ).toBeFocused();
      await nav.getByRole("button", { name: "Mở điều hướng" }).click();
      await compact
        .getByRole("link", { name: "Vật phẩm", exact: true })
        .click();
      await expect(page).toHaveURL(/\/merch$/);
      await expect(
        nav.getByRole("button", { name: "Mở điều hướng" }),
      ).toHaveAttribute("aria-expanded", "false");
    }
    if (width === 1440 || width === 375)
      await nav.screenshot({ path: `test-results/navbar-${width}.png` });
  });
}

test("guest order lookup remains accessible through the desktop account menu", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await setupNav(page);
  const account = page.getByRole("button", { name: "Tài khoản", exact: true });
  await account.click();
  const secondary = page.getByRole("navigation", {
    name: "Tài khoản và tiện ích",
  });
  await expect(
    secondary.getByRole("link", { name: "Đăng nhập / Đăng ký" }),
  ).toHaveAttribute("href", "/auth");
  await secondary.getByRole("link", { name: "Tra cứu đơn khách" }).click();
  await expect(page).toHaveURL(/\/guest-orders$/);
  await expect(account).toHaveAttribute("aria-expanded", "false");
});

for (const role of ["CUSTOMER", "ORGANIZER", "ADMIN"] as const) {
  test(`account navigation scopes actions to ${role}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await setupNav(page, role);
    const account = page
      .getByRole("navigation", { name: "Điều hướng chính", exact: true })
      .getByRole("button", { name: /Nguyễn Thị Khánh Linh/ });
    await account.click();
    const menu = page.getByRole("navigation", {
      name: "Tài khoản và tiện ích",
    });
    if (role === "CUSTOMER") {
      await expect(
        menu.getByRole("link", { name: "Đặt trước của tôi" }),
      ).toHaveAttribute("href", "/reservations");
      await expect(
        menu.getByRole("link", { name: "Tổ chức đang theo dõi" }),
      ).toHaveAttribute("href", "/following");
      await expect(
        menu.getByRole("link", { name: "Báo khi có hàng" }),
      ).toHaveAttribute("href", "/restock-subscriptions");
      await expect(menu.getByRole("link", { name: "Quản lý BTC" })).toHaveCount(
        0,
      );
    } else {
      await expect(
        menu.getByRole("link", {
          name: role === "ORGANIZER" ? "Quản lý BTC" : "Quản trị",
        }),
      ).toBeVisible();
      await expect(
        menu.getByRole("link", { name: "Đặt trước của tôi" }),
      ).toHaveCount(0);
    }
    await page.keyboard.press("Escape");
    await expect(account).toBeFocused();
    await account.click();
    await page
      .getByRole("heading", { name: "Chiến dịch đặt trước", exact: true })
      .click();
    await expect(account).toHaveAttribute("aria-expanded", "false");
  });
}

test("campaign details do not add a campaign item to primary navigation", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await setupNav(page);
  await page.goto("/campaigns/test");
  const link = page
    .getByRole("navigation", { name: "Điều hướng chính", exact: true })
    .getByRole("link", { name: "Đặt trước", exact: true });
  await expect(link).toHaveCount(0);
});

test("switching breakpoints closes hidden menus", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 900 });
  await setupNav(page, "CUSTOMER");
  await page.getByRole("button", { name: "Mở điều hướng" }).click();
  await page.setViewportSize({ width: 1440, height: 900 });
  const account = page
    .getByRole("navigation", { name: "Điều hướng chính", exact: true })
    .getByRole("button", { name: /Nguyễn Thị Khánh Linh/ });
  await account.click();
  await page.keyboard.press("Escape");
  await expect(account).toBeFocused();
  await page.setViewportSize({ width: 375, height: 900 });
  await expect(
    page.getByRole("button", { name: "Mở điều hướng" }),
  ).toHaveAttribute("aria-expanded", "false");
});

test("large unread counts do not widen mobile navigation", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 900 });
  await setupNav(page, "CUSTOMER");
  await page.route("**/customer/notifications/unread-count", (r) =>
    r.fulfill({ json: { success: true, data: { unreadCount: 1000 } } }),
  );
  await page.reload();
  await expect(page.getByLabel("Chưa đọc")).toHaveText("99+");
  const nav = await page
    .getByRole("navigation", { name: "Điều hướng chính", exact: true })
    .boundingBox();
  const end = await page
    .getByRole("button", { name: "Mở điều hướng" })
    .boundingBox();
  expect(end!.x + end!.width).toBeLessThanOrEqual(nav!.x + nav!.width);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
