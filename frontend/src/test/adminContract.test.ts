import { expect, it } from "vitest";
import { normalizeUserSummary } from "../api/admin";
const user = {
  id: "demo",
  fullName: "Demo",
  email: "demo@example.test",
  role: "CUSTOMER",
};
it("uses Java active/verified booleans rather than showing every account disabled", () => {
  expect(
    normalizeUserSummary({ ...user, active: true, verified: true }),
  ).toMatchObject({ isActive: true, isVerified: true });
  expect(
    normalizeUserSummary({ ...user, active: false, verified: false }),
  ).toMatchObject({ isActive: false, isVerified: false });
});
it("supports explicit is-prefixed booleans without overriding false values", () => {
  expect(
    normalizeUserSummary({
      ...user,
      isActive: false,
      active: true,
      isVerified: false,
      verified: true,
    }),
  ).toMatchObject({ isActive: false, isVerified: false });
});
