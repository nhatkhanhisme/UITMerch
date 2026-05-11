const productHighlights = [
  "Chất liệu decal nhám chống nước",
  "Kích thước gọn cho laptop, bình nước, sổ tay",
  "Thiết kế nhận diện GDGoC UIT phiên bản giới hạn",
];

const stickerUrl = "/assets/figma/sticker/Layer_1.svg";

export function HomeItem() {
  return (
    <section
      className="
        relative isolate min-h-[100svh]
        px-5 pb-10 pt-20
        sm:px-8 sm:pb-12 sm:pt-24
        lg:px-16 lg:pb-14 lg:pt-28
      "
      id="home-item"
      data-section="home-item"
    >
      {/* Content */}
      <div
        className="
          relative z-10 mx-auto grid max-w-canvas items-center
          gap-10
          md:grid-cols-[0.9fr_1.1fr] md:gap-8
          lg:grid-cols-[0.9fr_1.1fr] lg:gap-14
        "
      >
        <div className="order-2 mx-auto max-w-xl text-center md:order-1 md:text-left">
          <p
            className="
              font-google text-xs font-semibold uppercase
              tracking-[0.22em] text-slate/70
              sm:text-sm
            "
          >
            Vật phẩm nổi bật
          </p>

          <h2
            className="
              mt-5 font-brand font-extrabold leading-[1.05] text-peach
              text-[40px]
              sm:text-5xl
              lg:text-[70px]
              xl:text-[76px]
            "
          >
            Sticker
            <br />
            GDG
          </h2>

          <p
            className="
              mt-5 font-google font-semibold text-black-blue
              text-lg
              sm:text-xl
              lg:max-w-[620px]
            "
          >
            Mang tinh thần công nghệ UIT lên mọi góc học tập.
          </p>

          <p
            className="
              mx-auto mt-5 max-w-[560px]
              font-google text-sm leading-7 text-gray
              sm:text-[15px] sm:leading-7
              md:mx-0
            "
          >
            Bộ sticker GDGoC UIT được thiết kế với bề mặt mịn, màu sắc tươi và
            độ bám tốt để bạn cá nhân hóa laptop, bình nước hoặc góc làm việc
            mỗi ngày.
          </p>

          <ul
            className="
              mx-auto mt-7 max-w-[560px] space-y-3 text-left
              font-google text-sm leading-6 text-slate
              sm:text-[15px]
              md:mx-0
            "
          >
            {productHighlights.map((item) => (
              <li className="flex items-start gap-3" key={item}>
                <span className="mt-2 size-2 shrink-0 rounded-full bg-gold shadow-[0_0_0_5px_rgba(248,217,135,0.24)]" />
                <span>{item}</span>
              </li>
            ))}
          </ul>

          <button
            className="
              mt-9 min-h-11 rounded-full bg-black-blue
              px-7 py-3.5
              font-google text-sm font-semibold uppercase tracking-[0.18em]
              text-white
              shadow-[0_20px_45px_rgba(19,27,46,0.22)]
              transition duration-200 ease-out
              hover:-translate-y-0.5 hover:bg-slate
            "
            type="button"
          >
            Xem chi tiết
          </button>
        </div>

        <div
          className="
            relative order-1 mx-auto
            w-[80%] max-w-[340px]
            md:order-2 md:w-full md:max-w-[440px]
            lg:max-w-[520px]
            xl:max-w-[560px]
          "
        >
          <div className="absolute inset-6 rounded-[44px] bg-white/35 blur-2xl" />

          <div
            className="
              relative overflow-hidden
              rounded-[32px] border border-white/75 bg-white/35
              p-4
              shadow-[18px_24px_70px_rgba(80,128,150,0.22),inset_2px_2px_12px_rgba(255,255,255,0.9)]
              backdrop-blur-md
              transition duration-200 ease-out
              hover:-translate-y-1
              hover:shadow-[20px_30px_80px_rgba(80,128,150,0.28),inset_2px_2px_12px_rgba(255,255,255,0.95)]
              sm:rounded-[46px] sm:p-8
            "
          >
            <div
              className="
                relative aspect-[1.08]
                rounded-[24px]
                bg-gradient-to-br from-white via-[#EEF9FC] to-[#D7F2FA]
                p-4
                sm:rounded-[34px] sm:p-8
              "
            >
              <div
                className="
                  absolute left-4 top-4 z-20 rounded-full
                  bg-white/70 px-4 py-2
                  font-google text-xs font-semibold text-slate
                  shadow-[0_12px_30px_rgba(82,128,145,0.14)]
                  sm:left-7 sm:top-7 sm:px-4 sm:text-xs
                "
              >
                Wow Pingle so cute
              </div>

              <img
                alt="Sticker UIT"
                className="
                  absolute left-1/2 top-1/2 z-10
                  h-[74%] w-[74%]
                  -translate-x-1/2 -translate-y-1/2
                  object-contain
                  drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)]
                  sm:h-[68%] sm:w-[68%]
                "
                src={stickerUrl}
              />
            </div>
          </div>

          <div
            className="mt-6 flex justify-center gap-3"
            aria-label="Product slides"
          >
            <span className="h-2.5 w-8 rounded-full bg-peach" />
            <span className="size-2.5 rounded-full bg-white/80 shadow-[inset_0_0_0_1px_rgba(71,85,105,0.16)]" />
            <span className="size-2.5 rounded-full bg-white/80 shadow-[inset_0_0_0_1px_rgba(71,85,105,0.16)]" />
          </div>
        </div>
      </div>
    </section>
  );
}
