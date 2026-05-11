import { useEffect, useState } from "react";
import { BackgroundEffect } from "../ui";
import { ScrollDownButton } from "./ScrollDownButton";

const logoTitleUrl = "/assets/figma/logo-title.svg";
const logo20FrontUrl = "/assets/figma/logo-20-front.png";
const logo20EffectUrl = "/assets/figma/logo-20-effect.png";
const HIDE_HERO_DARK_LAYER = true;
const heroParagraph =
  "Rhoncus morbi et augue nec, in id ullamcorper at sit. Condimentum sit nunc in eros scelerisque sed. Commodo in viverra nunc, ullamcorper ut. Non, amet, aliquet scelerisque nullam sagittis, pulvinar. Fermentum scelerisque sit consectetur hac mi. Mollis leo eleifend ultricies purus iaculis. Rhoncus morbi et augue nec, in id ullamcorper at sit. Condimentum sit nunc in eros scelerisque sed. Commodo in viverra nunc, ullamcorper ut. Non, amet, aliquet scelerisque nullam sagittis, pulvinar. Fermentum scelerisque sit consectetur hac mi. Mollis leo eleifend ultricies purus iaculis.";

export function HomeHero() {
  const [scale, setScale] = useState(1);
  const [viewportWidth, setViewportWidth] = useState(1440);

  useEffect(() => {
    const updateScale = () => {
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
            className="relative mx-auto flex w-full max-w-[472px] flex-col items-center text-center lg:absolute lg:left-[71px] lg:top-[189px] lg:block lg:h-[621px] lg:w-[472px] lg:max-w-none"
            data-node-id="57:1325"
          >
            <img
              alt="UIT Merch"
              className="relative h-auto w-[200px] sm:w-[236px] lg:absolute lg:left-[79px] lg:top-0 lg:h-[228px] lg:w-[320px]"
              data-node-id="17:4228"
              src={logoTitleUrl}
            />
            <p
              className="relative mt-8 max-w-[472px] text-center font-google text-sm leading-6 text-gray sm:mt-10 sm:text-[15px] lg:absolute lg:left-0 lg:top-[357px] lg:mt-0 lg:h-[264px] lg:w-[472px] lg:text-[15px] lg:leading-[1.35]"
              data-node-id="17:4943"
            >
              {heroParagraph}
            </p>
          </div>

          <div
            className="relative mx-auto aspect-[1.09] w-full max-w-[560px] overflow-hidden sm:max-w-[640px] lg:absolute lg:left-[511px] lg:top-[62px] lg:h-[818px] lg:w-[891px] lg:max-w-none"
            data-node-id="28:12288"
          >
            {!HIDE_HERO_DARK_LAYER && (
              <img
                alt=""
                className="pointer-events-none absolute left-[128px] top-[90px] size-[615px] object-cover"
                src={logo20FrontUrl}
              />
            )}
            <div className="pointer-events-none absolute left-1/2 top-1/2 h-[82%] w-[84%] -translate-x-1/2 -translate-y-1/2 overflow-hidden lg:left-[131px] lg:top-[113px] lg:h-[613px] lg:w-[630px] lg:translate-x-0 lg:translate-y-0">
              <img
                alt="UIT 20 years anniversary"
                className="absolute left-[-62.9%] top-[-7.78%] h-[107.76%] w-[385.95%] max-w-none"
                src={logo20EffectUrl}
              />
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
