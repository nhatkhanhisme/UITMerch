const logoHeaderUrl = "/assets/figma/logo-header.svg";

const infoLeft = [
  "Khu phố 6, phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh",
  "Không gian học tập công nghệ, nghiên cứu và đổi mới sáng tạo",
  "Kết nối sinh viên với các hoạt động học thuật và cộng đồng",
];

const infoRight = [
  "Merch chính thức từ các khoa, câu lạc bộ và sự kiện UIT",
  "Sản phẩm được cập nhật theo từng đợt mở bán",
  "Thiết kế dành cho sinh viên, cựu sinh viên và người yêu UIT",
];

function InfoList({ items }: { items: string[] }) {
  return (
    <ul className="space-y-4 font-sans text-sm leading-7 text-gray sm:text-[15px]">
      {items.map((item) => (
        <li className="flex gap-3" key={item}>
          <span className="mt-2 h-0.5 w-7 shrink-0 rounded-full bg-peach" />
          <span>{item}</span>
        </li>
      ))}
    </ul>
  );
}

export function HomeEnd() {
  return (
    <section
      className="
        relative isolate flex min-h-[100svh] flex-col justify-between
        px-5 pb-10 pt-20
        sm:px-8 sm:pb-12 sm:pt-24
        lg:px-16 lg:pb-14 lg:pt-20
      "
      id="home-end"
      data-section="home-end"
    >
      {/* Decorative background text */}
      <div className="pointer-events-none absolute left-1/2 top-8 -translate-x-1/2 font-fredoka text-[32vw] leading-none text-blue-100/10 sm:top-12 lg:text-[300px]">
        UIT
      </div>

      {/* ── Main content — grows to push footer down ── */}
      <div className="relative z-10 mx-auto flex w-full max-w-canvas flex-1 flex-col">

        {/* Hero grid — content + campus image */}
        <div className="my-auto grid gap-8 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:gap-12">
          <div>
            <p className="font-sans text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
              Đại học Công nghệ Thông tin
            </p>
            <h2 className="mt-4 font-fredoka font-bold text-black-blue">
              <span className="flex flex-col gap-2 lg:gap-3">
                <span className="text-[30px] leading-none tracking-[0.01em] sm:text-4xl lg:text-[58px]">University of</span>
                <span className="text-[30px] leading-none tracking-[0.01em] sm:text-4xl lg:text-[58px]">Information</span>
                <span className="text-[30px] leading-none tracking-[0.01em] sm:text-4xl lg:text-[58px]">Technology</span>
              </span>
            </h2>
            <div className="mt-7 grid gap-5 sm:mt-8 md:grid-cols-2 md:gap-7">
              <InfoList items={infoLeft} />
              <InfoList items={infoRight} />
            </div>
          </div>

          <img
            src="/assets/figma/sticker/uit%20image-01%202.svg"
            alt="UIT Campus"
            className="w-full h-auto object-contain drop-shadow-[0_8px_32px_rgba(82,128,145,0.12)]"
          />
        </div>
      </div>

      {/* ── Footer — always pinned to bottom ── */}
      <footer className="relative z-10 mx-auto mt-12 w-full max-w-canvas">
        <div className="flex flex-col gap-4 border-t border-slate/15 pt-7 sm:flex-row sm:items-end sm:justify-between">
          {/* Logo + tagline */}
          <div className="flex flex-col items-center gap-2 sm:items-start">
            <img
              alt="UITMerch"
              className="h-[30px] w-[145px]"
              src={logoHeaderUrl}
            />
          </div>

          {/* Copyright */}
          <p className="text-center font-sans text-sm text-gray/80 sm:text-right">
            Copyright © 2026 UITMerch.
            <br className="sm:hidden" />
            {" "}Đồng hành cùng cộng đồng UIT.
          </p>
        </div>
      </footer>
    </section>
  );
}
