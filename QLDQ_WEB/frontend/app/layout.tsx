import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

const roboto = localFont({
  src: [
    { path: "./fonts/Roboto-Regular.ttf", weight: "400", style: "normal" },
    { path: "./fonts/Roboto-Bold.ttf", weight: "700", style: "normal" },
  ],
  display: "swap",
  variable: "--font-roboto",
});

export const metadata: Metadata = {
  title: "Quản lý Trung đội Dân quân | Ban CHQS phường Bắc Cam Ranh",
  description: "Hệ thống quản lý quân số, lịch công tác, địa bàn và nhiệm vụ của Ban CHQS Phường Bắc Cam Ranh.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return <html lang="vi" className={roboto.variable}><body>{children}</body></html>;
}
