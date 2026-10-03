import { Link, useLocation, useNavigate } from "react-router-dom";
import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronDown, LogOut, Menu, X } from "lucide-react";
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
  const navRef = useRef<HTMLDivElement | null>(null);
  const accountButtonRef = useRef<HTMLButtonElement | null>(null);
  const menuButtonRef = useRef<HTMLButtonElement | null>(null);

  const normalizePath = (pathname: string) =>
    pathname.replace(/\/+$/, "") || "/";
  const currentPath = normalizePath(location.pathname);
  const accountReturnPath = `${location.pathname}${location.search}${location.hash}`;
  const isAccountActive =
    ["/auth", "/profile", "/orders", "/cart", "/wishlist", "/following", "/restock-subscriptions", "/reservations", "/organizer", "/admin", "/guest-orders"].some((path) => currentPath === path || currentPath.startsWith(`${path}/`));
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
    if (!isAccountMenuOpen && !isMenuOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node | null;
      if (
        accountMenuRef.current &&
        target &&
        !accountMenuRef.current.contains(target)
      ) {
        setIsAccountMenuOpen(false);
      }
      if (target && !navRef.current?.contains(target)) setIsMenuOpen(false);
    };
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setIsAccountMenuOpen(false);
      setIsMenuOpen(false);
      (isMenuOpen ? menuButtonRef : accountButtonRef).current?.focus();
    };

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isAccountMenuOpen, isMenuOpen]);

  useEffect(() => {
    setIsAccountMenuOpen(false);
    setIsMenuOpen(false);
    setVisualSearchOpen(false);
  }, [location.pathname, location.search, location.hash]);

  useEffect(() => {
    const desktop = window.matchMedia("(min-width: 1280px)");
    const closeMenus = () => {
      setIsMenuOpen(false);
      setIsAccountMenuOpen(false);
    };
    desktop.addEventListener("change", closeMenus);
    return () => desktop.removeEventListener("change", closeMenus);
  }, []);

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

  const isCurrent = (href: string) =>
    currentPath === href ||
    (href !== "/" && currentPath.startsWith(`${href}/`)) ||
    (href === "/events" && currentPath.startsWith("/event/")) ||
    (href === "/merch" && currentPath.startsWith("/campaigns")) ||
    (href === "/profile" && currentPath.startsWith("/profile/"));
  const linkClassName = (href: string) =>
    [
      "flex min-h-11 items-center rounded-full px-4 py-2 font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700",
      isCurrent(href)
        ? "bg-black-blue text-white shadow-sm font-semibold"
        : "text-slate hover:bg-white/50 hover:text-black-blue",
    ].join(" ");
  const accountLinks = [
    {
      label: user ? "Hồ sơ" : "Đăng nhập / Đăng ký",
      href: user ? "/profile" : "/auth",
    },
    ...(user?.role === "CUSTOMER"
      ? [
          { label: "Giỏ hàng", href: "/cart" },
          { label: "Đơn hàng của tôi", href: "/orders" },
          { label: "Đặt trước của tôi", href: "/reservations" },
          { label: "Yêu thích", href: "/wishlist" },
          { label: "Tổ chức đang theo dõi", href: "/following" },
          { label: "Nhắc khi có hàng", href: "/restock-subscriptions" },
        ]
      : []),
    ...(user?.role === "ORGANIZER"
      ? [
          { label: "Quản lý BTC", href: "/organizer" },
          { label: "Tra cứu đơn khách", href: "/guest-orders" },
        ]
      : []),
    ...(user?.role === "ADMIN" ? [{ label: "Quản trị", href: "/admin" }] : []),
  ];
  const renderAccountLinks = () =>
    accountLinks.map((item) => (
      <Link
        key={item.href}
        to={item.href}
        state={item.href === "/auth" ? accountState : undefined}
        aria-current={isCurrent(item.href) ? "page" : undefined}
        className={`${linkClassName(item.href)} rounded-xl text-sm`}
        onClick={handleNavClick}
      >
        {item.label}
      </Link>
    ));

  return (
    <>
      <div ref={navRef} className="relative h-14 w-full sm:h-16">
        <nav
          aria-label="Điều hướng chính"
          className="relative z-10 flex h-14 w-full items-center gap-2 rounded-full border border-white/70 bg-white/35 px-3 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95)] backdrop-blur-xl sm:h-16 sm:px-5 xl:gap-6"
        >
          <Link
            aria-label="UITMerch — về trang chủ"
            onClick={handleNavClick}
            to="/"
            className="shrink-0 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700"
          >
            <img
              alt="UITMerch"
              className="h-6 w-[117px] sm:h-7 sm:w-[136.5px]"
              src={logoHeaderUrl}
            />
          </Link>
          <div className="hidden min-w-0 flex-1 items-center justify-center gap-1 whitespace-nowrap font-sans text-[15px] xl:flex">
            {navItems.map((item) => (
              <Link
                key={item.href}
                className={linkClassName(item.href)}
                to={item.href}
                aria-current={isCurrent(item.href) ? "page" : undefined}
                onClick={handleNavClick}
              >
                {item.label}
              </Link>
            ))}
          </div>
          <div className="ml-auto flex shrink-0 items-center gap-1 sm:gap-2 xl:ml-0">
            <button
              aria-label="Tìm kiếm bằng ảnh AI"
              title="Tìm kiếm bằng ảnh AI"
              type="button"
              className="flex size-11 shrink-0 items-center justify-center gap-2 rounded-full bg-brand-gradient font-sans text-sm font-semibold text-black-blue transition-colors hover:brightness-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700 xl:w-auto xl:px-4"
              onClick={() => {
                setIsAccountMenuOpen(false);
                setIsMenuOpen(false);
                setVisualSearchOpen(true);
              }}
            >
              <SparkleIcon />
              <span className="hidden xl:inline">Tìm bằng AI</span>
            </button>
            <NotificationBell />
            <div className="relative hidden xl:block" ref={accountMenuRef}>
              <button
                ref={accountButtonRef}
                type="button"
                aria-label={accountLabel}
                aria-expanded={isAccountMenuOpen}
                aria-controls="nav-account-panel"
                className={`flex min-h-11 max-w-[176px] items-center gap-2 rounded-full py-1 pl-1 pr-3 font-sans text-sm font-medium text-black-blue transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700 ${isAccountActive || isAccountMenuOpen ? "bg-cyan-100 ring-2 ring-cyan-700" : "hover:bg-white/50"}`}
                onClick={() => {
                  setIsMenuOpen(false);
                  setIsAccountMenuOpen((open) => !open);
                }}
              >
                <span className="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-white/80">
                  {user?.avatarUrl ? (
                    <img
                      alt=""
                      src={user.avatarUrl}
                      className="size-full object-cover"
                    />
                  ) : user ? (
                    <span className="text-xs font-semibold">
                      {avatarFallback}
                    </span>
                  ) : (
                    <img alt="" src={accountIconUrl} className="size-4" />
                  )}
                </span>
                <span className="truncate" title={accountLabel}>
                  {accountLabel}
                </span>
                <ChevronDown
                  aria-hidden="true"
                  className={`size-4 shrink-0 transition-transform ${isAccountMenuOpen ? "rotate-180" : ""}`}
                />
              </button>
              {isAccountMenuOpen && (
                <div
                  id="nav-account-panel"
                  className="absolute right-0 top-[calc(100%+12px)] max-h-[calc(100dvh-110px)] w-64 overflow-y-auto rounded-2xl border border-white/80 bg-white/95 p-2 text-black-blue shadow-[0_18px_45px_rgba(82,128,145,0.18)] backdrop-blur-xl"
                >
                  <nav aria-label="Tài khoản và tiện ích">
                    {renderAccountLinks()}
                    {user && (
                      <>
                        <div className="my-2 border-t border-slate/15" />
                        <button
                          type="button"
                          className="flex min-h-11 w-full items-center gap-2 rounded-xl px-4 text-left text-sm font-medium hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700"
                          onClick={handleLogout}
                        >
                          <LogOut className="size-4" aria-hidden="true" />
                          Đăng xuất
                        </button>
                      </>
                    )}
                  </nav>
                </div>
              )}
            </div>
            <button
              ref={menuButtonRef}
              aria-expanded={isMenuOpen}
              aria-controls="nav-mobile-panel"
              aria-label={isMenuOpen ? "Đóng điều hướng" : "Mở điều hướng"}
              type="button"
              className="grid size-11 shrink-0 place-items-center rounded-full text-black-blue transition-colors hover:bg-white/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700 xl:hidden"
              onClick={() => {
                setIsAccountMenuOpen(false);
                setIsMenuOpen((open) => !open);
              }}
            >
              {isMenuOpen ? (
                <X className="size-5" aria-hidden="true" />
              ) : (
                <Menu className="size-5" aria-hidden="true" />
              )}
            </button>
          </div>
        </nav>
        {isMenuOpen && (
          <div
            id="nav-mobile-panel"
            className="absolute left-0 right-0 top-[calc(100%+10px)] z-20 max-h-[calc(100dvh-100px)] overflow-y-auto rounded-2xl border border-white/80 bg-white/95 p-3 font-sans text-slate shadow-[0_18px_45px_rgba(82,128,145,0.18)] backdrop-blur-xl xl:hidden"
          >
            <nav
              aria-label="Điều hướng thu gọn"
              className="grid grid-cols-1 gap-1 min-[400px]:grid-cols-2"
            >
              {navItems.map((item) => (
                <Link
                  key={item.href}
                  className={linkClassName(item.href)}
                  to={item.href}
                  aria-current={isCurrent(item.href) ? "page" : undefined}
                  onClick={handleNavClick}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
            <div className="my-3 border-t border-slate/15" />
            <p className="px-4 pb-1 text-xs font-semibold uppercase tracking-wider text-slate">
              {user ? user.fullName : "Tài khoản"}
            </p>
            <nav
              aria-label="Tài khoản trên màn hình nhỏ"
              className="grid grid-cols-1 gap-1 min-[400px]:grid-cols-2"
            >
              {renderAccountLinks()}
              {user && (
                <button
                  type="button"
                  className="flex min-h-11 items-center gap-2 rounded-xl px-4 text-left text-sm font-medium hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700"
                  onClick={handleLogout}
                >
                  <LogOut className="size-4" aria-hidden="true" />
                  Đăng xuất
                </button>
              )}
            </nav>
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
