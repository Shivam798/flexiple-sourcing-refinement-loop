import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Produces a self-contained server bundle, so the runtime image carries only what it
  // needs instead of the whole node_modules tree.
  output: "standalone",
};

export default nextConfig;
