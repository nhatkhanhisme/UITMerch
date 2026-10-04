import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PickupScheduleOrders } from "../features/PickupScheduleOrders";
import { useAuthStore } from "../stores/authStore";
import * as orders from "../api/order";
import type { ApiResponse, OrderResponse } from "../types/shared";

vi.mock("../api/order");
const ready = { id: "12345678-first", status: "READY", guestName: "First customer" } as OrderResponse;
const last = { id: "87654321-last", status: "READY", guestName: "Last customer" } as OrderResponse;
function response(data: OrderResponse[], page = 0): ApiResponse<OrderResponse[]> {
  return { message: "OK", data, meta: {
    page, pageSize: 20, totalElements: 21, totalPages: 2, hasNext: page === 0, hasPrevious: page > 0,
  } };
}
function mount() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}><PickupScheduleOrders orgId="org" scheduleId="schedule" /></QueryClientProvider>);
}
beforeEach(() => {
  vi.resetAllMocks();
  useAuthStore.setState({ user: { id: "owner", email: "owner@uit.edu.vn", fullName: "Owner", role: "ORGANIZER", isVerified: true } });
});

it("loads only the requested page and lets the organizer reach the last order", async () => {
  vi.mocked(orders.getPickupScheduleOrders).mockImplementation(async (_org, _schedule, params) =>
    response(params?.page === 1 ? [last] : [ready], params?.page));
  mount();
  await screen.findByText("First customer");
  expect(orders.getPickupScheduleOrders).toHaveBeenCalledWith("org", "schedule", { page: 0, size: 20 }, expect.any(AbortSignal));
  fireEvent.click(screen.getByRole("button", { name: "Trang sau" }));
  await screen.findByText("Last customer");
  expect(screen.queryByText("First customer")).toBeNull();
  expect(screen.getByText("Trang 2 / 2 · 21 đơn")).toBeTruthy();
  expect((screen.getByRole("button", { name: "Trang sau" }) as HTMLButtonElement).disabled).toBe(true);
});

it("shows loading feedback and does not allow a check-in before data is ready", () => {
  vi.mocked(orders.getPickupScheduleOrders).mockImplementation(() => new Promise(() => {}));
  mount();
  expect(screen.getByRole("status").textContent).toContain("Đang tải");
  expect(screen.queryByRole("button", { name: /Check-in đơn/ })).toBeNull();
});

it("shows a retry action when loading fails", async () => {
  vi.mocked(orders.getPickupScheduleOrders).mockRejectedValueOnce(new Error("offline")).mockResolvedValue(response([ready]));
  mount();
  await screen.findByRole("alert");
  fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));
  await screen.findByText("First customer");
});

it("updates a successful check-in without dropping pagination metadata", async () => {
  let current = ready;
  vi.mocked(orders.getPickupScheduleOrders).mockImplementation(async () => response([current]));
  vi.mocked(orders.checkInOrder).mockImplementation(async () => {
    current = { ...ready, status: "COMPLETED" };
    return { message: "OK", data: current };
  });
  mount();
  const button = await screen.findByRole("button", { name: "Check-in đơn 12345678" });
  fireEvent.click(button);
  await screen.findByText("✓ Đã nhận");
  await waitFor(() => expect(orders.checkInOrder).toHaveBeenCalledWith("org", ready.id));
  expect(screen.getByText("Trang 1 / 2 · 21 đơn")).toBeTruthy();
});

it("does not offer check-in for cancelled or already completed orders", async () => {
  vi.mocked(orders.getPickupScheduleOrders).mockResolvedValue(response([
    { ...ready, status: "CANCELLED" }, { ...last, status: "COMPLETED" },
  ]));
  mount();
  await screen.findByText("✓ Đã nhận");
  expect(screen.queryByRole("button", { name: /Check-in đơn/ })).toBeNull();
  expect(screen.getByText("Chưa sẵn sàng nhận")).toBeTruthy();
});
