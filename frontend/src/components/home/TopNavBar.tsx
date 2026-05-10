import { useState } from "react";

const logoHeaderUrl = "/assets/figma/logo-header.svg";
const accountIconUrl = "/assets/figma/account-icon.svg";

const navItems = ["Trang chủ", "Vật phẩm", "Tổ chức"];

export function TopNavBar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

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
            <a className="flex min-h-11 flex-col justify-center" href="#" key={item}>
              {item}
            </a>
          ))}
        </div>

        <div
          className="hidden shrink-0 items-center pr-[1.01px] md:flex"
          data-node-id="I17:4918;17:4808"
        >
          <button
            aria-label="Account"
            className="flex min-h-11 items-center py-0.5"
            data-node-id="I17:4918;17:4809"
            type="button"
          >
            <span className="flex size-[30.4px] scale-95 items-center justify-center rounded-full p-2">
              <img
                alt=""
                className="size-4"
                data-node-id="I17:4918;17:4811"
                src={accountIconUrl}
              />
            </span>
          </button>
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
            <a
              className="flex min-h-11 items-center rounded-2xl px-4 transition duration-200 ease-out hover:bg-white/70"
              href="#"
              key={item}
              onClick={() => setIsMenuOpen(false)}
            >
              {item}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
