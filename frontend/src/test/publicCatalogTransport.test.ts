import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { apiClient, publicClient, authTransport, registerAuthBridge } from "../api/client";
import { getPublicMerchList, getPopularMerch, getPublicMerchDetail, getCategories } from "../api/merch";
import { getPublicOrganizations, getPublicOrganizationDetail, getPublicOrgMerch, getPublicOrgEvents } from "../api/organization";
import { getPublicEvents, getPublicEvent } from "../api/event";
import { listCampaigns, getCampaign } from "../api/campaigns";
import { createGuestCheckoutOrder, getPickupScheduleOrders } from "../api/order";

function reply(config: InternalAxiosRequestConfig) {
  return { config, status: 200, statusText: "OK", headers: {}, data: { message: "OK", data: [] } };
}
beforeEach(() => {
  registerAuthBridge({
    read: () => ({ accessToken: "access", refreshToken: "refresh", tokenType: "Bearer", user: {
      id: "customer", email: "customer@uit.edu.vn", fullName: "Customer", role: "CUSTOMER", isVerified: true,
    } }),
    write: vi.fn(), clear: vi.fn(), sync: vi.fn(async () => {}),
  });
  publicClient.defaults.headers.common.Authorization = "Bearer inherited-stale";
});
afterEach(() => {
  delete publicClient.defaults.adapter;
  delete apiClient.defaults.adapter;
  delete publicClient.defaults.headers.common.Authorization;
  registerAuthBridge({ read: () => null, write: vi.fn(), clear: vi.fn(), sync: vi.fn(async () => {}) });
  vi.restoreAllMocks();
});

describe("anonymous catalog transport", () => {
  it.each([
    ["merch list", () => getPublicMerchList()],
    ["popular merch", () => getPopularMerch()],
    ["merch detail", () => getPublicMerchDetail("merch")],
    ["categories", () => getCategories()],
    ["organizations", () => getPublicOrganizations()],
    ["organization detail", () => getPublicOrganizationDetail("org")],
    ["organization merch", () => getPublicOrgMerch("org")],
    ["organization events", () => getPublicOrgEvents("org")],
    ["events", () => getPublicEvents()],
    ["event detail", () => getPublicEvent("event")],
    ["campaigns", () => listCampaigns()],
    ["campaign detail", () => getCampaign("campaign")],
  ] as const)("does not send account credentials for %s", async (_name, request) => {
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) => {
      expect(config.headers.get("Authorization")).toBeUndefined();
      expect(config.withCredentials).toBe(false);
      return reply(config);
    });
    publicClient.defaults.adapter = adapter;
    await request();
    expect(adapter).toHaveBeenCalledOnce();
  });

  it("does not rotate the session when an anonymous catalog request fails", async () => {
    const refresh = vi.spyOn(authTransport, "post");
    publicClient.defaults.adapter = async config => {
      throw new AxiosError("Rejected", "ERR_BAD_RESPONSE", config, undefined, { ...reply(config), status: 401 });
    };
    await expect(getCategories()).rejects.toBeInstanceOf(AxiosError);
    expect(refresh).not.toHaveBeenCalled();
  });

  it("keeps account credentials for checkout and organization campaign management", async () => {
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) => {
      expect(config.headers.get("Authorization")).toBe("Bearer access");
      return reply(config);
    });
    apiClient.defaults.adapter = adapter;
    await createGuestCheckoutOrder({ guestName: "Customer", guestPhone: "0901234567", items: [{ merchId: "merch", quantity: 1 }] });
    await listCampaigns(0, "org");
    await getCampaign("campaign", "org");
    expect(adapter).toHaveBeenCalledTimes(3);
  });

  it("uses the authenticated paginated check-in contract", async () => {
    apiClient.defaults.adapter = async config => {
      expect(config.headers.get("Authorization")).toBe("Bearer access");
      expect(config.url).toBe("/api/v1/organizations/org/pickup-schedules/schedule/orders/page");
      expect(config.params).toEqual({ page: 2, size: 20 });
      return reply(config);
    };
    await getPickupScheduleOrders("org", "schedule", { page: 2, size: 20 });
  });
});
