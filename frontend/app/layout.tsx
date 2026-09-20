import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

/**
 * Apple platforms resolve to SF Pro through `-apple-system` in the theme's font stack and
 * never download anything. Inter is the fallback for everyone else — it shares SF Pro's
 * proportions closely enough that the layout does not shift between them.
 */
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:3000"),
  title: "Shortlist",
  // Matches the page background so the browser chrome blends into the app on mobile.
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f5f5f7" },
    { media: "(prefers-color-scheme: dark)", color: "#000000" },
  ],
  description:
    "Describe who you want to hire, then refine the search in conversation until the shortlist is right.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  // The font variable goes on <html>, not <body>. Tailwind resolves --font-sans at :root, and
  // a custom property referencing one declared further down the tree is invalid at
  // computed-value time — which silently drops the typeface back to the system default.
  return (
    <html lang="en" className={inter.variable}>
      <body className="antialiased">{children}</body>
    </html>
  );
}
