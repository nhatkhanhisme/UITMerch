import { useEffect, useState } from "react";
import { HomeMoreLink } from "./HomeMoreLink";

const stickerUrl = "/assets/figma/sticker/Layer_1.svg";
const slideDuration = 5000;

const productSlides = [
  {
    badge: "Wow Pingle so cute",
    description:
      "Sticker GDG mang tinh thần công nghệ UIT lên những vật dụng quen thuộc hằng ngày. Thiết kế nổi bật, dễ nhận diện và phù hợp để trang trí laptop, bình nước hoặc góc học tập.",
    image: stickerUrl,
    subtitle: "Cài dòng này tiêu đề phụ",
    title: "Sticker Pingle GDG",
  },
  {
    badge: "Limited GDG edition",
    description:
      "Dây đeo GDG là phụ kiện gọn nhẹ cho sinh viên UIT trong các buổi workshop, sự kiện và sinh hoạt câu lạc bộ. Màu sắc trẻ trung, dễ phối cùng balo hoặc thẻ sinh viên.",
    image: stickerUrl,
    subtitle: "Cài dòng này tiêu đề phụ",
    title: "Dây Đeo GDG",
  },
  {
    badge: "UIT community merch",
    description:
      "Sổ tay GDG giúp bạn ghi lại ý tưởng, checklist học tập và kế hoạch dự án theo cách ngăn nắp hơn. Thiết kế tối giản, tiện mang theo mỗi ngày.",
    image: stickerUrl,
    subtitle: "Cài dòng này tiêu đề phụ",
    title: "Sổ Tay GDG",
  },
];

export function HomeItem() {
  const [activeSlide, setActiveSlide] = useState(0);
  const [previousSlide, setPreviousSlide] = useState<number | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const activeProduct = productSlides[activeSlide];
  const previousProduct =
    previousSlide === null ? null : productSlides[previousSlide];

  const goToSlide = (nextSlide: number) => {
    if (nextSlide === activeSlide) {
      return;
    }

    setPreviousSlide(activeSlide);
    setActiveSlide(nextSlide);
    window.setTimeout(() => setPreviousSlide(null), 560);
  };

  useEffect(() => {
    if (isPaused) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      goToSlide((activeSlide + 1) % productSlides.length);
    }, slideDuration);

    return () => window.clearInterval(timer);
  }, [activeSlide, isPaused]);

  return (
    <section
      className="
        relative isolate flex min-h-[100svh] items-center
        px-5 py-20
        sm:px-8 sm:py-24
        lg:px-16 lg:py-16
      "
      id="home-item"
      data-section="home-item"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      <style>
        {`
          @keyframes homeItemEnterRight {
            from {
              opacity: 0;
              transform: translate3d(46px, 0, 0);
            }
            to {
              opacity: 1;
              transform: translate3d(0, 0, 0);
            }
          }

          @keyframes homeItemExitLeft {
            from {
              opacity: 1;
              transform: translate3d(0, 0, 0);
            }
            to {
              opacity: 0;
              transform: translate3d(-46px, 0, 0);
            }
          }

          .home-item-enter {
            animation: homeItemEnterRight 560ms cubic-bezier(0.22, 1, 0.36, 1) both;
            grid-area: 1 / 1;
          }

          .home-item-exit {
            animation: homeItemExitLeft 560ms cubic-bezier(0.22, 1, 0.36, 1) both;
            grid-area: 1 / 1;
            pointer-events: none;
          }
        `}
      </style>

      <div className="relative z-10 mx-auto w-full max-w-canvas">
        <div
          className="
            grid items-center gap-10
            lg:grid-cols-[0.95fr_1.05fr] lg:gap-10
            xl:gap-14
          "
        >
          <div className="mx-auto w-full max-w-[760px] text-center">
            <div className="grid">
              {previousProduct && (
                <div className="home-item-exit">
                  <p className="font-google text-xs font-semibold uppercase tracking-[0.22em] text-slate/70 sm:text-sm">
                    Vật phẩm nổi bật
                  </p>
                  <h2 className="mt-5 whitespace-nowrap font-google text-[36px] font-black leading-[1.08] text-peach sm:text-[48px] lg:text-[54px] xl:text-[60px]">
                    {previousProduct.title}
                  </h2>
                  <p className="mt-1 font-google text-xl font-bold text-gold sm:text-2xl">
                    {previousProduct.subtitle}
                  </p>
                  <p className="mx-auto mt-10 max-w-[430px] font-google text-sm leading-6 text-gray">
                    {previousProduct.description}
                  </p>
                </div>
              )}

              <div className="home-item-enter" key={`copy-${activeSlide}`}>
                <p className="font-google text-xs font-semibold uppercase tracking-[0.22em] text-slate/70 sm:text-sm">
                  Vật phẩm nổi bật
                </p>
                <h2 className="mt-5 whitespace-nowrap font-google text-[36px] font-black leading-[1.08] text-peach sm:text-[48px] lg:text-[54px] xl:text-[60px]">
                  {activeProduct.title}
                </h2>
                <p className="mt-1 font-google text-xl font-bold text-gold sm:text-2xl">
                  {activeProduct.subtitle}
                </p>
                <p className="mx-auto mt-10 max-w-[430px] font-google text-sm leading-6 text-gray">
                  {activeProduct.description}
                </p>
              </div>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-[460px]">
            <div className="absolute inset-6 rounded-[42px] bg-white/35 blur-2xl" />
            <div
              className="
                relative aspect-square overflow-hidden rounded-[34px]
                border border-white/80 bg-white/30 p-8
                shadow-[20px_28px_70px_rgba(82,128,145,0.22),inset_2px_2px_14px_rgba(255,255,255,0.95)]
                backdrop-blur-md
              "
            >
              <div className="absolute left-7 top-7 z-20 grid">
                {previousProduct && (
                  <div className="home-item-exit rounded-full bg-white/70 px-4 py-2 font-google text-xs font-semibold text-slate shadow-[0_12px_30px_rgba(82,128,145,0.14)]">
                    {previousProduct.badge}
                  </div>
                )}
                <div
                  className="home-item-enter rounded-full bg-white/70 px-4 py-2 font-google text-xs font-semibold text-slate shadow-[0_12px_30px_rgba(82,128,145,0.14)]"
                  key={`badge-${activeSlide}`}
                >
                  {activeProduct.badge}
                </div>
              </div>

              <div className="absolute inset-0 grid place-items-center">
                {previousProduct && (
                  <img
                    alt={previousProduct.title}
                    className="home-item-exit h-[72%] w-[72%] object-contain drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)]"
                    src={previousProduct.image}
                  />
                )}
                <img
                  alt={activeProduct.title}
                  className="home-item-enter h-[72%] w-[72%] object-contain drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)]"
                  key={`image-${activeSlide}`}
                  src={activeProduct.image}
                />
              </div>
            </div>
          </div>

        </div>

        <div className="mt-10 flex flex-col items-center gap-8">
          <div
            className="
              flex items-center justify-center gap-4
              sm:gap-5
              lg:gap-6
            "
            aria-label="Product slides"
          >
            {productSlides.map((product, index) => {
              const isActive = activeSlide === index;

              return (
                <button
                  aria-label={`Show ${product.title}`}
                  aria-current={isActive}
                  className={[
                    "relative grid h-4 w-4 place-items-center rounded-full border border-white bg-white/45",
                    "shadow-[0_6px_14px_rgba(82,128,145,0.18),inset_1px_1px_5px_rgba(255,255,255,1),inset_-1px_-1px_3px_rgba(71,85,105,0.08)]",
                    "backdrop-blur-md transition duration-300 ease-out",
                    "hover:-translate-y-0.5 hover:bg-white/65 hover:shadow-[0_8px_18px_rgba(82,128,145,0.22),inset_1px_1px_6px_rgba(255,255,255,1)]",
                    "sm:h-5 sm:w-5",
                    isActive
                      ? "scale-[1.35] border-white/95 bg-white/35 shadow-[0_10px_24px_rgba(82,128,145,0.16),0_0_0_1px_rgba(255,255,255,0.72),inset_2px_2px_8px_rgba(255,255,255,0.96),inset_-2px_-2px_5px_rgba(82,128,145,0.12)]"
                      : "",
                  ].join(" ")}
                  key={product.title}
                  onClick={() => goToSlide(index)}
                  type="button"
                >
                  {isActive ? (
                    <span
                      aria-hidden="true"
                      className="absolute inset-[3px] rounded-full bg-white/45 shadow-[inset_1px_1px_4px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(82,128,145,0.1)]"
                    />
                  ) : null}
                </button>
              );
            })}
          </div>

          <HomeMoreLink ariaLabel="Xem them vat pham" to="/merch" />
        </div>
      </div>
    </section>
  );
}
