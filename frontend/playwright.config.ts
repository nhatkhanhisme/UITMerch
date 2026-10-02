import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report" }],
  ],
  use: {
    baseURL: "http://127.0.0.1:5189",
    trace: "retain-on-failure",
    ...devices["Desktop Chrome"],
    launchOptions: {
      executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE,
    },
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 5189 --strictPort",
    url: "http://127.0.0.1:5189",
    reuseExistingServer: !process.env.CI,
    env: { VITE_API_BASE_URL: "http://127.0.0.1:5189" },
  },
});
