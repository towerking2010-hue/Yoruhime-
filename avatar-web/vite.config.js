import { defineConfig } from 'vite';
import { resolve } from 'node:path';
export default defineConfig({
  base: './',
  publicDir: 'public',
  build: {
    outDir: '../reborn/src/main/assets/avatar',
    emptyOutDir: true,
    assetsInlineLimit: 0,
    sourcemap: false,
    rollupOptions: { output: { manualChunks: undefined } }
  }
});
