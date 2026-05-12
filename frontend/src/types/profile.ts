import type { UserRole } from "./auth";

export type CustomerProfile = {
  id: string;
  email: string;
  fullName: string;
  phone?: string | null;
  address?: string | null;
  avatarUrl?: string | null;
  role: UserRole;
  createdAt: string;
};

export type CustomerProfileUpdate = {
  fullName?: string;
  phone?: string;
  address?: string;
  avatarUrl?: string;
};

export type OrganizationStatus = "PENDING" | "ACTIVE" | "INACTIVE";

export type OrganizerProfile = {
  id: string;
  ownerId: string;
  name: string;
  description?: string | null;
  logoUrl?: string | null;
  coverUrl?: string | null;
  status: OrganizationStatus;
  createdAt: string;
  updatedAt: string;
};

export type OrganizerProfileUpdate = {
  name?: string;
  description?: string;
  logoUrl?: string;
  coverUrl?: string;
};
