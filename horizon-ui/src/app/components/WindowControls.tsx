import { Minus, Square, X } from 'lucide-react';
import { motion } from 'motion/react';

interface WindowControlsProps {
  onMinimize?: () => void;
  onMaximize?: () => void;
  onClose?: () => void;
}

// Check if electronAPI is available
const isElectron = typeof window !== 'undefined' && (window as any).electronAPI;

export default function WindowControls({ onMinimize, onMaximize, onClose }: WindowControlsProps) {
  const handleMinimize = () => {
    if (isElectron) {
      (window as any).electronAPI.minimizeWindow();
    } else if (onMinimize) {
      onMinimize();
    } else {
      console.log('Minimize clicked (no handler)');
    }
  };
  
  const handleMaximize = () => {
    if (isElectron) {
      (window as any).electronAPI.maximizeWindow();
    } else if (onMaximize) {
      onMaximize();
    } else {
      console.log('Maximize clicked (no handler)');
    }
  };
  
  const handleClose = () => {
    if (isElectron) {
      (window as any).electronAPI.closeWindow();
    } else if (onClose) {
      onClose();
    } else {
      console.log('Close clicked (no handler)');
    }
  };
  
  return (
    <div className="flex items-center gap-2">
      <motion.button
        whileHover={{ scale: 1.1, backgroundColor: 'rgba(255, 255, 255, 0.1)' }}
        whileTap={{ scale: 0.95 }}
        onClick={handleMinimize}
        className="w-10 h-10 rounded-lg flex items-center justify-center transition-all"
        style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
      >
        <Minus className="w-5 h-5" style={{ color: '#E0E0E0' }} />
      </motion.button>
      
      <motion.button
        whileHover={{ scale: 1.1, backgroundColor: 'rgba(255, 255, 255, 0.1)' }}
        whileTap={{ scale: 0.95 }}
        onClick={handleMaximize}
        className="w-10 h-10 rounded-lg flex items-center justify-center transition-all"
        style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
      >
        <Square className="w-4 h-4" style={{ color: '#E0E0E0' }} />
      </motion.button>
      
      <motion.button
        whileHover={{ scale: 1.1, backgroundColor: 'rgba(239, 68, 68, 0.2)' }}
        whileTap={{ scale: 0.95 }}
        onClick={handleClose}
        className="w-10 h-10 rounded-lg flex items-center justify-center transition-all group"
        style={{ backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
      >
        <X className="w-5 h-5 group-hover:text-red-400" style={{ color: '#E0E0E0' }} />
      </motion.button>
    </div>
  );
}
