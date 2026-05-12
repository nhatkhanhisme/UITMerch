export type UserRole = "CUSTOMER" | "ORGANIZER" | "ADMIN";

export type AuthUser = {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  isVerified: boolean;
};

export type AuthSession = {
  user: AuthUser;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
};
