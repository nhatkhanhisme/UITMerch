export function BackgroundEffect() {
  return (
    <div className="absolute inset-0 z-0 overflow-hidden" aria-hidden="true">
      <style>
        {`
          @keyframes backgroundEffectOrbitFluid {
            0%, 100% {
              transform: translate(-50%, -50%) rotate(0deg);
            }

            25% {
              transform: translate(-50%, -50%) translate(-15vw, 0) rotate(90deg);
            }

            50% {
              transform: translate(-50%, -50%) translate(-15vw, -12vh) rotate(180deg);
            }

            75% {
              transform: translate(-50%, -50%) translate(10vw, -12vh) rotate(270deg);
            }

            99.99% {
              transform: translate(-50%, -50%) rotate(360deg);
            }
          }
        `}
      </style>
      <div className="absolute inset-0 h-full w-full" data-node-id="13:4062">
        <div
          className="absolute h-[clamp(520px,80vmax,821px)] w-[clamp(530px,82vmax,835px)]"
          data-node-id="13:4031"
          style={{
            animation: "backgroundEffectOrbitFluid 6s linear infinite",
            left: "63.85%",
            top: "52.39%",
            transform: "translate(-50%, -50%) rotate(0deg)",
            transformOrigin: "center",
            willChange: "transform"
          }}
        >
          <img
            alt=""
            className="pointer-events-none absolute inset-[-30.45%_-29.94%] h-[160.9%] w-[159.88%] max-w-none"
            src="/assets/figma/background-effect-hero.svg"
          />
        </div>
      </div>
    </div>
  );
}
