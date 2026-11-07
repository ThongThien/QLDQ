"use client";

import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type AuthUser, type Soldier, type WorkItem, type WorkItemInput, type WorkSchedule } from "../api-client";
import { ModuleToast, OperationsShell, PdfPreviewButton, PublishConfirmModal, SchedulePreview, ScheduleStatusBadge, WeekStartModal, formatModuleDate, isMondayDate } from "../schedule-modules/shared";
import { confirmWorkAssignmentConflicts } from "../assignment-conflicts";
import { useVisibilityPolling } from "../use-visibility-polling";
import { useRouter } from "next/navigation";

const service = api.schedules("work-schedules");
const emptyItem = (date: string, id = -Date.now()): WorkItem => ({ id, scheduleId: 0, workDate: date, period: "MORNING", startTime: "07:00:00", endTime: "09:00:00", allDay: false, content: "", leader: null, participantsDescription: null, location: null, preparation: null, dutyLeader: null, note: null, sortOrder: 0, occurrenceStatus: "UPCOMING", assignedSoldiers: [], createdAt: "", updatedAt: "" });
const datesOfWeek = (start: string) => Array.from({ length: 7 }, (_, index) => { const date = new Date(`${start}T12:00:00+07:00`); date.setUTCDate(date.getUTCDate() + index); return date.toISOString().slice(0, 10); });

export default function IntegratedWorkScheduleClient() {
  const router = useRouter();
  const [items, setItems] = useState<WorkSchedule[]>([]);
  const [draft, setDraft] = useState<WorkSchedule | null>(null);
  const [soldiers, setSoldiers] = useState<Soldier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [publishConfirm, setPublishConfirm] = useState(false);
  const [weekCreateOpen, setWeekCreateOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [printSchedule, setPrintSchedule] = useState<WorkSchedule | null>(null);
  const [businessWeek, setBusinessWeek] = useState({ from: "", to: "" });
  const [businessClock, setBusinessClock] = useState({ value: "", simulationEnabled: false });
  const [user, setUser] = useState<AuthUser | null>(null);
  const [viewScope, setViewScope] = useState<"unit" | "mine">("unit");
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
      setItems(page.content); setSoldiers(roster?.content.filter((item) => item.active) ?? []);
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
      else if (!background) setError(cause instanceof Error ? cause.message : "Không thể tải lịch công tác.");
    } finally { if (!background) setLoading(false); }
  }, [router, viewScope]);
  useEffect(() => { queueMicrotask(() => void load()); }, [load]);
  useVisibilityPolling(() => load(true), !loading);
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(""), 2600); return () => clearTimeout(timer); }, [toast]);

  function create(from: unknown) {
    if (typeof from !== "string") { setWeekCreateOpen(true); return; }
    if (!isMondayDate(from)) { setError("Ngày bắt đầu tuần phải là Thứ Hai."); return; }
    setWeekCreateOpen(false);
    setDraft({ id: 0, scheduleType: "WEEKLY_WORK", title: "Lịch công tác tuần", weekStartDate: from, weekEndDate: datesOfWeek(from)[6], weekLabel: "", scopeArea: null, generalNote: "", status: "DRAFT", createdBy: { id: 0, username: "", fullName: "", rankOrPosition: null, active: true }, items: [], createdAt: "", updatedAt: "" });
  }

  async function persist(publish = false) {
    if (!draft || saving) return;
    if (!draft.title.trim()) { setError("Tiêu đề lịch không được để trống."); return; }
    if (!isMondayDate(draft.weekStartDate)) { setError("Ngày bắt đầu tuần phải là Thứ Hai."); return; }
    setSaving(true); setError("");
    try {
      if (!(await confirmWorkAssignmentConflicts(draft.items))) return;
      const header = { title: draft.title, weekStartDate: draft.weekStartDate, scopeArea: draft.scopeArea, generalNote: draft.generalNote ?? "", status: draft.status };
      let saved = draft.id ? await service.update(draft.id, header) : await service.create(header);
      const original = draft.id ? items.find((item) => item.id === draft.id)?.items ?? [] : [];
      for (const removed of original.filter((item) => !draft.items.some((candidate) => candidate.id === item.id))) await service.removeItem(saved.id, removed.id);
      for (const [index, item] of draft.items.entries()) {
        const body = toItemInput(item, index);
        if (item.id > 0) await service.updateItem(saved.id, item.id, body); else await service.addItem(saved.id, body);
      }
      saved = await service.get(saved.id);
      if (publish && saved.status === "DRAFT") saved = await service.publish(saved.id);
      setDraft(null); setPublishConfirm(false); setToast(publish ? "Đã công bố lịch công tác tuần" : "Đã lưu lịch công tác tuần"); await load();
    } catch (cause) { handleError(cause, "Không thể lưu lịch công tác."); }
    finally { setSaving(false); }
  }

  async function remove(schedule: WorkSchedule) {
    if (saving || !window.confirm(`Xóa lịch “${schedule.title}”?`)) return;
    setSaving(true);
    try { await service.remove(schedule.id); setToast("Đã xóa lịch nháp"); await load(); }
    catch (cause) { handleError(cause, "Không thể xóa lịch."); }
    finally { setSaving(false); }
  }

  function exportPdf(schedule: WorkSchedule) {
    setPrintSchedule(schedule);
  }

  async function logout() {
    if (!window.confirm("Đăng xuất khỏi hệ thống?")) return;
    try { await api.auth.logout(); } finally { router.replace("/"); }
  }

  return <OperationsShell active="work" title="Lịch công tác tuần" subtitle={isCommander ? "Lập, kiểm tra và công bố kế hoạch công tác của Trung đội Dân quân thường trực." : "Theo dõi lịch chung của đơn vị và nhiệm vụ được phân công."} businessTime={businessClock.value} simulationEnabled={businessClock.simulationEnabled} onTimeChanged={() => void load(true)} user={user} onLogout={() => void logout()}>
    {loading && <section className="module-list-panel"><div className="module-empty"><p>Đang tải dữ liệu...</p></div></section>}
    {error && <div className="module-notification"><span>!</span><div><strong>Không thể thực hiện</strong><p>{error}</p></div></div>}
    {!loading && !draft && <><div className="module-actions-bar"><div><strong>{isCommander ? "Danh sách lịch" : viewScope === "unit" ? "Lịch đơn vị" : "Lịch của tôi"}</strong><span>{viewScope === "mine" ? "Các nhiệm vụ được phân công trực tiếp" : "Dữ liệu lịch công tác đã công bố"}</span></div>{!isCommander && <div className="scope-tabs"><button className={viewScope === "unit" ? "active" : ""} onClick={() => setViewScope("unit")}>Lịch đơn vị</button><button className={viewScope === "mine" ? "active" : ""} onClick={() => setViewScope("mine")}>Lịch của tôi</button></div>}{isCommander && <button className="btn btn-primary" onClick={create}>+ Tạo lịch công tác tuần</button>}</div><section className="module-list-panel">{items.length ? <div className="module-list-scroll"><table className="module-list-table"><thead><tr><th>Tuần</th><th>Khoảng ngày</th><th>Trạng thái</th><th>Người tạo</th><th>Cập nhật</th><th>Thao tác</th></tr></thead><tbody>{items.map((schedule) => <tr key={schedule.id}><td><strong>{schedule.title}</strong><small>{schedule.items.length} công việc</small></td><td>{formatModuleDate(schedule.weekStartDate)} – {formatModuleDate(schedule.weekEndDate)}</td><td><ScheduleStatusBadge status={schedule.status}/></td><td>{schedule.createdBy.fullName}</td><td>{new Date(schedule.updatedAt).toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" })}</td><td><div className="row-actions"><button onClick={() => setDraft(schedule)}>{isCommander ? "Xem / Chỉnh sửa" : "Xem chi tiết"}</button><PdfPreviewButton onClick={() => exportPdf(schedule)}/>{isCommander && schedule.status === "DRAFT" && <><button className="primary-link" onClick={() => { setDraft(schedule); setPublishConfirm(true); }}>Công bố</button><button className="danger-link" onClick={() => void remove(schedule)}>Xóa</button></>}</div></td></tr>)}</tbody></table></div> : <div className="module-empty"><span>▦</span><h3>Chưa có lịch công tác</h3><p>{isCommander ? "Tạo lịch mới để bắt đầu phân công nhiệm vụ tuần." : viewScope === "mine" ? "Đồng chí chưa được phân công nhiệm vụ công tác tuần." : "Đơn vị chưa công bố lịch công tác tuần."}</p></div>}</section></>}
    {draft && <WorkEditor schedule={draft} soldiers={soldiers} readOnly={!isCommander} onChange={setDraft} onBack={() => setDraft(null)} onSave={() => void persist()} onPublish={() => setPublishConfirm(true)}/>} 
    {weekCreateOpen && <WeekStartModal title="Tạo lịch công tác tuần" initialDate={businessWeek.from} onCancel={() => setWeekCreateOpen(false)} onConfirm={create}/>} 
    {publishConfirm && draft && <PublishConfirmModal title={`Công bố ${draft.title}?`} busy={saving} onCancel={() => setPublishConfirm(false)} onConfirm={() => void persist(true)}/>} 
    {saving && !publishConfirm && <div className="modal-layer"><button className="modal-backdrop" disabled aria-label="Đang lưu"/><div className="module-confirm"><h2>Đang lưu lịch...</h2><p>Hệ thống đang cập nhật lịch công tác và các phân công.</p></div></div>}
    {toast && <ModuleToast>{toast}</ModuleToast>}
    {printSchedule && <SchedulePreview onClose={() => setPrintSchedule(null)} onDownload={() => window.print()}><WeeklySchedulePrint schedule={printSchedule}/></SchedulePreview>} 
  </OperationsShell>;
}

function WorkEditor({ schedule, soldiers, readOnly, onChange, onBack, onSave, onPublish }: { schedule: WorkSchedule; soldiers: Soldier[]; readOnly: boolean; onChange: (value: WorkSchedule) => void; onBack: () => void; onSave: () => void; onPublish: () => void }) {
  const dates = datesOfWeek(schedule.weekStartDate);
  if (readOnly) return <><div className="module-editor-toolbar"><button className="back-link" onClick={onBack}>← Danh sách lịch</button></div><section className="general-info-panel"><div className="section-heading"><span>01</span><div><h2>{schedule.title}</h2><p>{formatModuleDate(schedule.weekStartDate)} – {formatModuleDate(schedule.weekEndDate)}</p></div></div>{schedule.generalNote && <p>{schedule.generalNote}</p>}</section><section className="module-list-panel"><div className="module-list-scroll"><table className="module-list-table"><thead><tr><th>Ngày</th><th>Thời gian</th><th>Nội dung</th><th>Địa điểm</th><th>Phân công</th></tr></thead><tbody>{schedule.items.map((item) => <tr key={item.id}><td>{formatModuleDate(item.workDate)}</td><td>{item.allDay ? "Cả ngày" : `${item.startTime?.slice(0, 5)} – ${item.endTime?.slice(0, 5)}`}</td><td><strong>{item.content}</strong><small>{item.leader ? `Chủ trì: ${item.leader}` : ""}</small></td><td>{item.location || "—"}</td><td>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ") || item.participantsDescription || "—"}</td></tr>)}</tbody></table></div></section></>;
  const updateItem = (id: number, patch: Partial<WorkItem>) => onChange({ ...schedule, items: schedule.items.map((item) => item.id === id ? { ...item, ...patch } : item) });
  return <><div className="module-editor-toolbar"><button className="back-link" onClick={onBack}>← Danh sách lịch</button><div><button className="btn btn-secondary" onClick={onSave}>Lưu nháp</button>{schedule.status === "DRAFT" && <button className="btn btn-primary" onClick={onPublish}>Công bố lịch</button>}</div></div><section className="general-info-panel"><div className="section-heading"><span>01</span><div><h2>Thông tin chung</h2><p>Thông tin nhận diện lịch công tác tuần</p></div></div><div className="general-info-grid"><label className="wide"><span>Tiêu đề</span><input value={schedule.title} onChange={(event) => onChange({ ...schedule, title: event.target.value })}/></label><label><span>Ngày bắt đầu</span><input type="date" value={schedule.weekStartDate} onChange={(event) => { const from = event.target.value; onChange({ ...schedule, weekStartDate: from, weekEndDate: datesOfWeek(from)[6] }); }}/></label><label><span>Phạm vi</span><input value={schedule.scopeArea ?? ""} onChange={(event) => onChange({ ...schedule, scopeArea: event.target.value })}/></label><label className="wide"><span>Ghi chú chung</span><textarea rows={2} value={schedule.generalNote ?? ""} onChange={(event) => onChange({ ...schedule, generalNote: event.target.value })}/></label></div></section><div className="day-sections">{dates.map((date) => { const dayItems = schedule.items.filter((item) => item.workDate === date); return <section className="day-section" key={date}><div className="day-section-head"><div><strong>{formatModuleDate(date)}</strong><span>{dayItems.length} công việc</span></div><button className="btn btn-outline" onClick={() => onChange({ ...schedule, items: [...schedule.items, emptyItem(date)] })}>+ Thêm công việc</button></div>{dayItems.map((item, index) => <article className="work-item-row" key={item.id}><div className="work-item-number">{String(index + 1).padStart(2, "0")}</div><div className="work-item-fields"><label><span>Buổi</span><select value={item.period} onChange={(event) => updateItem(item.id, { period: event.target.value as WorkItem["period"] })}><option value="MORNING">Sáng</option><option value="AFTERNOON">Chiều</option><option value="EVENING">Tối</option><option value="ALL_DAY">Cả ngày</option></select></label><label><span>Bắt đầu</span><input type="time" value={item.startTime?.slice(0, 5) ?? ""} onChange={(event) => updateItem(item.id, { startTime: event.target.value || null })}/></label><label className="content-field"><span>Nội dung</span><textarea rows={2} value={item.content} onChange={(event) => updateItem(item.id, { content: event.target.value })}/></label><label><span>Chủ trì</span><input value={item.leader ?? ""} onChange={(event) => updateItem(item.id, { leader: event.target.value })}/></label><label><span>Địa điểm</span><input value={item.location ?? ""} onChange={(event) => updateItem(item.id, { location: event.target.value })}/></label><label><span>Chiến sĩ</span><select multiple value={item.assignedSoldiers.map((soldier) => String(soldier.id))} onChange={(event) => { const ids = Array.from(event.target.selectedOptions).map((option) => Number(option.value)); updateItem(item.id, { assignedSoldiers: soldiers.filter((soldier) => ids.includes(soldier.id)) }); }}>{soldiers.map((soldier) => <option key={soldier.id} value={soldier.id}>{soldier.fullName}</option>)}</select></label></div><div className="work-item-actions"><button className="danger" onClick={() => onChange({ ...schedule, items: schedule.items.filter((candidate) => candidate.id !== item.id) })}>×</button></div></article>)}</section>; })}</div></>;
}

function WeeklySchedulePrint({ schedule }: { schedule: WorkSchedule }) {
  const rows = [...schedule.items].sort((left, right) => left.workDate.localeCompare(right.workDate) || left.sortOrder - right.sortOrder);
  return <div className="weekly-schedule-print">
    <header><div><strong>BAN CHQS PHƯỜNG BẮC CAM RANH</strong><span>TRUNG ĐỘI DÂN QUÂN THƯỜNG TRỰC</span></div><div><strong>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</strong><span>Độc lập - Tự do - Hạnh phúc</span></div></header>
    <h1>{schedule.title.toUpperCase()}</h1><p className="print-week">Từ ngày {formatModuleDate(schedule.weekStartDate)} đến ngày {formatModuleDate(schedule.weekEndDate)}</p>
    <table><thead><tr><th>Thứ / ngày</th><th>Thời gian</th><th>Nội dung công việc</th><th>Địa điểm</th><th>Người được phân công</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{formatModuleDate(item.workDate)}</td><td>{item.allDay ? "Cả ngày" : `${item.startTime?.slice(0, 5) ?? "--:--"} – ${item.endTime?.slice(0, 5) ?? "--:--"}`}</td><td><strong>{item.content}</strong>{item.leader && <small>Chủ trì: {item.leader}</small>}</td><td>{item.location || "—"}</td><td>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ") || item.participantsDescription || "—"}</td></tr>)}</tbody></table>
    {!rows.length && <p className="print-empty">Tuần này chưa có công việc.</p>}
    {schedule.generalNote && <p className="print-note"><strong>Ghi chú:</strong> {schedule.generalNote}</p>}
    <footer><div><strong>NGƯỜI LẬP</strong><span>{schedule.createdBy.fullName}</span></div><div><strong>CHỈ HUY ĐƠN VỊ</strong><span>(Ký, ghi rõ họ tên)</span></div></footer>
  </div>;
}

function toItemInput(item: WorkItem, sortOrder: number): WorkItemInput {
  const allDay = item.period === "ALL_DAY" || item.allDay;
  return { workDate: item.workDate, period: item.period, startTime: allDay ? null : item.startTime?.slice(0, 5) ?? null, endTime: allDay ? null : item.endTime?.slice(0, 5) ?? null, allDay, content: item.content, leader: item.leader, participantsDescription: item.participantsDescription, location: item.location, preparation: item.preparation, dutyLeader: item.dutyLeader, note: item.note, sortOrder, soldierIds: item.assignedSoldiers.map((soldier) => soldier.id) };
}
