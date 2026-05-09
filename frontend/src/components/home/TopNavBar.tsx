const logoHeaderUrl = "/assets/figma/logo-header.svg";
const accountIconUrl = "/assets/figma/account-icon.svg";

const navItems = ["Trang chủ", "Vật phẩm", "Tổ chức"];

export function TopNavBar() {
  return (
    <div className="relative h-16 w-[1280.41px]">
      <nav
        className="relative z-10 inline-flex h-16 w-[1280.41px] items-center gap-[369px] rounded-full border border-white/70 bg-white/20 px-[33px] py-[11px] shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px]"
        data-node-id="17:4918"
        data-name="TopNavBar Component"
      >
        <img
          alt="UITMerch"
          className="h-[30px] w-[145px] shrink-0"
          data-node-id="17:4151"
          src={logoHeaderUrl}
        />

        <div
          className="flex shrink-0 items-center gap-[60px] whitespace-nowrap font-google text-[15px] leading-5 tracking-[-0.35px] text-gray"
          data-node-id="I17:4918;17:4888"
        >
          {navItems.map((item) => (
            <a className="flex flex-col justify-center" href="#" key={item}>
              {item}
            </a>
          ))}
        </div>

        <div
          className="flex shrink-0 items-center pr-[1.01px]"
          data-node-id="I17:4918;17:4808"
        >
          <button
            aria-label="Account"
            className="flex h-[42px] items-center py-0.5"
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
      </nav>
    </div>
  );
}
