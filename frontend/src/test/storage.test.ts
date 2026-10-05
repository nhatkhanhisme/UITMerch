import { afterEach, expect, it, vi } from "vitest";
import { uploadAvatarImage, uploadOrganizerImage } from "../api/storage";
import type { InternalAxiosRequestConfig } from "axios";
import { apiClient } from "../api/client";
import { useAuthStore } from "../stores/authStore";
afterEach(() => { delete apiClient.defaults.adapter; useAuthStore.getState().clearSession(); });
function signedIn() {
  useAuthStore.getState().setSession({ accessToken:"access", tokenType:"Bearer", user:{ id:"owner",email:"owner@example.test",fullName:"Owner",role:"ORGANIZER",isVerified:true } });
}
it("uploads through the authenticated backend without anonymous Supabase credentials", async () => {
  signedIn();
  const request=vi.fn(async (config: InternalAxiosRequestConfig) => ({ config, status:200,statusText:"OK",headers:{},data:{data:{fileUrl:"https://storage.example.test/photo.png"}} }));
  apiClient.defaults.adapter=request;
  const file=new File(["image"],"photo.png",{type:"image/png"});
  expect(await uploadAvatarImage(file,"owner")).toBe("https://storage.example.test/photo.png");
  expect(request.mock.calls[0][0].url).toBe("/api/v1/uploads/avatar");
  expect(request.mock.calls[0][0].headers.get("Authorization")).toBe("Bearer access");
  await uploadOrganizerImage(file,"org","cover");
  expect(request.mock.calls[1][0].url).toBe("/api/v1/uploads/organizations/org/cover");
});
it("rejects foreign avatar targets, SVG and oversized images before sending", async () => {
  signedIn();const request=vi.fn();apiClient.defaults.adapter=request;
  await expect(uploadAvatarImage(new File(["image"],"photo.png",{type:"image/png"}),"foreign")).rejects.toThrow("tài khoản của mình");
  await expect(uploadAvatarImage(new File(["<svg/>"],"photo.svg",{type:"image/svg+xml"}),"owner")).rejects.toThrow("SVG");
  await expect(uploadAvatarImage(new File([new Uint8Array(10*1024*1024+1)],"photo.png",{type:"image/png"}),"owner")).rejects.toThrow("10MB");
  expect(request).not.toHaveBeenCalled();
});
