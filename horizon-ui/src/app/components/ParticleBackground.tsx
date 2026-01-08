import { motion } from 'motion/react';
import { useMemo } from 'react';

interface Particle {
  id: number;
  x: number;
  y: number;
  size: number;
  duration: number;
  delay: number;
}

export default function ParticleBackground() {
  const particles = useMemo(() => {
    return Array.from({ length: 50 }, (_, i) => ({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 4 + 1,
      duration: Math.random() * 10 + 10,
      delay: Math.random() * 5,
    }));
  }, []);

  return (
    <div className="fixed inset-0 overflow-hidden pointer-events-none">
      {particles.map((particle) => (
        <motion.div
          key={particle.id}
          className="absolute rounded-full"
          style={{
            left: `${particle.x}%`,
            top: `${particle.y}%`,
            width: particle.size,
            height: particle.size,
            background: 'radial-gradient(circle, rgba(124, 77, 255, 0.8) 0%, rgba(124, 77, 255, 0) 70%)',
            filter: 'blur(1px)',
          }}
          animate={{
            y: [0, -100, 0],
            x: [0, Math.random() * 50 - 25, 0],
            opacity: [0, 0.6, 0],
            scale: [0.5, 1, 0.5],
          }}
          transition={{
            duration: particle.duration,
            repeat: Infinity,
            delay: particle.delay,
            ease: 'easeInOut',
          }}
        />
      ))}
      
      {/* Gradient overlays */}
      <div 
        className="absolute inset-0"
        style={{
          background: 'radial-gradient(circle at 20% 50%, rgba(124, 77, 255, 0.15) 0%, transparent 50%)',
        }}
      />
      <div 
        className="absolute inset-0"
        style={{
          background: 'radial-gradient(circle at 80% 50%, rgba(183, 148, 246, 0.1) 0%, transparent 50%)',
        }}
      />
    </div>
  );
}
