import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      maxWidth: {
        canvas: "1440px"
      },
      colors: {
        canvas: "#E7F5FA",
        gray: "#475569",
        "black-blue": "#131B2E",
        "dark-gray": "#00000033",
        ink: "#1D2023",
        slate: "#394454",
        peach: "#F8BF87",
        gold: "#F8D987",
        aqua: "#92FBFF"
      },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', "sans-serif"],
        fredoka: ["Fredoka", "sans-serif"],
        brand: ["Fredoka", "sans-serif"],
        google: ['"Be Vietnam Pro"', "sans-serif"],
        display: ["Fredoka", "sans-serif"],
        condensed: ['"Be Vietnam Pro"', "sans-serif"]
      },
      spacing: {
        canvas: "1440px",
        nav: "60px",
        "nav-x": "33px",
        "nav-y": "11px"
      },
      borderRadius: {
        glass: "50px",
        panel: "40px"
      },
      boxShadow: {
        glass: "-11.15px -10.392px 48px -12px rgba(0, 0, 0, 0.15)",
        "glass-inset":
          "inset 2.351px 2.191px 10.12px rgba(255, 255, 255, 0.12), inset 1.422px 1.325px 5.06px rgba(255, 255, 255, 0.12)"
      },
      backgroundImage: {
        "brand-gradient": "linear-gradient(90deg, #F8D987 0%, #92FBFF 100%)"
      }
    }
  },
  plugins: []
};

export default config;
