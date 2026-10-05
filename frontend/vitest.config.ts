import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    execArgv: Number(process.versions.node.split(".")[0]) >= 25 ? ["--no-experimental-webstorage"] : [],
    include: ["src/**/*.test.{ts,tsx}"],
    restoreMocks: true,
    setupFiles: ["src/test/setup.ts"],
  },
});
