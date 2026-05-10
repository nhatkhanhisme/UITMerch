import { HomeEnd } from "../components/home/HomeEnd";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { HomeFixedChrome } from "../components/home/HomeFixedChrome";
import { HomeHero } from "../components/home/HomeHero";
import { HomeItem } from "../components/home/HomeItem";
import { HomeOrgan } from "../components/home/HomeOrgan";
import { HomeWhy } from "../components/home/HomeWhy";

export function HomePage() {
  return (
    <>
      <HomeFixedChrome />
      <HomeHero />
      <div className="relative isolate">
        <AmbientBackgroundGradients />

        <HomeItem />
        <HomeOrgan />
        {/* <HomeWhy /> */}
        <HomeEnd />
      </div>
    </>
  );
}
