/**
 * Color palette from tech.md
 * All colors must strictly follow these codes
 */

export const colors = {
  // UI Base Colors
  background: '#14141e',
  surface: 'rgba(30, 30, 45, 0.6)',
  border: 'rgba(255, 255, 255, 0.1)',
  primary: {
    start: '#667eea',
    end: '#764ba2',
  },
  accent: '#00f2fe',
  text: {
    main: '#FFFFFF',
    muted: '#A0A0B0',
  },
  
  // Role Colors
  roles: {
    owner: {
      start: '#8B0000',
      end: '#FF0000',
      effect: 'shimmering',
      weight: 'bold',
    },
    curator: {
      start: '#FF4B4B',
      end: '#FF9E9E',
      effect: 'shimmering',
      weight: 'bold-italic',
    },
    admin: {
      color: '#FF6B6B',
      effect: 'none',
      weight: 'bold-italic',
    },
    moderator: {
      color: '#2ecc71',
      effect: 'none',
      weight: 'regular',
    },
    helper: {
      color: '#3498db',
      effect: 'none',
      weight: 'italic',
    },
    ulta: {
      start: '#a18cd1',
      end: '#fdc2ed',
      effect: 'none',
      weight: 'bold-italic-underline',
    },
    prime: {
      start: '#54daf4',
      end: '#545ed6',
      effect: 'none',
      weight: 'bold-italic',
    },
    boost: {
      start: '#f6d14a',
      end: '#862f51',
      effect: 'none',
      weight: 'italic',
    },
    player: {
      color: '#B0B0B0',
      effect: 'none',
      weight: 'regular',
    },
  },
} as const
