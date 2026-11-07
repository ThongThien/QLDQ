"use client";

import { useEffect, useState } from "react";
import { api, ApiError, type TimeSimulationStatus } from "./api-client";

export function SimulationControls({ onChanged }: { onChanged?: () => void }) {
  const [status, setStatus] = useState<TimeSimulationStatus | null>(null);
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (process.env.NODE_ENV !== "development") return;
    api.timeSimulation.status()
      .then((result) => { setStatus(result); setValue(result.currentTime.slice(0, 16)); })
      .catch((cause) => { if (!(cause instanceof ApiError) || ![403, 404].includes(cause.status)) setError("Không thể đọc thời gian mô phỏng."); });
  }, []);

  if (process.env.NODE_ENV !== "development" || (!status && !error)) return null;

  async function execute(action: () => Promise<TimeSimulationStatus>) {
    setBusy(true); setError("");
    try {
      const next = await action();
      setStatus(next); setValue(next.currentTime.slice(0, 16)); onChanged?.();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể thay đổi thời gian mô phỏng.");
    } finally { setBusy(false); }
  }

  return <div className="simulation-center">
    {status?.enabled && <div className="simulation-banner"><strong>SIMULATION MODE</strong><span>{formatSimulationTime(status.currentTime)}</span></div>}
    <button type="button" className={status?.enabled ? "simulation-trigger active" : "simulation-trigger"} onClick={() => setOpen((current) => !current)}>
      ◷ <span>{status?.enabled ? "Đang mô phỏng" : "Mô phỏng thời gian"}</span>
    </button>
    {open && <div className="simulation-panel">
      <header><div><strong>Development time</strong><span>Asia/Ho_Chi_Minh</span></div><button onClick={() => setOpen(false)}>×</button></header>
      {error && <p className="simulation-error">{error}</p>}
      <label><span>Ngày giờ mô phỏng</span><input type="datetime-local" value={value} onChange={(event) => setValue(event.target.value)}/></label>
      <button className="btn btn-primary" disabled={busy || !value} onClick={() => void execute(() => api.timeSimulation.set(`${value}:00+07:00`))}>Bật / đặt thời gian</button>
      <div className="simulation-steps"><button disabled={busy || !status?.enabled} onClick={() => void execute(() => api.timeSimulation.advance(-1, 0))}>−1 giờ</button><button disabled={busy || !status?.enabled} onClick={() => void execute(() => api.timeSimulation.advance(1, 0))}>+1 giờ</button><button disabled={busy || !status?.enabled} onClick={() => void execute(() => api.timeSimulation.advance(0, -1))}>−1 ngày</button><button disabled={busy || !status?.enabled} onClick={() => void execute(() => api.timeSimulation.advance(0, 1))}>+1 ngày</button></div>
      <button className="simulation-reset" disabled={busy || !status?.enabled} onClick={() => void execute(() => api.timeSimulation.reset())}>Trở về thời gian thực</button>
    </div>}
  </div>;
}

function formatSimulationTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", weekday: "short", day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}
