import { Suspense } from 'react';
import { ShaderGradientCanvas, ShaderGradient } from '@shadergradient/react';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const GradientComponent = ShaderGradient as any;

export function ShaderBackground() {
    return (
        // Dùng Suspense với fallback là màu nền tĩnh mờ để tránh đơ lúc load
        <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
            <ShaderGradientCanvas
                style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    width: '100vw',
                    height: '100vh',
                    zIndex: 0,
                    pointerEvents: 'none'
                }}
            >
                <GradientComponent
                    animate="on"
                    axesHelper="off"
                    brightness={1.6}
                    cAzimuthAngle={180}
                    cDistance={2.8}
                    cPolarAngle={80}
                    cameraZoom={9.1}
                    color1="#92FBFF"
                    color2="#f8e697"
                    color3="#ffffff"
                    destination="onCanvas"
                    embedMode="off"
                    envPreset="city"
                    format="gif"
                    fov={45}
                    frameRate={10}
                    gizmoHelper="hide"
                    grain="off"
                    lightType="3d"
                    pixelDensity={1}
                    positionX={0}
                    positionY={0}
                    positionZ={0}
                    range="enabled"
                    rangeEnd={40}
                    rangeStart={0}
                    reflection={0.1}
                    rotationX={50}
                    rotationY={0}
                    rotationZ={-60}
                    shader="defaults"
                    type="waterPlane"
                    uAmplitude={0}
                    uDensity={1.5}
                    uFrequency={0}
                    uSpeed={0.3}
                    uStrength={1.5}
                    uTime={8}
                    wireframe={false}
                />
            </ShaderGradientCanvas>
        </Suspense>
    );
}