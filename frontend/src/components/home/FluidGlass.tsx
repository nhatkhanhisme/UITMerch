/* eslint-disable react/no-unknown-property */
import * as THREE from "three";
import { memo, Suspense, useEffect, useMemo, useRef, useState } from "react";
import { Canvas, createPortal, useFrame, useThree } from "@react-three/fiber";
import { MeshTransmissionMaterial } from "@react-three/drei";
import { easing } from "maath";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";

type Mode = "lens" | "bar" | "cube";
type BarPosition = "top" | "bottom";

type MaterialModeProps = {
  scale?: number;
  ior?: number;
  thickness?: number;
  anisotropy?: number;
  chromaticAberration?: number;
  transmission?: number;
  roughness?: number;
  color?: string;
  attenuationColor?: string;
  attenuationDistance?: number;
};

type BarProps = MaterialModeProps & {
  position?: BarPosition;
  yOffset?: number;
  showBufferPlane?: boolean;
  backgroundOpacity?: number;
};

type FluidGlassProps = {
  mode?: Mode;
  lensProps?: MaterialModeProps;
  barProps?: BarProps;
  cubeProps?: MaterialModeProps;
} & MaterialModeProps & {
    position?: BarPosition;
  };

type ModeWrapperProps = {
  glb: string;
  geometryKey: string;
  lockPosition?: BarPosition;
  yOffset?: number;
  showBufferPlane?: boolean;
  backgroundOpacity?: number;
  followPointer?: boolean;
  modeProps?: MaterialModeProps;
};

type LoadedModel = {
  geometry: THREE.BufferGeometry;
};

function useModel(glbPath: string, geometryKey: string): LoadedModel | null {
  const [model, setModel] = useState<LoadedModel | null>(null);

  useEffect(() => {
    let mounted = true;
    const loader = new GLTFLoader();

    loader.load(
      glbPath,
      (gltf) => {
        if (!mounted) return;
        const node = gltf.scene.getObjectByName(geometryKey) as THREE.Mesh | null;
        const geometry = node?.geometry?.clone();
        if (!geometry) return;
        geometry.computeBoundingBox();
        setModel({ geometry });
      },
      undefined,
      () => {
        if (mounted) {
          setModel(null);
        }
      },
    );

    return () => {
      mounted = false;
    };
  }, [geometryKey, glbPath]);

  return model;
}

export default function FluidGlass({
  mode = "lens",
  lensProps = {},
  barProps = {},
  cubeProps = {},
  position,
  ...directProps
}: FluidGlassProps) {
  const Wrapper = mode === "bar" ? Bar : mode === "cube" ? Cube : Lens;
  const baseModeProps = mode === "bar" ? barProps : mode === "cube" ? cubeProps : lensProps;
  const modeProps: BarProps =
    mode === "bar"
      ? { ...baseModeProps, ...directProps, position: position ?? (baseModeProps as BarProps).position }
      : { ...baseModeProps, ...directProps };

  return (
    <Canvas
      camera={{ position: [0, 0, 20], fov: 15 }}
      gl={{ alpha: true, antialias: true }}
      style={{ background: "transparent" }}
      dpr={[1, 1.5]}
      onCreated={({ gl }) => {
        gl.setClearColor(0x000000, 0);
      }}
    >
      <Suspense fallback={null}>
        <Wrapper modeProps={modeProps} />
      </Suspense>
    </Canvas>
  );
}

const ModeWrapper = memo(function ModeWrapper({
  glb,
  geometryKey,
  lockPosition = "bottom",
  yOffset = 0,
  showBufferPlane = true,
  backgroundOpacity = 1,
  followPointer = true,
  modeProps = {},
}: ModeWrapperProps) {
  const ref = useRef<THREE.Mesh>(null);
  const model = useModel(glb, geometryKey);
  const buffer = useFBOCompat();
  const { viewport: vp } = useThree();
  const [scene] = useState(() => new THREE.Scene());
  const geoWidthRef = useRef(1);

  useEffect(() => {
    if (!model?.geometry?.boundingBox) return;
    const box = model.geometry.boundingBox;
    geoWidthRef.current = box ? box.max.x - box.min.x || 1 : 1;
  }, [model]);

  useFrame((state, delta) => {
    if (!ref.current) return;

    const { gl, viewport, pointer, camera } = state;
    const v = viewport.getCurrentViewport(camera, [0, 0, 15]);
    const destX = followPointer ? (pointer.x * v.width) / 2 : 0;
    const baseY =
      lockPosition === "top"
        ? v.height / 2 - 0.2
        : lockPosition === "bottom"
          ? -v.height / 2 + 0.2
          : followPointer
            ? (pointer.y * v.height) / 2
            : 0;
    const destY = baseY + yOffset;

    easing.damp3(ref.current.position, [destX, destY, 15], 0.15, delta);

    if (modeProps.scale == null) {
      const maxWorld = v.width * 0.9;
      const desired = maxWorld / geoWidthRef.current;
      ref.current.scale.setScalar(Math.min(0.15, desired));
    }

    gl.setRenderTarget(buffer);
    gl.setClearColor(0x000000, 0);
    gl.clear(true, true, true);
    gl.render(scene, camera);
    gl.setRenderTarget(null);
    gl.setClearColor(0x000000, 0);
  });

  const { scale, ior, thickness, anisotropy, chromaticAberration, ...extraMat } = modeProps;

  return (
    <>
      {createPortal(<BackgroundPlane opacity={backgroundOpacity} />, scene)}
      {showBufferPlane && (
        <mesh scale={[vp.width, vp.height, 1]}>
          <planeGeometry />
          <meshBasicMaterial map={buffer.texture} transparent depthWrite={false} />
        </mesh>
      )}
      {model?.geometry && (
        <mesh ref={ref} scale={scale ?? 0.15} rotation-x={Math.PI / 2} geometry={model.geometry}>
          <MeshTransmissionMaterial
            buffer={buffer.texture}
            ior={ior ?? 1.15}
            thickness={thickness ?? 5}
            anisotropy={anisotropy ?? 0.01}
            chromaticAberration={chromaticAberration ?? 0.1}
            {...extraMat}
          />
        </mesh>
      )}
    </>
  );
});

function Lens({ modeProps }: { modeProps?: MaterialModeProps }) {
  return (
    <ModeWrapper glb="/assets/3d/lens.glb" geometryKey="Cylinder" followPointer={false} modeProps={modeProps} />
  );
}

function BackgroundPlane({ opacity = 1 }: { opacity?: number }) {
  const color = useMemo(() => new THREE.Color("#e7f5fa"), []);
  return (
    <mesh position={[0, 0, 0]}>
      <planeGeometry args={[50, 50]} />
      <meshBasicMaterial color={color} transparent opacity={opacity} depthWrite={false} />
    </mesh>
  );
}

function Cube({ modeProps }: { modeProps?: MaterialModeProps }) {
  return <ModeWrapper glb="/assets/3d/cube.glb" geometryKey="Cube" followPointer modeProps={modeProps} />;
}

function Bar({ modeProps = {} }: { modeProps?: BarProps }) {
  const defaultMat: MaterialModeProps = {
    transmission: 1,
    roughness: 0,
    thickness: 10,
    ior: 1.15,
    color: "#ffffff",
    attenuationColor: "#ffffff",
    attenuationDistance: 0.25,
  };

  const {
    position = "bottom",
    yOffset = 0,
    showBufferPlane = false,
    backgroundOpacity = 0,
    ...barMat
  } = modeProps;

  return (
    <ModeWrapper
      glb="/assets/3d/bar.glb"
      geometryKey="Cube"
      lockPosition={position}
      yOffset={yOffset}
      showBufferPlane={showBufferPlane}
      backgroundOpacity={backgroundOpacity}
      followPointer={false}
      modeProps={{ ...defaultMat, ...barMat }}
    />
  );
}

function useFBOCompat() {
  const { gl, size } = useThree();
  const target = useMemo(
    () =>
      new THREE.WebGLRenderTarget(size.width, size.height, {
        depthBuffer: true,
        stencilBuffer: false,
      }),
    [size.height, size.width],
  );

  useEffect(() => {
    target.setSize(size.width, size.height);
    return () => target.dispose();
  }, [size.height, size.width, target]);

  useEffect(() => {
    gl.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
  }, [gl]);

  return target;
}
