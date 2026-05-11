const organizations = [
  "GDGoC UIT",
  "CLB Lập trình",
  "Khoa CNTT",
  "Khoa KHMT",
  "CLB Truyền thông",
  "Khoa MMT&TT",
  "UIT Guitar",
  "Đoàn Hội UIT",
];

function initials(name: string) {
  return name
    .split(" ")
    .map((word) => word[0])
    .join("")
    .slice(0, 3)
    .toUpperCase();
}

export function HomeOrgan() {
  return (
    <section
      // FIX: sizing
      className="
        relative isolate scroll-mt-16 overflow-hidden
        px-4 pb-8 pt-24
        sm:scroll-mt-20 sm:px-8 sm:pb-10 sm:pt-28
        lg:px-16 lg:pb-24
      "
      id="home-organ"
      data-section="home-organ"
    >
      <div className="relative z-10 mx-auto max-w-canvas">
        <div className="max-w-3xl">
          <p className="font-google text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
            Cộng đồng UIT
          </p>
          {/* // FIX: sizing */}
          <h2 className="mb-6 mt-2 font-brand text-3xl font-bold leading-tight text-black-blue sm:text-4xl lg:text-6xl">
            Khám phá merch từ các CLB và Khoa
          </h2>
        </div>

        {/* // FIX: sizing */}
        <div className="grid grid-cols-2 gap-6 sm:gap-8 md:grid-cols-3 md:pr-12 lg:grid-cols-4 lg:pr-16">
          {organizations.map((name, index) => (
            <article
              className="group flex min-w-0 flex-col items-center justify-center gap-3 bg-transparent transition duration-200 ease-out hover:-translate-y-1"
              key={name}
            >
              <div className="relative h-36 w-36 sm:h-40 sm:w-40">
                <div className="relative z-10 inline-flex h-full w-full items-center justify-center rounded-full border border-white/70 bg-white/20 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px]">
                  <span className="text-base font-bold text-slate-700">
                    {index === 0 ? "GDSC" : initials(name)}
                  </span>
                </div>
              </div>
              <h3 className="text-sm font-bold text-slate-700 text-center mt-1 w-full truncate">
                {name}
              </h3>
            </article>
          ))}

          <article className="group flex min-w-0 flex-col items-center justify-center gap-3 bg-transparent transition duration-200 ease-out hover:-translate-y-1">
            <div className="relative h-36 w-36 sm:h-40 sm:w-40">
              <div className="relative z-10 inline-flex h-full w-full items-center justify-center rounded-full border border-white/70 bg-white/20 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px]">
                <span className="text-base font-bold text-slate-700">...</span>
              </div>
            </div>
            <p className="text-sm font-bold text-slate-700 text-center mt-1 w-full truncate">
              Nhiều đơn vị khác
            </p>
          </article>
        </div>
      </div>
    </section>
  );
}
