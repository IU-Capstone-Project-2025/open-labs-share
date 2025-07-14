import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "url";

export default defineConfig(({ command, mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '');
  
  return {
    plugins: [react()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      open: true,
      allowedHosts: ['*'],
    },
    define: {
      // Expose env variables to the client side
      'import.meta.env.VITE_API_GATEWAY_URL': JSON.stringify(env.VITE_API_GATEWAY_URL),
      'import.meta.env.VITE_ML_SERVICE_URL': JSON.stringify(env.VITE_ML_SERVICE_URL),
    },
  };
});
