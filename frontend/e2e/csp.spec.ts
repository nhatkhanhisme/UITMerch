import { test, expect } from '@playwright/test';

test('production CSP blocks injected scripts and unapproved outbound connections', async ({ page }) => {
  test.skip(!test.info().config.configFile?.endsWith('playwright.csp.config.ts'), 'Production CSP check');
  await page.route('**/api/v1/**', route => route.fulfill({ json: { success: true, data: [] } }));
  const response = await page.goto('/auth');
  expect(response?.headers()['content-security-policy']).toContain("script-src 'self'");
  await expect(page.getByRole('heading', { name: /Truy cập/ })).toBeVisible();
  const result = await page.evaluate(async () => {
    const node = document.createElement('script');
    node.textContent = 'document.documentElement.dataset.injected = "yes"';
    document.head.append(node);
    let blocked = false;
    try { await fetch('https://unapproved.example.invalid/exfiltrate'); } catch { blocked = true; }
    return { injected: document.documentElement.dataset.injected, blocked };
  });
  expect(result).toEqual({ injected: undefined, blocked: true });
});
