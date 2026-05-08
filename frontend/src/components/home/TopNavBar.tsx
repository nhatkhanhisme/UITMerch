const logoHeaderUrl = "/assets/figma/logo-header.svg";
const accountIconUrl = "/assets/figma/account-icon.svg";

const navItems = ["Trang chủ", "Vật phẩm", "Tổ chức"];

export function TopNavBar() {
  return (
    <nav
      className="inline-flex h-16 w-[1280.41px] items-center gap-[369px] rounded-full border border-white/20 bg-white/70 px-[33px] py-[11px] shadow-[0_8px_32px_rgba(99,102,241,0.10)] backdrop-blur-[12px]"
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
  );
}
