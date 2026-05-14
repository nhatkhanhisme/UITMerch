import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../stores/authStore";
import { CustomerProfilePage } from "./CustomerProfilePage";
import { OrganizerProfilePage } from "./OrganizerProfilePage";

export function ProfilePage() {
  const user = useAuthStore((state) => state.user);
  const location = useLocation();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;

  if (!user) {
    return <Navigate replace state={{ from: returnTo }} to="/auth" />;
  }

  if (user.role === "CUSTOMER") {
    return <CustomerProfilePage />;
  }

  if (user.role === "ORGANIZER") {
    return <OrganizerProfilePage />;
  }

  return <Navigate replace to="/" />;
}
