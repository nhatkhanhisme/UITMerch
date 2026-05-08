import { useEffect, useState } from "react";
import { BackgroundEffect } from "../ui";
import { ScrollDownButton } from "./ScrollDownButton";
import { SlideBar } from "./SlideBar";
import { TopNavBar } from "./TopNavBar";

const logoTitleUrl =
  "/assets/figma/logo-title.svg";
const logo20FrontUrl = "/assets/figma/logo-20-front.png";
const logo20EffectUrl =
  "/assets/figma/logo-20-effect.png";
const heroParagraph =
  "Rhoncus morbi et augue nec, in id ullamcorper at sit. Condimentum sit nunc in eros scelerisque sed. Commodo in viverra nunc, ullamcorper ut. Non, amet, aliquet scelerisque nullam sagittis, pulvinar. Fermentum scelerisque sit consectetur hac mi. Mollis leo eleifend ultricies purus iaculis. Rhoncus morbi et augue nec, in id ullamcorper at sit. Condimentum sit nunc in eros scelerisque sed. Commodo in viverra nunc, ullamcorper ut. Non, amet, aliquet scelerisque nullam sagittis, pulvinar. Fermentum scelerisque sit consectetur hac mi. Mollis leo eleifend ultricies purus iaculis.";

export function HomeHero() {
  const [scale, setScale] = useState(1);

  useEffect(() => {
    const updateScale = () => setScale(Math.min(window.innerWidth / 1440, 1));

    updateScale();
    window.addEventListener("resize", updateScale);

    return () => window.removeEventListener("resize", updateScale);
  }, []);

  return (
    <section
      className="relative w-full overflow-hidden bg-canvas"
      data-node-id="13:3814"
      data-section="home-hero"
      style={{ height: 1024 * scale }}
    >
      <div
        className="relative mx-auto"
        data-node-id="57:1326"
        data-name="slide 1"
        style={{ height: 1024 * scale, width: 1440 * scale }}
      >
        <div
          className="absolute left-0 top-0 h-[1024px] w-[1440px] origin-top-left overflow-hidden"
          style={{ transform: `scale(${scale})` }}
        >
          <BackgroundEffect />
        </div>

        <div
          className="absolute left-0 top-0 z-[1] h-[1024px] w-[1440px] origin-top-left"
          style={{ transform: `scale(${scale})` }}
        >
          <div className="absolute left-[80px] top-[18px] z-10">
            <TopNavBar />
          </div>
          <div className="absolute left-[1326px] top-[233px]">
            <SlideBar activeIndex={0} />
          </div>

          <div
            className="absolute left-[71px] top-[189px] h-[621px] w-[472px]"
            data-node-id="57:1325"
          >
            <img
              alt="UIT Merch"
              className="absolute left-[79px] top-0 h-[243px] w-[340px]"
              data-node-id="17:4228"
              src={logoTitleUrl}
            />
            <p
              className="absolute left-0 top-[357px] h-[264px] w-[472px] text-center font-google text-[16px] leading-[1.4] text-gray"
              data-node-id="17:4943"
            >
              {heroParagraph}
            </p>
          </div>

          <div
            className="absolute left-[511px] top-[62px] h-[818px] w-[891px] overflow-hidden"
            data-node-id="28:12288"
          >
            <img
              alt=""
              className="pointer-events-none absolute left-[128px] top-[90px] size-[615px] object-cover"
              src={logo20FrontUrl}
            />
            <div className="pointer-events-none absolute left-[131px] top-[113px] h-[613px] w-[630px] overflow-hidden">
              <img
                alt="UIT 20 years anniversary"
                className="absolute left-[-62.9%] top-[-7.78%] h-[107.76%] w-[385.95%] max-w-none"
                src={logo20EffectUrl}
              />
            </div>
          </div>

          <ScrollDownButton className="absolute left-[683px] top-[860px]" />
        </div>
      </div>
    </section>
  );
}
