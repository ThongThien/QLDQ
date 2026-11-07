"use client";

import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type AuthUser, type Soldier, type WorkItem, type WorkItemInput, type WorkPeriod, type WorkSchedule } from "../api-client";
import { ModuleToast, OperationsShell, PdfPreviewButton, PublishConfirmModal, SchedulePreview, ScheduleStatusBadge, WeekStartModal, formatModuleDate, isMondayDate } from "../schedule-modules/shared";
import { confirmWorkAssignmentConflicts } from "../assignment-conflicts";
import { useVisibilityPolling } from "../use-visibility-polling";
import { useRouter } from "next/navigation";

const service = api.schedules("area-monitoring");
const datesOfWeek = (start: string) => Array.from({ length: 7 }, (_, index) => { const date = new Date(`${start}T12:00:00+07:00`); date.setUTCDate(date.getUTCDate() + index); return date.toISOString().slice(0, 10); });
const blankShift = (date: string, period: WorkPeriod, id: number): WorkItem => ({ id, scheduleId: 0, workDate: date, period, startTime: period === "MORNING" ? "07:00:00" : "13:30:00", endTime: period === "MORNING" ? "11:00:00" : "17:00:00", allDay: false, content: "Nắm tình hình địa bàn", leader: null, participantsDescription: null, location: "", preparation: null, dutyLeader: null, note: null, sortOrder: 0, occurrenceStatus: "UPCOMING", assignedSoldiers: [], createdAt: "", updatedAt: "" });

export default function IntegratedAreaClient() {
  const router = useRouter();
  const [schedules, setSchedules] = useState<WorkSchedule[]>([]);
  const [draft, setDraft] = useState<WorkSchedule | null>(null);
  const [soldiers, setSoldiers] = useState<Soldier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [confirmPublish, setConfirmPublish] = useState(false);
  const [weekCreateOpen, setWeekCreateOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [businessWeek, setBusinessWeek] = useState({ from: "", to: "" });
  const [businessClock, setBusinessClock] = useState({ value: "", simulationEnabled: false });
  const [user, setUser] = useState<AuthUser | null>(null);
  const [viewScope, setViewScope] = useState<"unit" | "mine">("unit");
  const [previewSchedule, setPreviewSchedule] = useState<WorkSchedule | null>(null);
  const isCommander = user?.role === "COMMANDER";

  function handleError(cause: unknown, fallback: string) {
    if (cause instanceof ApiError && cause.status === 401) router.replace("/");
    else setError(cause instanceof Error ? cause.message : fallback);
  }

  const load = useCallback(async (background = false) => {
    if (!background) { setLoading(true); setError(""); }
    try {
      const me = await api.auth.me();
      setUser(me);
      const [page, summary] = await Promise.all([service.list(viewScope === "mine"), api.dashboard()]);
      const roster = me.role === "COMMANDER" ? await api.soldiers() : null;
      setSchedules(page.content); setSoldiers(roster?.content.filter((item) => item.active) ?? []);
      setBusinessWeek({ from: summary.weekStartDate, to: summary.weekEndDate });
      setBusinessClock({ value: summary.businessTime, simulationEnabled: summary.simulationEnabled });
      const linkedItemId = Number(new URLSearchParams(window.location.search).get("item"));
      const linkedSchedule = linkedItemId ? page.content.find((schedule) => schedule.items.some((item) => item.id === linkedItemId)) : null;
      if (linkedSchedule) {
        setDraft(linkedSchedule);
        window.history.replaceState({}, "", window.location.pathname);
      }
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) router.replace("/");
      else if (!background) setError(cause instanceof Error ? cause.message : "Không thể tải dữ liệu địa bàn.");
    } finally { if (!background) setLoading(false); }
  }, [router, viewScope]);
  useEffect(() => { queueMicrotask(() => void load()); }, [load]);
  useVisibilityPolling(() => load(true), !loading);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(""), 2600); return () => clearTimeout(timer); }, [toast]);

  function create(from: unknown) {
    if (typeof from !== "string") { setWeekCreateOpen(true); return; }
    if (!isMondayDate(from)) { setError("Ngày bắt đầu tuần phải là Thứ Hai."); return; }
    let id = -1;
    setWeekCreateOpen(false);
    const items = datesOfWeek(from).flatMap((date) => [blankShift(date, "MORNING", id--), blankShift(date, "AFTERNOON", id--)]);
    setDraft({ id: 0, scheduleType: "AREA_MONITORING", title: "Lịch nắm tình hình địa bàn", weekStartDate: from, weekEndDate: datesOfWeek(from)[6], weekLabel: "", scopeArea: "Phường Bắc Cam Ranh", generalNote: "", status: "DRAFT", createdBy: { id: 0, username: "", fullName: "", rankOrPosition: null, active: true }, items, createdAt: "", updatedAt: "" });
  }

  async function persist(publish = false) {
    if (!draft || saving) return;
    if (!isMondayDate(draft.weekStartDate)) { setError("Ngày bắt đầu tuần phải là Thứ Hai."); return; }
    setSaving(true); setError("");
    try {
      if (!(await confirmWorkAssignmentConflicts(draft.items))) return;
      const header = { title: draft.title, weekStartDate: draft.weekStartDate, scopeArea: draft.scopeArea, generalNote: draft.generalNote ?? "", status: draft.status };
      let saved = draft.id ? await service.update(draft.id, header) : await service.create(header);
      const original = draft.id ? schedules.find((item) => item.id === draft.id)?.items ?? [] : [];
      for (const removed of original.filter((item) => !draft.items.some((candidate) => candidate.id === item.id))) await service.removeItem(saved.id, removed.id);
      for (const [index, item] of draft.items.entries()) {
        const body = toInput(item, index);
        if (item.id > 0) await service.updateItem(saved.id, item.id, body); else await service.addItem(saved.id, body);
      }
      saved = await service.get(saved.id);
      if (publish && saved.status === "DRAFT") await service.publish(saved.id);
      setDraft(null); setConfirmPublish(false); setToast(publish ? "Đã công bố lịch theo dõi địa bàn" : "Đã lưu lịch theo dõi địa bàn"); await load();
    } catch (cause) { handleError(cause, "Không thể lưu lịch địa bàn."); }
    finally { setSaving(false); }
  }

  async function remove(schedule: WorkSchedule) {
    if (saving || !window.confirm(`Xóa lịch “${schedule.title}”?`)) return;
    setSaving(true);
    try { await service.remove(schedule.id); setToast("Đã xóa lịch nháp"); await load(); }
    catch (cause) { handleError(cause, "Không thể xóa lịch."); }
    finally { setSaving(false); }
  }

  async function logout() {
    if (!window.confirm("Đăng xuất khỏi hệ thống?")) return;
    try { await api.auth.logout(); } finally { router.replace("/"); }
  }

  return <OperationsShell active="area" title="Theo dõi tình hình trên địa bàn" subtitle={isCommander ? "Phân công lực lượng nắm tình hình địa bàn phường Bắc Cam Ranh theo từng buổi." : "Theo dõi lịch chung của đơn vị và lượt được phân công."} businessTime={businessClock.value} simulationEnabled={businessClock.simulationEnabled} onTimeChanged={() => void load(true)} user={user} onLogout={() => void logout()}>
    {loading && <section className="module-list-panel"><div className="module-empty"><p>Đang tải dữ liệu...</p></div></section>}
    {error && <div className="module-notification"><span>!</span><div><strong>Không thể thực hiện</strong><p>{error}</p></div></div>}
    {!loading && !draft && <><div className="module-actions-bar"><div><strong>{isCommander ? "Danh sách lịch theo dõi" : viewScope === "unit" ? "Lịch đơn vị" : "Lịch của tôi"}</strong><span>{viewScope === "mine" ? "Các lượt được phân công trực tiếp" : "Lịch theo dõi địa bàn đã công bố"}</span></div>{!isCommander && <div className="scope-tabs"><button className={viewScope === "unit" ? "active" : ""} onClick={() => setViewScope("unit")}>Lịch đơn vị</button><button className={viewScope === "mine" ? "active" : ""} onClick={() => setViewScope("mine")}>Lịch của tôi</button></div>}{isCommander && <button className="btn btn-primary" onClick={create}>+ Tạo lịch theo dõi</button>}</div><section className="module-list-panel">{schedules.length ? <div className="module-list-scroll"><table className="module-list-table"><thead><tr><th>Tuần</th><th>Khoảng ngày</th><th>Trạng thái</th><th>Người tạo</th><th>Cập nhật</th><th>Thao tác</th></tr></thead><tbody>{schedules.map((schedule) => <tr key={schedule.id}><td><strong>{schedule.title}</strong><small>{schedule.items.length} lượt theo dõi</small></td><td>{formatModuleDate(schedule.weekStartDate)} – {formatModuleDate(schedule.weekEndDate)}</td><td><ScheduleStatusBadge status={schedule.status}/></td><td>{schedule.createdBy.fullName}</td><td>{new Date(schedule.updatedAt).toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })}</td><td><div className="row-actions"><button onClick={() => setDraft(schedule)}>{isCommander ? "Xem / Chỉnh sửa" : "Xem chi tiết"}</button><PdfPreviewButton onClick={() => setPreviewSchedule(schedule)}/>{isCommander && schedule.status === "DRAFT" && <><button className="primary-link" onClick={() => { setDraft(schedule); setConfirmPublish(true); }}>Công bố</button><button className="danger-link" onClick={() => void remove(schedule)}>Xóa</button></>}</div></td></tr>)}</tbody></table></div> : <div className="module-empty"><h3>Chưa có lịch theo dõi địa bàn</h3><p>{isCommander ? "Tạo lịch mới để phân công lực lượng." : viewScope === "mine" ? "Đồng chí chưa được phân công theo dõi địa bàn." : "Đơn vị chưa công bố lịch theo dõi địa bàn."}</p></div>}</section></>}
    {draft && <AreaEditor schedule={draft} soldiers={soldiers} readOnly={!isCommander} onChange={setDraft} onBack={() => setDraft(null)} onSave={() => void persist()} onPublish={() => setConfirmPublish(true)}/>} 
    {weekCreateOpen && <WeekStartModal title="Tạo lịch theo dõi địa bàn" initialDate={businessWeek.from} onCancel={() => setWeekCreateOpen(false)} onConfirm={create}/>} 
    {confirmPublish && draft && <PublishConfirmModal title={`Công bố ${draft.title}?`} busy={saving} onCancel={() => setConfirmPublish(false)} onConfirm={() => void persist(true)}/>} 
    {saving && !confirmPublish && <div className="modal-layer"><button className="modal-backdrop" disabled aria-label="Đang lưu"/><div className="module-confirm"><h2>Đang lưu lịch...</h2><p>Hệ thống đang cập nhật lịch địa bàn và các phân công.</p></div></div>}
    {toast && <ModuleToast>{toast}</ModuleToast>}
    {previewSchedule && <SchedulePreview onClose={() => setPreviewSchedule(null)} onDownload={() => window.print()}><AreaSchedulePrint schedule={previewSchedule}/></SchedulePreview>}
  </OperationsShell>;
}

function AreaEditor({ schedule, soldiers, readOnly, onChange, onBack, onSave, onPublish }: { schedule: WorkSchedule; soldiers: Soldier[]; readOnly: boolean; onChange: (value: WorkSchedule) => void; onBack: () => void; onSave: () => void; onPublish: () => void }) {
  if (readOnly) return <><div className="module-editor-toolbar"><button className="back-link" onClick={onBack}>← Danh sách lịch</button></div><section className="area-info-banner"><div><span>PHẠM VI THEO DÕI</span><strong>{schedule.scopeArea || "Phường Bắc Cam Ranh"} · {formatModuleDate(schedule.weekStartDate)} – {formatModuleDate(schedule.weekEndDate)}</strong></div><ScheduleStatusBadge status={schedule.status}/></section><div className="area-day-grid">{schedule.items.map((item) => <section className="area-day-card" key={item.id}><header><div><div><strong>{formatModuleDate(item.workDate)}</strong><small>{item.period === "MORNING" ? "Buổi sáng" : item.period === "AFTERNOON" ? "Buổi chiều" : "Buổi tối"}</small></div></div></header><div className={`area-shift ${item.period === "MORNING" ? "morning" : "afternoon"}`}><div className="area-shift-fields"><div><span>Khu vực</span><strong>{item.location || "—"}</strong></div><div><span>Thời gian</span><strong>{item.startTime?.slice(0, 5)} – {item.endTime?.slice(0, 5)}</strong></div><div><span>Chiến sĩ</span><strong>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ")}</strong></div></div></div></section>)}</div></>;
  const update = (id: number, patch: Partial<WorkItem>) => onChange({ ...schedule, items: schedule.items.map((item) => item.id === id ? { ...item, ...patch } : item) });
  return <><div className="module-editor-toolbar"><button className="back-link" onClick={onBack}>← Danh sách lịch</button><div><button className="btn btn-secondary" onClick={onSave}>Lưu nháp</button>{schedule.status === "DRAFT" && <button className="btn btn-primary" onClick={onPublish}>Công bố lịch</button>}</div></div><section className="area-info-banner"><div><span>PHẠM VI THEO DÕI</span><strong>{schedule.scopeArea || "Chưa nhập phạm vi"} · {formatModuleDate(schedule.weekStartDate)} – {formatModuleDate(schedule.weekEndDate)}</strong></div><ScheduleStatusBadge status={schedule.status}/></section><section className="general-info-panel"><div className="general-info-grid"><label><span>Tiêu đề</span><input value={schedule.title} onChange={(event) => onChange({ ...schedule, title: event.target.value })}/></label><label><span>Phạm vi</span><input value={schedule.scopeArea ?? ""} onChange={(event) => onChange({ ...schedule, scopeArea: event.target.value })}/></label></div></section><div className="area-day-grid">{datesOfWeek(schedule.weekStartDate).map((date, dayIndex) => <section className="area-day-card" key={date}><header><div><span>{String(dayIndex + 2).padStart(2, "0")}</span><div><strong>{formatModuleDate(date)}</strong><small>Phân công theo buổi</small></div></div></header>{(["MORNING", "AFTERNOON"] as WorkPeriod[]).map((period) => { const shift = schedule.items.find((item) => item.workDate === date && item.period === period); if (!shift) return null; return <div className={`area-shift ${period === "MORNING" ? "morning" : "afternoon"}`} key={shift.id}><div className="area-shift-title"><i/><strong>{period === "MORNING" ? "BUỔI SÁNG" : "BUỔI CHIỀU"}</strong></div><div className="area-shift-fields"><label><span>Khu vực</span><input value={shift.location ?? ""} onChange={(event) => update(shift.id, { location: event.target.value })}/></label><label><span>Thời gian</span><input type="time" value={shift.startTime?.slice(0, 5) ?? ""} onChange={(event) => update(shift.id, { startTime: event.target.value })}/></label><label><span>Chiến sĩ</span><select multiple value={shift.assignedSoldiers.map((soldier) => String(soldier.id))} onChange={(event) => { const ids = Array.from(event.target.selectedOptions).map((option) => Number(option.value)); update(shift.id, { assignedSoldiers: soldiers.filter((soldier) => ids.includes(soldier.id)) }); }}>{soldiers.map((soldier) => <option key={soldier.id} value={soldier.id}>{soldier.fullName}</option>)}</select></label></div></div>; })}</section>)}</div><section className="area-note"><strong>Ghi chú thực hiện</strong><textarea rows={4} value={schedule.generalNote ?? ""} onChange={(event) => onChange({ ...schedule, generalNote: event.target.value })}/></section></>;
}

function AreaSchedulePrint({ schedule }: { schedule: WorkSchedule }) {
  const rows = [...schedule.items].sort((left, right) => left.workDate.localeCompare(right.workDate) || left.sortOrder - right.sortOrder);
  return <div className="weekly-schedule-print area-schedule-print">
    <header><div><strong>BAN CHQS PHƯỜNG BẮC CAM RANH</strong><span>TRUNG ĐỘI DÂN QUÂN THƯỜNG TRỰC</span></div><div><strong>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</strong><span>Độc lập - Tự do - Hạnh phúc</span></div></header>
    <h1>{schedule.title.toUpperCase()}</h1><p className="print-week">Từ ngày {formatModuleDate(schedule.weekStartDate)} đến ngày {formatModuleDate(schedule.weekEndDate)}</p>
    <table><thead><tr><th>Thứ / ngày</th><th>Buổi</th><th>Thời gian</th><th>Khu vực</th><th>Lực lượng được phân công</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{formatModuleDate(item.workDate)}</td><td>{item.period === "MORNING" ? "Sáng" : item.period === "AFTERNOON" ? "Chiều" : "Tối"}</td><td>{item.startTime?.slice(0, 5) ?? "--:--"} – {item.endTime?.slice(0, 5) ?? "--:--"}</td><td>{item.location || schedule.scopeArea || "—"}</td><td>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ") || item.participantsDescription || "—"}</td></tr>)}</tbody></table>
    {schedule.generalNote && <p className="print-note"><strong>Ghi chú:</strong> {schedule.generalNote}</p>}
    <footer><div><strong>NGƯỜI LẬP</strong><span>{schedule.createdBy.fullName}</span></div><div><strong>CHỈ HUY ĐƠN VỊ</strong><span>(Ký, ghi rõ họ tên)</span></div></footer>
  </div>;
}

function toInput(item: WorkItem, sortOrder: number): WorkItemInput {
  return { workDate: item.workDate, period: item.period, startTime: item.startTime?.slice(0, 5) ?? null, endTime: item.endTime?.slice(0, 5) ?? null, allDay: false, content: item.content || "Nắm tình hình địa bàn", leader: item.leader, participantsDescription: item.participantsDescription, location: item.location, preparation: item.preparation, dutyLeader: item.dutyLeader, note: item.note, sortOrder, soldierIds: item.assignedSoldiers.map((soldier) => soldier.id) };
}
