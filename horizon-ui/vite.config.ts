import { defineConfig } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  // КРИТИЧЕСКИ ВАЖНО для file:// протокола в WebView!
  base: './',
  
  plugins: [
    // The React and Tailwind plugins are both required for Make, even if
    // Tailwind is not being actively used – do not remove them
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      // Alias @ to the src directory
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    // Генерировать манифест для отладки
    manifest: true,
    // Не использовать относительные пути для динамических импортов
    assetsInlineLimit: 0,
    rollupOptions: {
      output: {
        // Стабильные имена файлов для отладки
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name].[ext]'
      }
    }
  }
})
