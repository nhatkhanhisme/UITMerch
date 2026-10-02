import { apiClient } from "./client";
import type { ApiResponse } from "../types/shared";
import type { SpringPage, Follow } from "../types/features";
const path = "/api/v1/customer/following";
export type FollowPreferences = Pick<
  Follow,
  "notifyMerch" | "notifyEvents" | "emailEnabled"
>;
export async function listFollowing(page = 0) {
  return (
    await apiClient.get<ApiResponse<SpringPage<Follow>>>(path, {
      params: { page, size: 20 },
    })
  ).data.data;
}
export async function followOrganization(
  orgId: string,
  preferences: FollowPreferences,
) {
  return (
    await apiClient.post<ApiResponse<Follow>>(`${path}/${orgId}`, preferences)
  ).data.data;
}
export async function updateFollowing(
  orgId: string,
  preferences: Partial<FollowPreferences>,
) {
  return (
    await apiClient.patch<ApiResponse<Follow>>(`${path}/${orgId}`, preferences)
  ).data.data;
}
export async function unfollowOrganization(orgId: string) {
  await apiClient.delete(`${path}/${orgId}`);
}
