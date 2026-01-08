import { useEffect, useRef } from 'react';
import { SkinViewer, WalkingAnimation } from 'skinview3d';

interface SkinViewer3DProps {
  username?: string;
  skinUrl?: string;
  width?: number;
  height?: number;
  rotation?: number;
}

export default function SkinViewer3D({ 
  username = 'Steve', 
  skinUrl,
  width = 400, 
  height = 550,
  rotation = 0
}: SkinViewer3DProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const viewerRef = useRef<SkinViewer | null>(null);

  useEffect(() => {
    if (!canvasRef.current) return;

    // Создаем SkinViewer
    const viewer = new SkinViewer({
      canvas: canvasRef.current,
      width,
      height,
      skin: skinUrl || `https://crafatar.com/skins/${username}`,
      enableZoom: false,
      enableRotate: false,
    });

    // Настройки камеры
    viewer.camera.position.set(0, 15, 40);
    viewer.camera.lookAt(0, 15, 0);

    // Анимация ходьбы
    viewer.animation = new WalkingAnimation();
    viewer.animation.speed = 0.5;
    
    // Автоматическое вращение
    viewer.autoRotate = true;
    viewer.autoRotateSpeed = 1;

    // Освещение
    viewer.scene.add(new (window as any).THREE.AmbientLight(0xffffff, 0.6));
    const light = new (window as any).THREE.DirectionalLight(0xffffff, 0.8);
    light.position.set(50, 50, 50);
    viewer.scene.add(light);

    viewerRef.current = viewer;

    return () => {
      viewer.dispose();
    };
  }, [username, skinUrl, width, height]);

  useEffect(() => {
    if (viewerRef.current) {
      viewerRef.current.playerObject.rotation.y = (rotation * Math.PI) / 180;
    }
  }, [rotation]);

  return (
    <canvas 
      ref={canvasRef} 
      style={{ 
        cursor: 'grab',
        borderRadius: '20px',
        boxShadow: '0 20px 60px rgba(124, 77, 255, 0.4)'
      }}
    />
  );
}

