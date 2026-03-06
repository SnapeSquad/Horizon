/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Base UI Colors from tech.md
        background: '#14141e',
        surface: 'rgba(30, 30, 45, 0.6)',
        border: 'rgba(255, 255, 255, 0.1)',
        'primary-start': '#667eea',
        'primary-end': '#764ba2',
        accent: '#00f2fe',
        'text-main': '#FFFFFF',
        'text-muted': '#A0A0B0',
        
        // Role Colors
        'owner-start': '#8B0000',
        'owner-end': '#FF0000',
        'curator-start': '#FF4B4B',
        'curator-end': '#FF9E9E',
        admin: '#FF6B6B',
        moderator: '#2ecc71',
        helper: '#3498db',
        'ulta-start': '#a18cd1',
        'ulta-end': '#fdc2ed',
        'prime-start': '#54daf4',
        'prime-end': '#545ed6',
        'boost-start': '#f6d14a',
        'boost-end': '#862f51',
        player: '#B0B0B0',
      },
      backdropBlur: {
        glass: '50px',
      },
      fontFamily: {
        minecraft: ['Minecraft Unicode', 'monospace'],
        sans: ['Inter', 'SF Pro Display', 'sans-serif'],
      },
      animation: {
        shimmer: 'shimmer 3s linear infinite',
        pulse: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-200% center' },
          '100%': { backgroundPosition: '200% center' },
        },
        'pulse-glow': {
          '0%, 100%': { 
            boxShadow: '0 0 20px rgba(102, 126, 234, 0.5)',
          },
          '50%': { 
            boxShadow: '0 0 30px rgba(102, 126, 234, 0.8)',
          },
        },
      },
    },
  },
  plugins: [],
}
