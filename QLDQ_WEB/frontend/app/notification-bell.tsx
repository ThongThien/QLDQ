"use client";

import { useEffect, useRef, useState } from "react";
import { api, ApiError, type NotificationItem } from "./api-client";
import { useVisibilityPolling } from "./use-visibility-polling";
import { useRouter } from "next/navigation";

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const rootRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  async function refreshCount() {
    try {
      const result = await api.notifications.unreadCount();
      setUnread(result.unreadCount);
    } catch (cause) {
      if (!(cause instanceof ApiError && cause.status === 401)) setError("Không thể cập nhật thông báo.");
    }
  }

  useEffect(() => { queueMicrotask(() => void refreshCount()); }, []);
  useVisibilityPolling(refreshCount);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (!next) return;
    setLoading(true); setError("");
    try {
      const result = await api.notifications.list();
      setItems(result.content);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải thông báo.");
    } finally { setLoading(false); }
  }

  async function markAll() {
    try {
      await api.notifications.markAllRead();
      const readAt = new Date().toISOString();
      setItems((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? readAt })));
      setUnread(0);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể cập nhật thông báo."); }
  }

  async function openNotification(item: NotificationItem) {
    try {
      if (!item.readAt) {
        await api.notifications.markRead(item.id);
        setUnread((value) => Math.max(0, value - 1));
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể cập nhật thông báo.");
      return;
    }
    if (item.title === "Nhiệm vụ đã được gỡ khỏi lịch") {
      setItems((current) => current.map((candidate) => candidate.id === item.id ? { ...candidate, readAt: candidate.readAt ?? new Date().toISOString() } : candidate));
      return;
    }
    if (item.referenceType === "GUARD_SHIFT") router.push(`/?view=schedules&guardShift=${item.referenceId}`);
    else router.push(`/?workItem=${item.referenceId}`);
  }

  return <div className="notification-center" ref={rootRef}>
    <button type="button" className="notification" aria-label={`Thông báo, ${unread} chưa đọc`} aria-expanded={open} onClick={() => void toggle()}>
      <span className="notification-bell-icon" aria-hidden="true">🔔</span>
      {unread > 0 && <b>{unread > 99 ? "99+" : unread}</b>}
    </button>
    {open && <section className="notification-panel" aria-label="Danh sách thông báo">
      <header><div><strong>Thông báo</strong><span>{unread} chưa đọc</span></div>{unread > 0 && <button type="button" onClick={() => void markAll()}>Đọc tất cả</button>}</header>
      <div className="notification-list">
        {loading && <p className="notification-state">Đang tải...</p>}
        {!loading && error && <p className="notification-state error">{error}</p>}
        {!loading && !error && !items.length && <p className="notification-state">Chưa có thông báo.</p>}
        {!loading && items.map((item) => <button type="button" key={item.id} className={`notification-item ${item.readAt ? "read" : "unread"}`} onClick={() => void openNotification(item)}>
          <i/>
          <span><strong>{item.title}</strong><small>{item.content}</small><time>{formatTime(item.createdAt)}</time></span>
        </button>)}
      </div>
    </section>}
  </div>;
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" }).format(new Date(value));
}
