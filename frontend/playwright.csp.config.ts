import { defineConfig } from '@playwright/test';
import base from './playwright.config';
export default defineConfig({
  ...base,
  webServer: {
    command: 'node scripts/csp-preview.mjs',
    url: 'http://127.0.0.1:5189',
    reuseExistingServer: false,
  },
});
