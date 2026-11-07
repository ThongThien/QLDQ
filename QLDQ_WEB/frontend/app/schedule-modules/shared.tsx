"use client";

import { FormEvent, ReactNode, useState } from "react";
import type { ModuleStatus } from "./types";
import { NotificationBell } from "../notification-bell";
import { SimulationControls } from "../simulation-controls";
import { BusinessClock } from "../business-clock";
import { AppSidebar } from "../app-sidebar";
import type { AuthUser } from "../api-client";
import { SiteFooter, UnitLogo } from "../unit-brand";

export function ScheduleStatusBadge({ status }: { status: ModuleStatus }) {
  return <span className={`module-status module-status-${status.toLowerCase()}`}><i />{status === "DRAFT" ? "Bản nháp" : "Đã công bố"}</span>;
}

export function WeekSelector() {
  return <div className="module-week-selector"><div><span>Dữ liệu đang hiển thị</span><strong>Theo lịch được chọn</strong></div></div>;
}

export function PdfDownloadButton({ onClick }: { onClick: () => void }) {
  return <button type="button" className="btn btn-outline" onClick={onClick}>↓ Tải xuống PDF</button>;
}

export function PdfPreviewButton({ onClick }: { onClick: () => void }) {
  return <button type="button" className="btn btn-outline" onClick={onClick}>▣ Xem trước PDF</button>;
}

export function NotificationPanel({ title, children }: { title: string; children: ReactNode }) {
  return <div className="module-notification"><span>✓</span><div><strong>{title}</strong><p>{children}</p></div></div>;
}

export function PublishConfirmModal({ title, busy = false, onCancel, onConfirm }: { title: string; busy?: boolean; onCancel: () => void; onConfirm: () => void }) {
  return <div className="modal-layer"><button className="modal-backdrop" disabled={busy} onClick={onCancel} aria-label="Đóng"/><div className="module-confirm"><div className="module-confirm-icon">!</div><h2>Xác nhận công bố</h2><p>{title}</p><small>Sau khi công bố, các chiến sĩ sẽ nhận được thông báo kiểm tra lịch.</small><div><button className="btn btn-secondary" disabled={busy} onClick={onCancel}>Hủy</button><button className="btn btn-primary" disabled={busy} onClick={onConfirm}>{busy ? "Đang công bố..." : "Công bố lịch"}</button></div></div></div>;
}

export const isMondayDate = (date: string) => Boolean(date) && new Date(`${date}T12:00:00+07:00`).getUTCDay() === 1;

export function WeekStartModal({ title, initialDate, busy = false, onCancel, onConfirm }: { title: string; initialDate: string; busy?: boolean; onCancel: () => void; onConfirm: (date: string) => void }) {
  const [date, setDate] = useState(initialDate);
  const [error, setError] = useState("");
  function submit(event: FormEvent) {
    event.preventDefault();
    if (!isMondayDate(date)) { setError("Ngày bắt đầu tuần bắt buộc phải là Thứ Hai."); return; }
    setError(""); onConfirm(date);
  }
  return <div className="modal-layer"><button className="modal-backdrop" disabled={busy} onClick={onCancel} aria-label="Đóng"/><form className="module-confirm week-start-modal" onSubmit={submit}><div className="module-confirm-icon">7</div><h2>{title}</h2><p>Chọn ngày bắt đầu tuần. Hệ thống chỉ chấp nhận Thứ Hai và không tạo trùng tuần.</p><label><span>Ngày bắt đầu tuần (Thứ Hai)</span><input required type="date" value={date} onChange={(event) => { setDate(event.target.value); setError(""); }}/></label>{error && <small className="field-error">{error}</small>}<div><button type="button" className="btn btn-secondary" disabled={busy} onClick={onCancel}>Hủy</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Đang tạo..." : "Tiếp tục"}</button></div></form></div>;
}

export function SchedulePreview({ children, onClose, onDownload }: { children: ReactNode; onClose: () => void; onDownload: () => void }) {
  const [zoom, setZoom] = useState(0.9);
  return <div className="modal-layer module-preview-layer"><button className="modal-backdrop" onClick={onClose} aria-label="Đóng"/><div className="module-preview" role="dialog" aria-modal="true" aria-label="Xem trước PDF"><div className="module-preview-toolbar"><div><strong>Xem trước PDF</strong><span>Bố cục theo biểu mẫu hành chính gốc</span></div><div className="preview-zoom"><button onClick={() => setZoom((value) => Math.max(.6, value - .1))} aria-label="Thu nhỏ">−</button><span>{Math.round(zoom * 100)}%</span><button onClick={() => setZoom((value) => Math.min(1.3, value + .1))} aria-label="Phóng to">+</button></div><div><PdfDownloadButton onClick={onDownload}/><button className="btn btn-secondary" onClick={onClose}>Đóng</button></div></div><div className="module-preview-scroll"><article id="pdf-print-target" className="admin-document" style={{ zoom }}>{children}</article></div></div></div>;
}

export function OperationsShell({ active, title, subtitle, businessTime, simulationEnabled, onTimeChanged, user, onLogout, children }: { active: "work" | "area"; title: string; subtitle: string; businessTime?: string; simulationEnabled?: boolean; onTimeChanged?: () => void; user: AuthUser | null; onLogout: () => void; children: ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const commander = user?.role === "COMMANDER";
  const initials = commander ? "CH" : user?.fullName.split(" ").slice(-2).map((word) => word[0]).join("") || "CS";
  return <div className="app-shell"><header className="topbar"><button className="mobile-menu" onClick={() => setMenuOpen((value) => !value)} aria-label="Mở menu">☰</button><UnitLogo compact/><div className="brand-copy"><strong>QUẢN LÝ TRUNG ĐỘI DÂN QUÂN</strong><span>Ban CHQS phường Bắc Cam Ranh · Dân quân thường trực</span></div><div className="topbar-spacer"/><BusinessClock value={businessTime} frozen={simulationEnabled} className="business-clock"/>{commander && <SimulationControls onChanged={onTimeChanged}/>}<NotificationBell/><div className="user-select-wrap"><div className="avatar">{initials}</div><div className="select-copy"><span>{commander ? "Chỉ huy · Ban CHQS phường" : "Chiến sĩ · Dân quân thường trực"}</span><strong>{user?.fullName || "Đang tải"}</strong></div></div><button className="logout-button" onClick={onLogout}>Đăng xuất</button></header><AppSidebar active={active} commander={commander} open={menuOpen} onClose={() => setMenuOpen(false)}/><main className="main-content operations-content"><div className="module-page-title"><div><span>{commander ? "ĐIỀU HÀNH NHIỆM VỤ" : "THÔNG TIN NHIỆM VỤ"}</span><h1>{title}</h1><p>{subtitle}</p></div><WeekSelector/></div>{children}</main><SiteFooter withSidebar/></div>;
}

export function ModuleToast({ children }: { children: ReactNode }) {
  return <div className="module-toast"><span>✓</span>{children}</div>;
}

export const formatModuleDate = (date: string) => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${date}T12:00:00+07:00`));
