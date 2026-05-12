import { Link, useLocation } from "react-router-dom";
import { useState } from "react";

const logoHeaderUrl = "/assets/figma/logo-header.svg";
const accountIconUrl = "/assets/figma/account-icon.svg";

const navItems = [
  { label: "Trang chủ", href: "/" },
  { label: "Vật phẩm", href: "/merch" },
  { label: "Tổ chức", href: "/organization" },
];

export function TopNavBar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const location = useLocation();

  const normalizePath = (pathname: string) =>
    pathname.replace(/\/+$/, "") || "/";
  const currentPath = normalizePath(location.pathname);
  const accountReturnPath = `${location.pathname}${location.search}${location.hash}`;
  const isAccountActive = normalizePath("/auth") === currentPath;

  const linkClassName = (href: string) => {
    const isActive = normalizePath(href) === currentPath;

    return [
      "flex min-h-11 items-center rounded-full px-4 py-2 transition duration-200 ease-out",
      isActive
        ? "bg-white/70 text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.12)]"
        : "text-gray hover:bg-white/35 hover:text-black-blue",
    ].join(" ");
  };

  return (
    <div className="relative h-14 w-full sm:h-16 lg:w-[1280.41px]">
      <nav
        className="relative z-10 inline-flex h-14 w-full items-center justify-between rounded-full border border-white/70 bg-white/20 px-4 py-2 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px] sm:h-16 sm:px-[33px] sm:py-[11px] lg:w-[1280.41px] lg:gap-[369px] lg:justify-start"
        data-node-id="17:4918"
        data-name="TopNavBar Component"
      >
        <img
          alt="UITMerch"
          className="h-[26px] w-[126px] shrink-0 sm:h-[30px] sm:w-[145px]"
          data-node-id="17:4151"
          src={logoHeaderUrl}
        />

        {/* RESPONSIVE */}
        <div
          className="hidden shrink-0 items-center gap-8 whitespace-nowrap font-google text-[15px] leading-5 tracking-[-0.35px] text-gray md:flex lg:gap-[60px]"
          data-node-id="I17:4918;17:4888"
        >
          {navItems.map((item) => (
            <Link
              className={linkClassName(item.href)}
              to={item.href}
              key={item.label}
            >
              {item.label}
            </Link>
          ))}
        </div>

        <div
          className="hidden shrink-0 items-center pr-[1.01px] md:flex"
          data-node-id="I17:4918;17:4808"
        >
          <Link
            aria-label="Account"
            className={[
              "flex min-h-11 items-center rounded-full py-0.5 transition duration-200",
              isAccountActive
                ? "bg-white/70 shadow-[0_8px_20px_rgba(82,128,145,0.12)]"
                : "hover:bg-white/35",
            ].join(" ")}
            data-node-id="I17:4918;17:4809"
            state={{ from: accountReturnPath }}
            to="/auth"
          >
            <span className="flex size-[30.4px] scale-95 items-center justify-center rounded-full p-2">
              <img
                alt=""
                className="size-4"
                data-node-id="I17:4918;17:4811"
                src={accountIconUrl}
              />
            </span>
          </Link>
        </div>

        {/* RESPONSIVE */}
        <button
          aria-expanded={isMenuOpen}
          aria-label="Open navigation"
          className="grid size-11 place-items-center rounded-full font-google text-2xl leading-none text-slate transition duration-200 ease-out hover:bg-white/35 md:hidden"
          onClick={() => setIsMenuOpen((open) => !open)}
          type="button"
        >
          ☰
        </button>
      </nav>

      {/* RESPONSIVE */}
      {isMenuOpen && (
        <div className="absolute left-0 right-0 top-[calc(100%+10px)] z-20 rounded-[28px] border border-white/70 bg-white/75 p-3 font-google text-sm text-slate shadow-[0_18px_45px_rgba(82,128,145,0.16)] backdrop-blur-xl md:hidden">
          {navItems.map((item) => (
            <Link
              className={linkClassName(item.href)}
              key={item.label}
              to={item.href}
              onClick={() => setIsMenuOpen(false)}
            >
              {item.label}
            </Link>
          ))}
          <Link
            className={linkClassName("/auth")}
            onClick={() => setIsMenuOpen(false)}
            state={{ from: accountReturnPath }}
            to="/auth"
          >
            Account
          </Link>
        </div>
      )}
    </div>
  );
}
