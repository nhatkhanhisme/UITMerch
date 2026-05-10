import { HomeEnd } from "../components/home/HomeEnd";
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
      <HomeItem />
      <HomeOrgan />
      {/* <HomeWhy /> */}
      <HomeEnd />
    </>
  );
}
