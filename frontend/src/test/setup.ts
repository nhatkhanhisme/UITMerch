import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/react";
// Node 25+ exposes a native storage getter; tests must use jsdom browser storage.
vi.stubGlobal("localStorage", window.localStorage);
vi.stubGlobal("sessionStorage", window.sessionStorage);
afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
});
