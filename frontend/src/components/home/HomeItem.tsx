import { useEffect, useState } from "react";
import { HomeMoreLink } from "./HomeMoreLink";
import { getPopularMerch } from "../../api/merch";
import { getPublicOrganizations } from "../../api/organization";

const slideDuration = 5000;

type SlideItem = {
  badge: string;
  description: string;
  image: string;
  subtitle: string;
  title: string;
};

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

export function HomeItem() {
  const [slides, setSlides] = useState<SlideItem[]>([]);
  const [activeSlide, setActiveSlide] = useState(0);
  const [previousSlide, setPreviousSlide] = useState<number | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;
    setIsLoading(true);
    setError(null);

    async function loadPopular() {
      try {
        // Fetch orgs to map orgName if available
        const orgsRes = await getPublicOrganizations({ size: 50 });
        const orgMap: Record<string, string> = {};
        if (orgsRes?.data) {
          orgsRes.data.forEach((o) => {
            orgMap[o.id] = o.name;
          });
        }

        const res = await getPopularMerch();
        if (isActive) {
          if (res?.data && res.data.length > 0) {
            const loadedSlides = res.data.slice(0, 5).map((item) => {
              const orgName = orgMap[item.orgId] || "UIT Org";
              return {
                badge: item.categoryName || "Vật phẩm UIT",
                description:
                  item.description ||
                  `Vật phẩm chính thức từ ${orgName}. Thiết kế độc đáo mang đậm phong cách sinh viên trường Đại học Công nghệ Thông tin.`,
                image:
                  item.imageUrl ||
                  "https://placehold.co/900x900/e9feff/1a3a4a?font=montserrat&text=MERCH",
                subtitle:
                  item.price !== undefined && item.price > 0
                    ? currencyFormatter.format(item.price)
                    : "Miễn phí / Quà tặng",
                title: item.name,
              };
            });
            setSlides(loadedSlides);
          } else {
            setError("Hiện tại chưa có vật phẩm nổi bật nào được đăng tải.");
          }
        }
      } catch {
        if (isActive) {
          setError("Không thể kết nối đến máy chủ để tải vật phẩm nổi bật.");
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void loadPopular();

    return () => {
      isActive = false;
    };
  }, []);

  const activeProduct = slides[activeSlide];
  const previousProduct =
    previousSlide === null ? null : slides[previousSlide];

  const goToSlide = (nextSlide: number) => {
    if (nextSlide === activeSlide || !slides[nextSlide]) {
      return;
    }

    setPreviousSlide(activeSlide);
    setActiveSlide(nextSlide);
    window.setTimeout(() => setPreviousSlide(null), 560);
  };

  useEffect(() => {
    if (isPaused || slides.length <= 1) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      goToSlide((activeSlide + 1) % slides.length);
    }, slideDuration);

    return () => window.clearInterval(timer);
  }, [activeSlide, isPaused, slides.length]);

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
        {isLoading ? (
          <div className="flex min-h-[50vh] flex-col items-center justify-center gap-5 text-center">
            <div className="relative flex size-16 items-center justify-center">
              <div className="absolute inset-0 rounded-full border-4 border-white/20" />
              <div className="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-peach" />
            </div>
            <p className="font-sans text-sm font-semibold tracking-wide text-slate/70 animate-pulse">
              Đang tải danh sách vật phẩm nổi bật...
            </p>
          </div>
        ) : error || slides.length === 0 ? (
          <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
            <div className="mx-auto max-w-md rounded-[32px] border border-white/50 bg-white/30 p-8 shadow-glass backdrop-blur-xl">
              <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-peach/10 text-peach mb-4">
                ⚠
              </div>
              <p className="font-fredoka text-xl font-bold text-black-blue">
                {error || "Chưa có vật phẩm nổi bật nào."}
              </p>
              <p className="mt-3 font-sans text-sm leading-6 text-ink/65">
                Vui lòng kiểm tra lại kết nối mạng hoặc truy cập kho vật phẩm để xem toàn bộ danh mục sản phẩm.
              </p>
            </div>
          </div>
        ) : (
          <>
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
                      <p className="font-sans text-xs font-semibold uppercase tracking-[0.22em] text-slate/70 sm:text-sm">
                        Vật phẩm nổi bật
                      </p>
                      <h2 className="mt-5 whitespace-nowrap font-fredoka text-[36px] font-bold leading-[1.06] tracking-[0.01em] text-peach sm:text-[48px] lg:text-[54px] xl:text-[60px]">
                        {previousProduct.title}
                      </h2>
                      <p className="mt-1 font-sans text-xl font-semibold text-gold sm:text-2xl">
                        {previousProduct.subtitle}
                      </p>
                      <p className="mx-auto mt-10 max-w-[430px] font-sans text-sm font-normal leading-6 text-gray">
                        {previousProduct.description}
                      </p>
                    </div>
                  )}

                  {activeProduct && (
                    <div className="home-item-enter" key={`copy-${activeSlide}`}>
                      <p className="font-sans text-xs font-semibold uppercase tracking-[0.22em] text-slate/70 sm:text-sm">
                        Vật phẩm nổi bật
                      </p>
                      <h2 className="mt-5 whitespace-nowrap font-fredoka text-[36px] font-bold leading-[1.06] tracking-[0.01em] text-peach sm:text-[48px] lg:text-[54px] xl:text-[60px]">
                        {activeProduct.title}
                      </h2>
                      <p className="mt-1 font-sans text-xl font-semibold text-gold sm:text-2xl">
                        {activeProduct.subtitle}
                      </p>
                      <p className="mx-auto mt-10 max-w-[430px] font-sans text-sm font-normal leading-6 text-gray">
                        {activeProduct.description}
                      </p>
                    </div>
                  )}
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
                      <div className="home-item-exit rounded-full bg-white/70 px-4 py-2 font-sans text-xs font-semibold text-slate shadow-[0_12px_30px_rgba(82,128,145,0.14)]">
                        {previousProduct.badge}
                      </div>
                    )}
                    {activeProduct && (
                      <div
                        className="home-item-enter rounded-full bg-white/70 px-4 py-2 font-sans text-xs font-semibold text-slate shadow-[0_12px_30px_rgba(82,128,145,0.14)]"
                        key={`badge-${activeSlide}`}
                      >
                        {activeProduct.badge}
                      </div>
                    )}
                  </div>

                  <div className="absolute inset-0 grid place-items-center">
                    {previousProduct && (
                      <img
                        alt={previousProduct.title}
                        className="home-item-exit h-[72%] w-[72%] object-contain drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)]"
                        src={previousProduct.image}
                      />
                    )}
                    {activeProduct && (
                      <img
                        alt={activeProduct.title}
                        className="home-item-enter h-[72%] w-[72%] object-contain drop-shadow-[0_28px_35px_rgba(82,128,145,0.24)]"
                        key={`image-${activeSlide}`}
                        src={activeProduct.image}
                      />
                    )}
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
                {slides.map((product, index) => {
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
                      key={product.title + index}
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

              <HomeMoreLink ariaLabel="Xem thêm vật phẩm" to="/merch" />
            </div>
          </>
        )}
      </div>
    </section>
  );
}
