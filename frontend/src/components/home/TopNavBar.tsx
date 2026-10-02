import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useAuthStore } from "../../stores/authStore";
import { logout } from "../../api/auth";
import { getCustomerProfile, getOrganizerProfile } from "../../api/profile";
import { NotificationBell } from "../../features/NotificationBell";
import { VisualSearchModal } from "../features/VisualSearchModal";

const logoHeaderUrl = "/assets/figma/logo-header.svg";
const accountIconUrl = "/assets/figma/account-icon.svg";

function SparkleIcon() {
  return (
    <svg
      className="size-[15px] shrink-0"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.2}
      viewBox="0 0 24 24"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09Z"
      />
    </svg>
  );
}

const navItems = [
  { label: "Trang chủ", href: "/" },
  { label: "Vật phẩm", href: "/merch" },
  { label: "Tổ chức", href: "/organization" },
  { label: "Sự kiện", href: "/events" },
  { label: "Đặt trước", href: "/campaigns" },
  { label: "Đơn khách", href: "/guest-orders" },
];

const getInitials = (fullName: string) => {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "U";
  }

  const [first, last] =
    parts.length === 1 ? [parts[0], ""] : [parts[0], parts[parts.length - 1]];
  return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
};

export function TopNavBar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const [visualSearchOpen, setVisualSearchOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const accessToken = useAuthStore((state) => state.accessToken);
  const clearSession = useAuthStore((state) => state.clearSession);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);

  const normalizePath = (pathname: string) =>
    pathname.replace(/\/+$/, "") || "/";
  const currentPath = normalizePath(location.pathname);
  const accountReturnPath = `${location.pathname}${location.search}${location.hash}`;
  const accountTarget = user ? "/profile" : "/auth";
  const isAccountActive =
    currentPath === "/auth" || currentPath.startsWith("/profile");
  const isAccountMenuActive = isAccountActive || isAccountMenuOpen;
  const accountLabel = user ? user.fullName : "Tài khoản";
  const avatarFallback = useMemo(
    () => (user?.fullName ? getInitials(user.fullName) : "U"),
    [user?.fullName],
  );
  const accountState = user ? undefined : { from: accountReturnPath };

  useEffect(() => {
    if (!user || user.avatarUrl) {
      return;
    }

    let isActive = true;

    const loadAvatar = async () => {
      try {
        if (user.role === "CUSTOMER") {
          const response = await getCustomerProfile();
          const profile = response.data;

          if (isActive && profile?.avatarUrl) {
            updateUser({
              avatarUrl: profile.avatarUrl,
              fullName: profile.fullName,
            });
          }

          return;
        }

        if (user.role === "ORGANIZER") {
          const response = await getOrganizerProfile();
          const profile = response.data;

          if (isActive && profile?.logoUrl) {
            updateUser({ avatarUrl: profile.logoUrl });
          }
        }
      } catch {
        // Ignore avatar fetch errors for nav display.
      }
    };

    void loadAvatar();

    return () => {
      isActive = false;
    };
  }, [updateUser, user]);

  useEffect(() => {
    if (!isAccountMenuOpen) {
      return;
    }

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node | null;
      if (
        accountMenuRef.current &&
        target &&
        !accountMenuRef.current.contains(target)
      ) {
        setIsAccountMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isAccountMenuOpen]);

  useEffect(() => {
    setIsAccountMenuOpen(false);
    setVisualSearchOpen(false);
  }, [location.pathname, location.search, location.hash]);

  const handleLogout = async () => {
    if (accessToken) {
      try {
        await logout(accessToken);
      } catch {
        // Clear session even if server-side logout fails
      }
    }
    clearSession();
    setIsAccountMenuOpen(false);
    setIsMenuOpen(false);
    navigate("/");
  };

  const scrollToTop = () => {
    // Home page uses its own overflow container, not window scroll.
    const homeContainer = document.querySelector(
      ".home-scroll-snap-container",
    ) as HTMLElement | null;
    if (homeContainer) {
      homeContainer.scrollTo({ top: 0, behavior: "smooth" });
    } else {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const handleNavClick = () => {
    setIsMenuOpen(false);
    setIsAccountMenuOpen(false);
    scrollToTop();
  };

  const linkClassName = (href: string) => {
    const isActive =
      href === "/profile"
        ? currentPath.startsWith("/profile")
        : normalizePath(href) === currentPath;

    return [
      "flex min-h-11 items-center rounded-full px-4 py-2 transition duration-200 ease-out",
      isActive
        ? "bg-white/70 text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.12)]"
        : "text-gray hover:bg-white/35 hover:text-black-blue",
    ].join(" ");
  };

  return (
    <>
      <div className="relative h-[52px] w-full sm:h-16 lg:w-[1280.41px]">
        <nav
          className="relative z-10 flex h-[52px] w-full items-center justify-between rounded-full border border-white/70 bg-white/20 px-3 py-2 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px] sm:h-16 sm:px-[33px] sm:py-[11px] lg:w-[1280.41px]"
          data-node-id="17:4918"
          data-name="TopNavBar Component"
        >
          <Link
            aria-label="UITMerch — về trang chủ"
            data-node-id="17:4151"
            onClick={handleNavClick}
            to="/"
          >
            <img
              alt="UITMerch"
              className="h-[24px] w-[116px] shrink-0 sm:h-[30px] sm:w-[145px]"
              src={logoHeaderUrl}
            />
          </Link>

          {/* RESPONSIVE */}
          <div
            className="hidden min-w-0 flex-1 items-center justify-center gap-6 whitespace-nowrap font-sans text-[15px] font-medium leading-5 text-gray md:flex lg:gap-10"
            data-node-id="I17:4918;17:4888"
          >
            {navItems.map((item) => (
              <Link
                className={linkClassName(item.href)}
                key={item.label}
                onClick={handleNavClick}
                to={item.href}
              >
                {item.label}
              </Link>
            ))}
          </div>

          {/* AI Visual Search — special feature */}
          <button
            aria-label="Tìm kiếm bằng ảnh AI"
            className="hidden md:flex items-center gap-2 rounded-full bg-brand-gradient px-4 py-2 text-sm font-bold text-black-blue whitespace-nowrap shrink-0 mr-1 animate-ai-glow transition-transform duration-200 hover:scale-105 active:scale-95"
            onClick={() => setVisualSearchOpen(true)}
            type="button"
          >
            <SparkleIcon />
            Tìm bằng AI
          </button>

          {user?.role === "CUSTOMER" && (
            <Link
              aria-label="Theo dõi và báo hàng"
              title="Theo dõi và báo hàng"
              className="rounded-full p-2"
              to="/following"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                aria-hidden="true"
              >
                <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z" />
              </svg>
            </Link>
          )}
          <NotificationBell />
          <div
            className="hidden shrink-0 items-center pr-[1.01px] md:flex"
            data-node-id="I17:4918;17:4808"
          >
            {user ? (
              <div className="relative min-w-0" ref={accountMenuRef}>
                <button
                  aria-expanded={isAccountMenuOpen}
                  aria-haspopup="menu"
                  aria-label={accountLabel}
                  className={[
                    "flex min-h-11 min-w-0 items-center gap-3 rounded-full py-0.5 pl-2 pr-3 transition duration-200",
                    "max-w-[180px] sm:max-w-[180px]",
                    isAccountMenuActive
                      ? "bg-white/70 shadow-[0_8px_20px_rgba(82,128,145,0.12)]"
                      : "hover:bg-white/35",
                  ].join(" ")}
                  data-node-id="I17:4918;17:4809"
                  onClick={() => setIsAccountMenuOpen((open) => !open)}
                  type="button"
                >
                  <span className="flex size-9 items-center justify-center overflow-hidden rounded-full bg-white/80 shadow-[0_6px_16px_rgba(82,128,145,0.16)]">
                    {user.avatarUrl ? (
                      <img
                        alt=""
                        className="size-full object-cover"
                        data-node-id="I17:4918;17:4811"
                        src={user.avatarUrl}
                      />
                    ) : (
                      <span className="font-fredoka text-xs font-bold text-black-blue">
                        {avatarFallback}
                      </span>
                    )}
                  </span>
                  <span
                    className="min-w-0 max-w-[140px] truncate font-sans text-sm font-medium text-black-blue sm:max-w-[180px]"
                    title={user.fullName}
                  >
                    {user.fullName}
                  </span>
                  <svg
                    aria-hidden="true"
                    className="ml-1 size-3 text-slate"
                    fill="none"
                    viewBox="0 0 12 8"
                  >
                    <path
                      d="M1 1.5L6 6.5L11 1.5"
                      stroke="currentColor"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="1.5"
                    />
                  </svg>
                </button>
                {isAccountMenuOpen ? (
                  <div
                    className="absolute right-0 top-[calc(100%+10px)] z-20 w-[220px] rounded-[24px] border border-white/70 bg-white/80 p-2 font-sans text-sm text-slate shadow-[0_18px_45px_rgba(82,128,145,0.16)] backdrop-blur-xl"
                    role="menu"
                  >
                    <Link
                      className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                      onClick={() => setIsAccountMenuOpen(false)}
                      role="menuitem"
                      to="/profile"
                    >
                      Hồ sơ
                      <span className="text-xs text-gray">Xem</span>
                    </Link>
                    {user.role === "CUSTOMER" && (
                      <>
                        <Link
                          className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                          onClick={() => setIsAccountMenuOpen(false)}
                          role="menuitem"
                          to="/cart"
                        >
                          Giỏ hàng
                          <span className="text-xs text-gray">🛒</span>
                        </Link>
                        <Link
                          className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                          onClick={() => setIsAccountMenuOpen(false)}
                          role="menuitem"
                          to="/orders"
                        >
                          Đơn hàng
                          <span className="text-xs text-gray">📦</span>
                        </Link>
                        <Link
                          className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                          onClick={() => setIsAccountMenuOpen(false)}
                          role="menuitem"
                          to="/wishlist"
                        >
                          Yêu thích
                          <span className="text-xs text-gray">♡</span>
                        </Link>
                      </>
                    )}
                    {user.role === "ORGANIZER" && (
                      <Link
                        className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                        onClick={() => setIsAccountMenuOpen(false)}
                        role="menuitem"
                        to="/organizer"
                      >
                        Quản lý BTC
                        <span className="text-xs text-gray">Dashboard</span>
                      </Link>
                    )}
                    {user.role === "ADMIN" && (
                      <Link
                        className="flex items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                        onClick={() => setIsAccountMenuOpen(false)}
                        role="menuitem"
                        to="/admin"
                      >
                        Quản trị
                        <span className="text-xs text-gray">Admin</span>
                      </Link>
                    )}
                    <button
                      className="mt-1 flex w-full items-center justify-between rounded-2xl px-4 py-3 font-semibold text-black-blue transition hover:bg-white"
                      onClick={handleLogout}
                      role="menuitem"
                      type="button"
                    >
                      Đăng xuất
                      <span className="text-xs text-gray">Thoát</span>
                    </button>
                  </div>
                ) : null}
              </div>
            ) : (
              <Link
                aria-label={accountLabel}
                className={[
                  "flex min-h-11 min-w-0 items-center gap-3 rounded-full py-0.5 pl-2 pr-4 transition duration-200",
                  "max-w-[220px] sm:max-w-[260px]",
                  isAccountActive
                    ? "bg-white/70 shadow-[0_8px_20px_rgba(82,128,145,0.12)]"
                    : "hover:bg-white/35",
                ].join(" ")}
                data-node-id="I17:4918;17:4809"
                state={accountState}
                to={accountTarget}
              >
                <span className="flex size-9 items-center justify-center overflow-hidden rounded-full bg-white/80 shadow-[0_6px_16px_rgba(82,128,145,0.16)]">
                  <img
                    alt=""
                    className="size-4"
                    data-node-id="I17:4918;17:4811"
                    src={accountIconUrl}
                  />
                </span>
                <span className="font-sans text-sm font-medium text-black-blue">
                  Tài khoản
                </span>
              </Link>
            )}
          </div>

          {/* RESPONSIVE */}
          <button
            aria-expanded={isMenuOpen}
            aria-label="Mở điều hướng"
            className="grid size-10 place-items-center rounded-full font-sans text-2xl leading-none text-slate transition duration-200 ease-out hover:bg-white/35 md:hidden"
            onClick={() => setIsMenuOpen((open) => !open)}
            type="button"
          >
            ☰
          </button>
        </nav>

        {/* RESPONSIVE */}
        {isMenuOpen && (
          <div className="absolute left-0 right-0 top-[calc(100%+10px)] z-20 max-h-[calc(100svh-5rem)] overflow-y-auto rounded-[24px] border border-white/70 bg-white/85 p-3 font-sans text-sm text-slate shadow-[0_18px_45px_rgba(82,128,145,0.16)] backdrop-blur-xl md:hidden">
            <button
              className="flex w-full items-center gap-2 rounded-full bg-brand-gradient px-4 py-3 font-bold text-black-blue mb-2 animate-ai-glow"
              onClick={() => {
                setVisualSearchOpen(true);
                setIsMenuOpen(false);
              }}
              type="button"
            >
              <SparkleIcon />
              Tìm kiếm bằng AI
            </button>
            {navItems.map((item) => (
              <Link
                className={linkClassName(item.href)}
                key={item.label}
                onClick={handleNavClick}
                to={item.href}
              >
                {item.label}
              </Link>
            ))}
            {user ? (
              <>
                <Link
                  className={linkClassName("/profile")}
                  onClick={() => setIsMenuOpen(false)}
                  to="/profile"
                >
                  Hồ sơ
                </Link>
                {user.role === "CUSTOMER" && (
                  <>
                    <Link
                      className={linkClassName("/cart")}
                      onClick={() => setIsMenuOpen(false)}
                      to="/cart"
                    >
                      Giỏ hàng
                    </Link>
                    <Link
                      className={linkClassName("/orders")}
                      onClick={() => setIsMenuOpen(false)}
                      to="/orders"
                    >
                      Đơn hàng
                    </Link>
                    <Link
                      className={linkClassName("/wishlist")}
                      onClick={() => setIsMenuOpen(false)}
                      to="/wishlist"
                    >
                      Yêu thích
                    </Link>
                  </>
                )}
                {user.role === "ORGANIZER" && (
                  <Link
                    className={linkClassName("/organizer")}
                    onClick={() => setIsMenuOpen(false)}
                    to="/organizer"
                  >
                    Quản lý BTC
                  </Link>
                )}
                {user.role === "ADMIN" && (
                  <Link
                    className={linkClassName("/admin")}
                    onClick={() => setIsMenuOpen(false)}
                    to="/admin"
                  >
                    Quản trị
                  </Link>
                )}
                <button
                  className={`${linkClassName("/logout")} w-full text-left`}
                  onClick={handleLogout}
                  type="button"
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <Link
                className={linkClassName(accountTarget)}
                onClick={() => setIsMenuOpen(false)}
                state={accountState}
                to={accountTarget}
              >
                Tài khoản
              </Link>
            )}
          </div>
        )}
      </div>

      {createPortal(
        <VisualSearchModal
          open={visualSearchOpen}
          onClose={() => setVisualSearchOpen(false)}
        />,
        document.body,
      )}
    </>
  );
}
