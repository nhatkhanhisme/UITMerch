const DEFAULT_API_BASE_URL = "http://localhost:8081";

export const appConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
  useMock: import.meta.env.VITE_USE_MOCK !== "false",
};
