import { useEffect, useState } from "react";
import { BackgroundEffect } from "../ui";
import { ScrollDownButton } from "./ScrollDownButton";

const logoTitleUrl = "/assets/figma/logo-title.svg";
const logo20BackUrl = "/assets/figma/logo-20-back.png";
const logo20FrontUrl = "/assets/figma/logo-20-front.png";
const logo20EffectUrl = "/assets/figma/logo-20-effect.png";
const heroParagraph =
  "UITMerch là một Website mang đến một không gian lưu trữ những sản phầm lưu niệm của các câu lạc bộ và khoa tại Trường Đại học Công nghệ Thông tin. Tại đây bạn có thể tìm thấy những món đồ độc quyền, được thiết kế dành riêng cho cộng đồng UIT, giúp bạn thể hiện tình yêu và sự gắn kết với mái trường thân yêu của mình.";

function HeroAnniversaryLogo() {
  return (
    <div
      aria-label="UIT 20 years anniversary logo"
      className="hero-anniversary-logo"
      role="img"
    >
      <style>
        {`
          .hero-anniversary-logo {
            position: relative;
            display: grid;
            height: 100%;
            overflow: hidden;
            width: 100%;
            place-items: center;
          }

          .hero-anniversary-logo__stack {
            position: relative;
            height: 100%;
            width: 100%;
          }

          .hero-anniversary-logo__layer {
            pointer-events: none;
            position: absolute;
            left: 50%;
            top: 50%;
            max-width: none;
            object-fit: contain;
            transform: translate3d(-50%, -50%, 0);
            transform-origin: center center;
            transition:
              height 3000ms cubic-bezier(0.22, 1, 0.36, 1),
              left 3000ms cubic-bezier(0.22, 1, 0.36, 1),
              opacity 1250ms ease,
              top 3000ms cubic-bezier(0.22, 1, 0.36, 1),
              transform 3000ms cubic-bezier(0.22, 1, 0.36, 1),
              width 3000ms cubic-bezier(0.22, 1, 0.36, 1);
            user-select: none;
            will-change: height, left, top, transform, width;
          }

          .hero-anniversary-logo__layer--back {
            height: 75.18%;
            opacity: 0;
            width: 69.02%;
          }

          .hero-anniversary-logo__layer--front {
            height: 75.18%;
            opacity: 0;
            width: 69.02%;
          }

          .hero-anniversary-logo__layer--effect {
            height: 88.9%;
            opacity: 1;
            width: 77.8%;
          }

          .hero-anniversary-logo:hover .hero-anniversary-logo__layer--back,
          .hero-anniversary-logo--active .hero-anniversary-logo__layer--back {
            height: 75.18%;
            left: 12.53%;
            object-fit: cover;
            opacity: 1;
            top: 20.60%;
            transform: rotate(-33.69deg) scaleY(0.92) skewX(22.42deg);
            transform-origin: center center;
            width: 69.02%;
          }

          .hero-anniversary-logo:hover .hero-anniversary-logo__layer--front,
          .hero-anniversary-logo--active .hero-anniversary-logo__layer--front {
            height: 75.18%;
            left: 12.64%;
            object-fit: cover;
            opacity: 1;
            top: 12.53%;
            transform: rotate(-33.69deg) scaleY(0.92) skewX(22.42deg);
            transform-origin: center center;
            width: 69.02%;
          }

          .hero-anniversary-logo:hover .hero-anniversary-logo__layer--effect,
          .hero-anniversary-logo--active .hero-anniversary-logo__layer--effect {
            height: 75.18%;
            left: 18.43%;
            object-fit: cover;
            top: 0.52%;
            transform: rotate(-33.69deg) scaleY(0.92) skewX(22.42deg);
            transform-origin: center center;
            width: 70.94%;
          }

          @media (prefers-reduced-motion: reduce) {
            .hero-anniversary-logo__layer,
            .hero-anniversary-logo:hover .hero-anniversary-logo__layer,
            .hero-anniversary-logo--active .hero-anniversary-logo__layer {
              transition: none;
            }
          }
        `}
      </style>
      <div className="hero-anniversary-logo__stack">
        <img
          alt=""
          aria-hidden="true"
          className="hero-anniversary-logo__layer hero-anniversary-logo__layer--back"
          src={logo20BackUrl}
        />
        <img
          alt=""
          aria-hidden="true"
          className="hero-anniversary-logo__layer hero-anniversary-logo__layer--front"
          src={logo20FrontUrl}
        />
        <img
          alt=""
          aria-hidden="true"
          className="hero-anniversary-logo__layer hero-anniversary-logo__layer--effect"
          src={logo20EffectUrl}
        />
      </div>
    </div>
  );
}

export function HomeHero() {
  const [scale, setScale] = useState(1);
  const [viewportHeight, setViewportHeight] = useState(1024);
  const [viewportWidth, setViewportWidth] = useState(1440);

  useEffect(() => {
    const updateScale = () => {
      setViewportHeight(window.innerHeight);
      setViewportWidth(window.innerWidth);
      setScale(Math.min(window.innerWidth / 1440, 1));
    };

    updateScale();
    window.addEventListener("resize", updateScale);

    return () => window.removeEventListener("resize", updateScale);
  }, []);

  // RESPONSIVE
  const isResponsiveHero = viewportWidth < 1024;
  const desktopScale = scale * 0.9;
  const heroScale = isResponsiveHero ? 1 : desktopScale;
  const heroArtworkHeight = 718;
  const desktopHeroRowTop = Math.max(
    74,
    Math.round(viewportHeight / (2 * heroScale) - heroArtworkHeight / 2),
  );

  return (
    <section
      className="relative mx-0 min-h-[100svh] w-screen bg-canvas "
      id="home-hero"
      data-node-id="13:3814"
      data-section="home-hero"
      style={{ height: isResponsiveHero ? "auto" : 1024 * heroScale }}
    >
      {/* FIX: Bug1 */}
      <div className="pointer-events-none absolute inset-0 h-full w-full overflow-hidden">
        <BackgroundEffect />
      </div>

      <div
        className="relative mx-auto w-full max-w-canvas px-5 sm:px-8 lg:max-w-none lg:px-0"
        data-node-id="57:1326"
        data-name="slide 1"
        style={{
          height: isResponsiveHero ? "auto" : 1024 * heroScale,
          width: isResponsiveHero ? "100%" : 1440 * heroScale,
        }}
      >
        <div
          className="relative z-[1] grid w-full items-center gap-10 sm:grid-cols-[0.82fr_1.18fr] sm:gap-6 lg:absolute lg:left-0 lg:top-0 lg:block lg:h-[1024px] lg:w-[1440px] lg:origin-top-left"
          style={{
            transform: isResponsiveHero ? "none" : `scale(${heroScale})`,
          }}
        >
          <div
            className="contents lg:absolute lg:left-[120px] lg:flex lg:h-[718px] lg:w-[1292px] lg:items-center lg:gap-[37px]"
            style={{ top: isResponsiveHero ? undefined : desktopHeroRowTop }}
          >
            <div
              className="relative mx-auto flex w-full max-w-[472px] flex-col items-center text-center lg:mx-0 lg:w-[472px] lg:max-w-none lg:gap-[129px]"
              data-node-id="57:1325"
            >
              <img
                alt="UIT Merch"
                className="relative h-auto w-[200px] sm:w-[236px] lg:h-[228px] lg:w-[320px]"
                data-node-id="17:4228"
                src={logoTitleUrl}
              />
              <p
                className="relative mt-8 max-w-[472px] text-center font-google text-base leading-7 text-gray sm:mt-10 sm:text-[16px] lg:mt-0 lg:w-[472px] lg:text-[16px] lg:leading-[1.45]"
                data-node-id="17:4943"
              >
                {heroParagraph}
              </p>
            </div>

            <div
              className="relative mx-auto aspect-[1.09] w-full max-w-[520px] overflow-hidden sm:max-w-[600px] lg:mx-0 lg:h-[718px] lg:w-[783px] lg:max-w-none"
              data-node-id="28:12288"
            >
              <HeroAnniversaryLogo />
            </div>
          </div>
        </div>
      </div>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute bottom-0 left-0 right-0 h-40 bg-gradient-to-b from-transparent to-[#E9FEFF]"
      />
    </section>
  );
}
