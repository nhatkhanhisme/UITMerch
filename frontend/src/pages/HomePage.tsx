import { useEffect } from "react";
import { HomeEnd } from "../components/home/HomeEnd";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { HomeFixedChrome } from "../components/home/HomeFixedChrome";
import { HomeHero } from "../components/home/HomeHero";
import { HomeItem } from "../components/home/HomeItem";
import { HomeOrgan } from "../components/home/HomeOrgan";
import { HomeWhy } from "../components/home/HomeWhy";

export function HomePage() {
  useEffect(() => {
    document.documentElement.classList.add("home-hide-native-scrollbar");
    document.body.classList.add("home-hide-native-scrollbar");

    return () => {
      document.documentElement.classList.remove("home-hide-native-scrollbar");
      document.body.classList.remove("home-hide-native-scrollbar");
    };
  }, []);

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
