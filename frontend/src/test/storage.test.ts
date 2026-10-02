import { afterEach, expect, it, vi } from "vitest";
afterEach(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});
it("missing optional Storage configuration does not prevent app imports", async () => {
  vi.stubEnv("VITE_SUPABASE_URL", "");
  vi.stubEnv("VITE_SUPABASE_ANON_KEY", "");
  vi.resetModules();
  const { supabase } = await import("../api/supabaseClient");
  expect(supabase).toBeNull();
  const { uploadAvatarImage } = await import("../api/storage");
  await expect(
    uploadAvatarImage(
      new File(["image"], "photo.png", { type: "image/png" }),
      "user",
    ),
  ).rejects.toThrow("Chưa cấu hình Supabase Storage");
});
it("configured Storage uploads preserve the bucket, path and public URL contract", async () => {
  vi.stubEnv("VITE_SUPABASE_URL", "https://test.supabase.co");
  vi.stubEnv("VITE_SUPABASE_ANON_KEY", "public-test-key");
  vi.resetModules();
  const upload = vi.fn(async () => ({ error: null }));
  const from = vi.fn(() => ({
    upload,
    getPublicUrl: () => ({
      data: { publicUrl: "https://test.supabase.co/storage/photo.png" },
    }),
  }));
  vi.doMock("./../api/supabaseClient", () => ({
    supabase: { storage: { from } },
  }));
  const { uploadAvatarImage } = await import("../api/storage");
  const file = new File(["image"], "photo.png", { type: "image/png" });
  expect(await uploadAvatarImage(file, "user")).toBe(
    "https://test.supabase.co/storage/photo.png",
  );
  expect(from).toHaveBeenCalledWith("avatars");
  expect(upload).toHaveBeenCalledWith(
    expect.stringMatching(/^user\/.*\.png$/),
    file,
    { contentType: "image/png", upsert: false },
  );
  vi.doUnmock("./../api/supabaseClient");
});
