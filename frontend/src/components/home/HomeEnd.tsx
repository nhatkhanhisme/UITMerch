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
    <ul className="space-y-4 font-google text-sm leading-7 text-gray sm:text-[15px]">
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
        relative isolate min-h-[100svh]
        px-5 pb-8 pt-20
        sm:px-8 sm:pb-10 sm:pt-24
        lg:px-16 lg:pb-14 lg:pt-28
      "
      id="home-end"
      data-section="home-end"
    >
      {/* RESPONSIVE */}
      <div className="pointer-events-none absolute left-1/2 top-8 -translate-x-1/2 font-display text-[32vw] leading-none text-blue-100/10 sm:top-12 sm:text-blue-100/10 lg:text-[300px] lg:text-blue-100/10">
        UIT
      </div>

      <div className="relative z-10 mx-auto max-w-canvas pt-12 sm:pt-16 lg:pt-20">
        {/* RESPONSIVE */}
        <div className="grid gap-8 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:gap-12">
          <div>
            <p className="font-google text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
              Đại học Công nghệ Thông tin
            </p>
            {/* RESPONSIVE */}
            <h2 className="mt-4 max-w-4xl font-brand text-[30px] font-extrabold leading-tight text-black-blue sm:text-4xl lg:text-[62px]">
              University of Information Technology
            </h2>
            {/* RESPONSIVE */}
            <div className="mt-7 grid gap-5 sm:mt-8 md:grid-cols-2 md:gap-7">
              <InfoList items={infoLeft} />
              <InfoList items={infoRight} />
            </div>
          </div>

          <img
            src="/assets/figma/sticker/uit%20image-01%202.svg"
            alt="UIT Campus"
            className="w-full h-auto object-contain"
          />
        </div>

        {/* RESPONSIVE */}
        <footer className="relative mt-12 flex flex-col gap-5 border-t border-slate/10 pt-8 text-center font-google text-sm text-gray sm:mt-16 sm:flex-row sm:items-center sm:justify-between sm:text-left">
          <img
            alt="UITMerch"
            className="mx-auto h-[30px] w-[145px] sm:mx-0"
            src={logoHeaderUrl}
          />
          <p>Copyright 2026 UITMerch. Đồng hành cùng cộng đồng UIT.</p>
        </footer>
      </div>
    </section>
  );
}
