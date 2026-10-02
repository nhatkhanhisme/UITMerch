import { expect, it } from "vitest";
import {
  percentage,
  normalizeNotification,
  collectPages,
  normalizePage,
  campusDate,
  featureError,
} from "../lib/featureUtils";
import { AxiosError } from "axios";
it("reads legacy envelopes and Spring Page envelopes", () => {
  expect(
    normalizePage({ success: true, message: "", data: [1] }).items,
  ).toEqual([1]);
  expect(
    normalizePage({
      success: true,
      message: "",
      data: {
        content: [2],
        number: 1,
        size: 20,
        totalElements: 22,
        totalPages: 2,
        last: true,
        first: false,
      },
    }).meta?.hasPrevious,
  ).toBe(true);
});
it("checks every page before deciding a subscription does not exist", async () => {
  const pages: number[] = [];
  const items = await collectPages(async (page) => {
    pages.push(page);
    return {
      content: [page],
      number: page,
      size: 1,
      totalElements: 3,
      totalPages: 3,
      last: page === 2,
      first: page === 0,
    };
  });
  expect(pages).toEqual([0, 1, 2]);
  expect(items).toEqual([0, 1, 2]);
});
it("fails closed for a failed later page", async () => {
  await expect(
    collectPages(async (page) => {
      if (page === 1) throw new Error("Network");
      return {
        content: [1],
        number: 0,
        size: 1,
        totalElements: 2,
        totalPages: 2,
        last: false,
        first: true,
      };
    }),
  ).rejects.toThrow("Network");
});
it("bounds unexpectedly large lists", async () => {
  await expect(
    collectPages(
      async () => ({
        content: [],
        number: 0,
        size: 1,
        totalElements: 10000,
        totalPages: 10000,
        last: false,
        first: true,
      }),
      2,
    ),
  ).rejects.toThrow("Danh sách quá lớn");
});
it("uses campus dates at the UTC midnight boundary", () => {
  expect(campusDate(new Date("2026-10-01T18:00:00Z"))).toBe("2026-10-02");
});
it("shows Retry-After for rate-limited responses", () => {
  const e = new AxiosError("limit");
  e.response = { status: 429, headers: { "retry-after": "60" } } as never;
  expect(featureError(e)).toContain("60 giây");
});

it("normalizes backend read flags and accepts the legacy isRead spelling", () => {
  const n = { id: "n", userId: "a", title: "", message: "", type: "" };
  expect(normalizeNotification({ ...n, read: true }).isRead).toBe(true);
  expect(
    normalizeNotification({ ...n, isRead: false, read: true }).isRead,
  ).toBe(false);
  expect(normalizeNotification(n).isRead).toBe(false);
});

it("converts analytics ratios into percentages",()=>{expect(percentage(0.25).replace(/\s/g,"")).toBe("25%");expect(percentage(0).replace(/\s/g,"")).toBe("0%");});
