const productHighlights = [
  "Chất liệu decal nhám chống nước",
  "Kích thước gọn cho laptop, bình nước, sổ tay",
  "Thiết kế nhận diện GDGoC UIT phiên bản giới hạn"
];

const stickerUrl = "/assets/figma/sticker/Layer_1.svg";

export function HomeItem() {
  return (
    <section
      className="relative scroll-mt-16 overflow-hidden bg-canvas px-5 py-16 sm:scroll-mt-20 sm:px-8 sm:py-20 lg:px-16 lg:py-24"
      data-section="home-item"
    >
      {/* RESPONSIVE */}
      <div className="relative mx-auto grid max-w-canvas items-center gap-10 pt-16 sm:pt-20 md:grid-cols-[0.9fr_1.1fr] md:gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:gap-20 lg:pt-24">
        <div className="order-2 mx-auto max-w-xl text-center md:order-1 md:text-left">
          <p className="font-google text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
            Vật phẩm nổi bật
          </p>

          {/* RESPONSIVE */}
          <h2 className="mt-5 font-brand text-[42px] font-extrabold leading-[1.05] text-peach sm:text-6xl lg:text-[88px]">
            Sticker GDG
          </h2>

          {/* RESPONSIVE */}
          <p className="mt-5 font-google text-xl font-semibold text-black-blue sm:text-2xl">
            Mang tinh thần công nghệ UIT lên mọi góc học tập.
          </p>

          {/* RESPONSIVE */}
          <p className="mx-auto mt-5 max-w-[560px] font-google text-sm leading-7 text-gray sm:text-base sm:leading-8 md:mx-0">
            Bộ sticker GDGoC UIT được thiết kế với bề mặt mịn, màu sắc tươi và
            độ bám tốt để bạn cá nhân hóa laptop, bình nước hoặc góc làm việc
            mỗi ngày.
          </p>

          {/* RESPONSIVE */}
          <ul className="mt-7 space-y-3 text-left font-google text-sm leading-6 text-slate sm:text-[15px]">
            {productHighlights.map((item) => (
              <li className="flex items-start gap-3" key={item}>
                <span className="mt-2 size-2 shrink-0 rounded-full bg-gold shadow-[0_0_0_5px_rgba(248,217,135,0.24)]" />
                <span>{item}</span>
              </li>
            ))}
          </ul>

          <button
            className="mt-9 min-h-11 rounded-full bg-black-blue px-8 py-4 font-google text-sm font-semibold uppercase tracking-[0.18em] text-white shadow-[0_20px_45px_rgba(19,27,46,0.22)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-slate"
            type="button"
          >
            Xem chi tiết
          </button>
        </div>

        {/* RESPONSIVE */}
        <div className="relative order-1 mx-auto w-4/5 max-w-[420px] md:order-2 md:w-full md:max-w-[520px] lg:-translate-x-20 lg:max-w-[620px]">
          <div className="absolute inset-6 rounded-[44px] bg-white/35 blur-2xl" />

          {/* RESPONSIVE */}
          <div className="relative overflow-hidden rounded-[32px] border border-white/75 bg-white/35 p-4 shadow-[18px_24px_70px_rgba(80,128,150,0.22),inset_2px_2px_12px_rgba(255,255,255,0.9)] backdrop-blur-md transition duration-200 ease-out hover:-translate-y-1 hover:shadow-[20px_30px_80px_rgba(80,128,150,0.28),inset_2px_2px_12px_rgba(255,255,255,0.95)] sm:rounded-[46px] sm:p-8">
            <div className="relative aspect-[1.08] rounded-[24px] bg-gradient-to-br from-white via-[#EEF9FC] to-[#D7F2FA] p-4 sm:rounded-[34px] sm:p-8">
              {/* RESPONSIVE */}
              <div className="absolute left-4 top-4 z-20 rounded-full bg-white/70 px-4 py-2 font-google text-xs font-semibold text-slate shadow-[0_12px_30px_rgba(82,128,145,0.14)] sm:left-8 sm:top-8 sm:px-5 sm:text-sm">
                Limited drop
              </div>

              <img
                alt="Sticker UIT"
                className="absolute left-1/2 top-1/2 z-10 h-[74%] w-[74%] -translate-x-1/2 -translate-y-1/2 object-contain drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)] sm:h-[68%] sm:w-[68%]"
                src={stickerUrl}
              />
            </div>
          </div>

          <div className="mt-8 flex justify-center gap-3" aria-label="Product slides">
            <span className="h-2.5 w-8 rounded-full bg-peach" />
            <span className="size-2.5 rounded-full bg-white/80 shadow-[inset_0_0_0_1px_rgba(71,85,105,0.16)]" />
            <span className="size-2.5 rounded-full bg-white/80 shadow-[inset_0_0_0_1px_rgba(71,85,105,0.16)]" />
          </div>
        </div>
      </div>
    </section>
  );
}
