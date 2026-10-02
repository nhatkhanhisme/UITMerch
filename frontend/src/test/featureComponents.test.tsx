import { beforeEach, expect, it, vi } from "vitest";
import {
  act,
  render,
  screen,
  waitFor,
  fireEvent,
} from "@testing-library/react";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useAuthStore } from "../stores/authStore";
import { RestockButton, FollowingPage } from "../features/Subscriptions";
import { CampaignDetailPage } from "../features/Campaigns";
import { PickupScanner, GuestOrdersPage } from "../features/Pickup";
import {
  NotificationBell,
  notificationTarget,
} from "../features/NotificationBell";
import * as restock from "../api/restock";
import * as following from "../api/following";
import * as campaign from "../api/campaigns";
import * as pickup from "../api/pickup";
import { apiClient } from "../api/client";
vi.mock("../api/restock");
vi.mock("../api/following");
vi.mock("../api/campaigns");
vi.mock("../api/pickup");
vi.mock("../hooks/useNotificationStream", () => ({
  useNotificationStream: vi.fn(),
}));
const customer = {
  id: "a",
  email: "a@uit.edu.vn",
  fullName: "A",
  role: "CUSTOMER" as const,
  isVerified: true,
};
const page = <T,>(content: T[]) => ({
  content,
  number: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  last: true,
  first: true,
});
function mount(element: React.ReactNode, path = "/") {
  const q = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={q}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="*" element={element} />
          <Route path="/campaigns/:id" element={element} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.resetAllMocks();
  useAuthStore.setState({
    user: customer,
    accessToken: "test",
    refreshToken: "test",
  });
});
it("offers login instead of calling restock APIs for anonymous visitors", () => {
  useAuthStore.setState({ user: null });
  mount(<RestockButton merchId="m" />);
  expect(screen.getByRole("link").getAttribute("href")).toBe("/auth");
  expect(restock.listRestock).not.toHaveBeenCalled();
});
it("does not allow subscription changes when subscription loading fails", async () => {
  vi.mocked(restock.listRestock).mockRejectedValue(new Error("offline"));
  mount(<RestockButton merchId="m" />);
  await screen.findByRole("alert");
  expect(
    (
      screen.getByRole("button", {
        name: "Báo tôi khi có hàng",
      }) as HTMLButtonElement
    ).disabled,
  ).toBe(true);
});
it("reads a subscription on a later page and unsubscribes it", async () => {
  vi.mocked(restock.listRestock).mockImplementation(async (p) => ({
    ...page(
      p === 1
        ? [
            {
              merchId: "m",
              emailEnabled: true,
              subscribedAt: "2026-10-01T00:00:00Z",
            },
          ]
        : [],
    ),
    last: p === 1,
    totalPages: 2,
  }));
  vi.mocked(restock.unsubscribeRestock).mockResolvedValue();
  mount(<RestockButton merchId="m" />);
  const button = await screen.findByRole("button", { name: "Huỷ báo có hàng" });
  fireEvent.click(button);
  await waitFor(() =>
    expect(restock.unsubscribeRestock).toHaveBeenCalledWith("m"),
  );
});
it("updates only the changed follow preference", async () => {
  vi.mocked(following.listFollowing).mockResolvedValue(
    page([
      {
        orgId: "o",
        notifyMerch: true,
        notifyEvents: true,
        emailEnabled: false,
        followedAt: "2026-10-01",
      },
    ]) as never,
  );
  vi.mocked(following.updateFollowing).mockResolvedValue({} as never);
  mount(<FollowingPage />);
  fireEvent.click(await screen.findByRole("checkbox", { name: "Email" }));
  await waitFor(() =>
    expect(following.updateFollowing).toHaveBeenCalledWith("o", {
      emailEnabled: true,
    }),
  );
});
it("freezes and replays the original reservation after a lost response", async () => {
  vi.mocked(campaign.getCampaign).mockResolvedValue({
    id: "c",
    orgId: "o",
    title: "Campaign",
    minimumQuantity: 2,
    reservedQuantity: 0,
    state: "ACTIVE",
    deadline: "2099-10-01T00:00:00Z",
    createdAt: "2026-10-01",
    variants: [
      {
        merchId: "m",
        label: "Blue M",
        unitPrice: 1000,
        availableQuantity: 5,
        available: true,
      },
    ],
  });
  vi.mocked(campaign.reserveCampaign).mockRejectedValue(new Error("offline"));
  mount(<CampaignDetailPage />, "/campaigns/c");
  fireEvent.change(await screen.findByRole("combobox"), {
    target: { value: "m" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Giữ chỗ" }));
  await screen.findByRole("alert");
  const original = vi.mocked(campaign.reserveCampaign).mock.calls[0][1];
  expect(original.requestId).toMatch(/[a-f0-9-]{36}/);
  expect((screen.getByRole("combobox") as HTMLSelectElement).disabled).toBe(
    true,
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Thử lại yêu cầu đang chờ" }),
  );
  await waitFor(() =>
    expect(campaign.reserveCampaign).toHaveBeenCalledTimes(2),
  );
  expect(vi.mocked(campaign.reserveCampaign).mock.calls[1][1]).toEqual(
    original,
  );
});
it("requires verification before pickup completion and displays server failure", async () => {
  vi.mocked(pickup.verifyPickup).mockRejectedValue(new Error("Expired"));
  mount(<PickupScanner orgId="o" />);
  expect(
    screen.queryByRole("button", { name: "Xác nhận giao hàng" }),
  ).toBeNull();
  fireEvent.change(screen.getByLabelText("Mã nhận hàng"), {
    target: { value: "token" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Kiểm tra mã" }));
  await screen.findByText("Expired");
  expect(pickup.completePickup).not.toHaveBeenCalled();
});
it("verifies then completes a pickup explicitly", async () => {
  const order = {
    id: "order",
    status: "READY",
    items: [],
    totalAmount: 1000,
    orgId: "o",
  };
  vi.mocked(pickup.verifyPickup).mockResolvedValue(order as never);
  vi.mocked(pickup.completePickup).mockResolvedValue({
    ...order,
    status: "COMPLETED",
  } as never);
  vi.mocked(pickup.getOrderHistory).mockResolvedValue(page([]) as never);
  mount(<PickupScanner orgId="o" />);
  fireEvent.change(screen.getByLabelText("Mã nhận hàng"), {
    target: { value: "token" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Kiểm tra mã" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Xác nhận giao hàng" }),
  );
  await screen.findByText("Đã giao hàng thành công.");
  expect(pickup.completePickup).toHaveBeenCalledWith("o", "token", null);
});
it("keeps guest receipt responses generic and does not store credentials", async () => {
  vi.mocked(pickup.requestGuestReceipt).mockResolvedValue();
  mount(<GuestOrdersPage />);
  fireEvent.change(screen.getByLabelText("ID đơn hàng"), {
    target: { value: "order" },
  });
  fireEvent.change(screen.getByLabelText("Email đặt hàng"), {
    target: { value: "guest@uit.edu.vn" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Gửi hướng dẫn nhận hàng" }),
  );
  await screen.findByText(/Nếu thông tin hợp lệ/);
  expect(localStorage.length).toBe(1);
  expect(sessionStorage.length).toBe(0);
});
it("opening the bell does not automatically mark every notification read", async () => {
  const get = vi.spyOn(apiClient, "get").mockImplementation(
    async (url) =>
      ({
        data: {
          success: true,
          data: String(url).endsWith("unread-count")
            ? { unreadCount: 1 }
            : [
                {
                  id: "n",
                  userId: "a",
                  title: "New",
                  message: "Message",
                  type: "MERCH_RESTOCKED",
                  isRead: false,
                  relatedMerchId: "m",
                },
              ],
        },
      }) as never,
  );
  const patch = vi.spyOn(apiClient, "patch").mockResolvedValue({} as never);
  mount(<NotificationBell />);
  fireEvent.click(screen.getByRole("button", { name: "Thông báo" }));
  await screen.findByText("New");
  expect(patch).not.toHaveBeenCalled();
  get.mockRestore();
  patch.mockRestore();
});
it("notification targets support orders, merchandise, organizations and event routes", () => {
  const n = {
    id: "n",
    userId: "a",
    title: "",
    message: "",
    type: "",
    isRead: false,
  };
  expect(notificationTarget({ ...n, relatedOrderId: "q" }, false)).toBe(
    "/orders/q",
  );
  expect(notificationTarget({ ...n, relatedMerchId: "m" }, false)).toBe(
    "/merch/m",
  );
  expect(notificationTarget({ ...n, relatedEventId: "e" }, false)).toBe(
    "/event/e",
  );
  expect(notificationTarget({ ...n, relatedOrgId: "o" }, false)).toBe(
    "/organization/o",
  );
  expect(notificationTarget(n, false)).toBeUndefined();
});

it("rolls back a follow preference when the server rejects it", async () => {
  vi.mocked(following.listFollowing).mockResolvedValue(
    page([
      {
        orgId: "o",
        notifyMerch: true,
        notifyEvents: true,
        emailEnabled: false,
        followedAt: "2026-10-01",
      },
    ]),
  );
  vi.mocked(following.updateFollowing).mockRejectedValue(new Error("Rejected"));
  mount(<FollowingPage />);
  const checkbox = await screen.findByRole("checkbox", { name: "Email" });
  fireEvent.click(checkbox);
  await screen.findByText("Rejected");
  await waitFor(() =>
    expect((checkbox as HTMLInputElement).checked).toBe(false),
  );
});

it("does not show a previous account's reservation response after account switch", async () => {
  vi.mocked(campaign.getCampaign).mockResolvedValue({
    id: "c",
    orgId: "o",
    title: "Campaign",
    minimumQuantity: 2,
    reservedQuantity: 0,
    state: "ACTIVE",
    deadline: "2099-10-01T00:00:00Z",
    createdAt: "2026-10-01",
    variants: [
      {
        merchId: "m",
        label: "Blue M",
        unitPrice: 1000,
        availableQuantity: 5,
        available: true,
      },
    ],
  });
  let resolve: (
    value: Awaited<ReturnType<typeof campaign.reserveCampaign>>,
  ) => void = () => {};
  vi.mocked(campaign.reserveCampaign).mockImplementation(
    () =>
      new Promise((r) => {
        resolve = r;
      }),
  );
  mount(<CampaignDetailPage />, "/campaigns/c");
  fireEvent.change(await screen.findByRole("combobox"), {
    target: { value: "m" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Giữ chỗ" }));
  await waitFor(() => expect(campaign.reserveCampaign).toHaveBeenCalledOnce());
  act(() => useAuthStore.setState({ user: { ...customer, id: "b" } }));
  await act(async () => {
    resolve({
      reservation: { id: "private-a" },
      order: { id: "private-order-a" },
    } as never);
  });
  expect(screen.queryByRole("link", { name: "Xem đơn hàng" })).toBeNull();
});
