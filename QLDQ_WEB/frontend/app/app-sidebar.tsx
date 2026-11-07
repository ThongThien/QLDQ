"use client";

import Link from "next/link";
import type { ReactNode } from "react";

export type SidebarItem = "overview" | "schedules" | "soldiers" | "work" | "area";

type SidebarIcon = "grid" | "calendar" | "users" | "file" | "pin" | "shield" | "arrow";

const ICONS: Record<SidebarIcon, ReactNode> = {
  grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
  users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,
  file: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 13h8M8 17h5"/></>,
  pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2"/></>,
  shield: <><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/></>,
  arrow: <path d="m9 18 6-6-6-6"/>,
};

function Icon({ name, size = 19 }: { name: SidebarIcon; size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{ICONS[name]}</svg>;
}

const commanderItems: Array<{ id: SidebarItem; label: string; icon: SidebarIcon; href: string }> = [
  { id: "overview", label: "Tổng quan", icon: "grid", href: "/?view=overview" },
  { id: "schedules", label: "Lịch gác", icon: "calendar", href: "/?view=schedules" },
  { id: "soldiers", label: "Chiến sĩ", icon: "users", href: "/?view=soldiers" },
  { id: "work", label: "Lịch công tác tuần", icon: "file", href: "/work-schedule" },
  { id: "area", label: "Theo dõi địa bàn", icon: "pin", href: "/area-monitoring" },
];

export function AppSidebar({ active, commander, open, onClose, onSelect }: { active: SidebarItem; commander: boolean; open: boolean; onClose: () => void; onSelect?: (item: "overview" | "schedules" | "soldiers") => void }) {
  const items = commander ? commanderItems : commanderItems.filter((item) => item.id !== "soldiers");
  return <>
    <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
      <div className="role-card"><div className="role-icon"><Icon name={commander ? "shield" : "users"}/></div><div><span>Đang thao tác với vai trò</span><strong>{commander ? "CHỈ HUY" : "CHIẾN SĨ"}</strong></div></div>
      <nav><span className="nav-label">{commander ? "TỔNG QUAN · QUẢN LÝ" : "NHIỆM VỤ CÁ NHÂN"}</span>{items.map((item) => {
        const content = <><Icon name={item.icon}/><span>{item.label}</span>{active === item.id && <Icon name="arrow" size={15}/>}</>;
        const local = item.id === "overview" || item.id === "schedules" || item.id === "soldiers";
        return <Link key={item.id} href={item.href} className={`sidebar-nav-link ${active === item.id ? "active" : ""}`} onClick={(event) => {
          if (local && onSelect) {
            event.preventDefault();
            window.history.pushState({}, "", item.href);
            onSelect(item.id as "overview" | "schedules" | "soldiers");
          }
          onClose();
        }}>{content}</Link>;
      })}</nav>
      <div className="sidebar-foot"><span>ĐƠN VỊ</span><strong>Trung đội Dân quân thường trực</strong><small>Ban CHQS phường Bắc Cam Ranh</small></div>
    </aside>
    {open && <button className="nav-backdrop" onClick={onClose} aria-label="Đóng menu"/>}
  </>;
}
