import { test, expect, type Page } from "@playwright/test";

async function publicFixtures(page: Page) {
  await page.route("**/api/v1/**", (r) => {
    const path = new URL(r.request().url()).pathname;
    return r.fulfill({
      json: {
        success: true,
        message: "OK",
        data:
          path === "/api/v1/public/campaigns"
            ? {
                content: [],
                number: 0,
                size: 20,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
              }
            : [],
        meta: { page: 0, totalPages: 0, totalElements: 0, hasNext: false },
      },
    });
  });
}

for (const width of [1440, 375, 320]) {
  test(`homepage promotes preorder outside the navbar at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await publicFixtures(page);
    await page.goto("/");
    const primary = page.getByRole("navigation", {
      name: "Điều hướng chính",
      exact: true,
    });
    await expect(
      primary.getByRole("link", { name: "Đặt trước", exact: true }),
    ).toHaveCount(0);
    if (width < 1280) {
      await page.getByRole("button", { name: "Mở điều hướng" }).click();
      await expect(
        page
          .getByRole("navigation", { name: "Điều hướng thu gọn", exact: true })
          .getByRole("link", { name: "Đặt trước", exact: true }),
      ).toHaveCount(0);
      await page.keyboard.press("Escape");
    }
    const spotlight = page.getByRole("region", {
      name: "Bộ sưu tập mở đặt trước",
    });
    await expect(
      spotlight.getByRole("heading", { name: "Bộ sưu tập mở đặt trước" }),
    ).toBeVisible();
    const link = spotlight.getByRole("link", {
      name: "Khám phá bộ sưu tập",
    });
    const box = await link.boundingBox();
    const nav = await primary.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(width);
    expect(box!.y).toBeGreaterThanOrEqual(nav!.y + nav!.height);
    if (width === 1440 || width === 375)
      await page.screenshot({
        path: `test-results/preorder-home-${width}.png`,
      });
    await link.click();
    await expect(page).toHaveURL(/\/campaigns$/);
    await expect(
      page.getByRole("heading", { name: "Bộ sưu tập mở đặt trước", exact: true }),
    ).toBeVisible();
  });
}

for (const width of [1440, 375]) {
  test(`catalog keeps a dedicated preorder entry at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    await publicFixtures(page);
    await page.goto("/merch");
    const spotlight = page.getByRole("region", {
      name: "Bộ sưu tập mở đặt trước",
    });
    await expect(spotlight).toBeVisible();
    const box = await spotlight.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(width);
    await spotlight
      .getByRole("link", { name: "Khám phá bộ sưu tập" })
      .click();
    await expect(page).toHaveURL(/\/campaigns$/);
  });
}

for (const width of [1440, 375]) {
  test(`short screens can scroll to preorder before leaving the hero at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 640 });
    await publicFixtures(page);
    await page.goto("/");
    const link = page.getByRole("link", { name: "Khám phá bộ sưu tập" });
    await link.waitFor();
    await page.mouse.move(width / 2, 350);
    await page.mouse.wheel(0, 200);
    await expect
      .poll(async () => {
        const box = await link.boundingBox();
        return !!box && box.y >= 80 && box.y + box.height <= 640;
      })
      .toBe(true);
    await expect(page).not.toHaveURL(/#home-item$/);
    await link.click();
    await expect(page).toHaveURL(/\/campaigns$/);
  });
}

test("homepage still snaps between sections when the current section fits", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 1024 });
  await publicFixtures(page);
  await page.goto("/");
  await page.getByRole("region", { name: "Bộ sưu tập mở đặt trước" }).waitFor();
  await page.mouse.move(720, 400);
  await page.mouse.wheel(0, 200);
  await expect(page).toHaveURL(/#home-item$/);
});
