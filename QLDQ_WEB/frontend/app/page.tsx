"use client";

import { FormEvent, ReactNode, useEffect, useState } from "react";
import { LOCATIONS, STATUS_LABEL, WEEKLY_SHIFT_TIMES, formatDate, shortDate } from "./ui-data";
import type { WeeklyRosterDay } from "./ui-data";
import type { CurrentUser, GuardRecord, GuardSchedule, ScheduleStatus, View } from "./types";
import { api, ApiError, type ActivityAction, type ActivityLog, type AuthUser, type Dashboard, type GuardShift, type NotificationItem, type Soldier, type WorkItem, type WorkSchedule, vietnamToday } from "./api-client";
import { NotificationBell } from "./notification-bell";
import { SimulationControls } from "./simulation-controls";
import { BusinessClock } from "./business-clock";
import { useVisibilityPolling } from "./use-visibility-polling";
import { AppSidebar, type SidebarItem } from "./app-sidebar";
import { SiteFooter, UnitLogo } from "./unit-brand";
import { WeekStartModal } from "./schedule-modules/shared";

type GuardForm = Pick<GuardSchedule, "date" | "startTime" | "endTime" | "location" | "assignedSoldiers" | "status" | "note">;
const EMPTY_FORM: GuardForm = { date: "", startTime: "18:00", endTime: "20:00", location: LOCATIONS[0], assignedSoldiers: [], status: "DRAFT", note: "" };
type IconName = "grid" | "calendar" | "users" | "shield" | "bell" | "chevron" | "plus" | "clock" | "pin" | "check" | "eye" | "edit" | "close" | "file" | "alert" | "menu" | "arrow";
const PATHS: Record<IconName, ReactNode> = {
  grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
  users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,
  shield: <><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/></>,
  chevron: <path d="m9 18 6-6-6-6"/>, plus: <path d="M12 5v14M5 12h14"/>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
  pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2"/></>,
  check: <path d="m5 12 4 4L19 6"/>, eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></>,
  edit: <><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></>, close: <path d="M18 6 6 18M6 6l12 12"/>,
  file: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 13h8M8 17h5"/></>,
  alert: <><path d="M10.3 2.9 1.8 17a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 2.9a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4M12 17h.01"/></>,
  menu: <path d="M4 6h16M4 12h16M4 18h16"/>, arrow: <path d="m9 18 6-6-6-6"/>,
};
function Icon({ name, size = 19 }: { name: IconName; size?: number }) { return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{PATHS[name]}</svg>; }
function StatusBadge({ status }: { status: ScheduleStatus }) { return <span className={`status status-${status.toLowerCase()}`}><span />{STATUS_LABEL[status]}</span>; }
function EmptyState({ children }: { children: ReactNode }) { return <div className="empty-state"><div className="empty-icon"><Icon name="calendar" size={24}/></div><p>{children}</p></div>; }

function LoginScreen({ onLogin }: { onLogin: (username: string, password: string) => Promise<void> }) {
  const [username, setUsername] = useState("chihuy");
  const [password, setPassword] = useState("Demo@123");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const selectedAccount = username === "chihuy" ? "COMMANDER" : username === "chiensi01" ? "SOLDIER" : null;
  function fillAccount(role: "COMMANDER" | "SOLDIER") {
    setUsername(role === "COMMANDER" ? "chihuy" : "chiensi01");
    setPassword("Demo@123");
    setError("");
  }
  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await onLogin(username, password);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Không thể kết nối tới Backend.");
    } finally {
      setSubmitting(false);
    }
  }
  return <main className="login-page">
    <section className="login-brand-panel"><UnitLogo/><span>BAN CHQS PHƯỜNG BẮC CAM RANH</span><h1>Quản lý Trung đội<br/>Dân quân thường trực</h1><p>Hệ thống quản lý quân số, lịch công tác, địa bàn và nhiệm vụ trực gác của đơn vị.</p></section>
    <section className="login-form-panel"><form className="login-form" onSubmit={submit}><div className="login-title"><span>CỔNG THÔNG TIN NỘI BỘ</span><h2>Đăng nhập hệ thống</h2><p>Chọn vai trò để điền nhanh tài khoản được cấp.</p></div><div className="account-tabs" aria-label="Chọn tài khoản đăng nhập"><button type="button" className={selectedAccount === "COMMANDER" ? "active" : ""} onClick={() => fillAccount("COMMANDER")} disabled={submitting}><span><Icon name="shield" size={17}/></span><div><strong>Chỉ huy</strong><small>chihuy · Quyền quản lý</small></div></button><button type="button" className={selectedAccount === "SOLDIER" ? "active" : ""} onClick={() => fillAccount("SOLDIER")} disabled={submitting}><span><Icon name="users" size={17}/></span><div><strong>Chiến sĩ</strong><small>chiensi01 · Quyền cá nhân</small></div></button></div><label><span>Tài khoản</span><input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required/></label><label><span>Mật khẩu</span><input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required/></label>{error && <p className="login-error">{error}</p>}<button className="btn btn-primary login-submit" type="submit" disabled={submitting}>{submitting ? "Đang đăng nhập..." : "Đăng nhập"} {!submitting && <Icon name="arrow" size={17}/>}</button><p className="login-security-note"><Icon name="shield" size={14}/> Phiên đăng nhập được bảo vệ và tự hết hạn khi không hoạt động.</p></form></section>
    <SiteFooter/>
  </main>;
}

function getRequestedView(user: CurrentUser): View {
  const requestedView = new URLSearchParams(window.location.search).get("view");
  if (requestedView === "schedules") return "schedules";
  if (requestedView === "soldiers" && user === "COMMANDER") return "soldiers";
  return "overview";
}

export default function Home() {
  const [schedules, setSchedules] = useState<GuardSchedule[]>([]);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [soldiers, setSoldiers] = useState<Soldier[]>([]);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);
  const [workItems, setWorkItems] = useState<WorkItem[]>([]);
  const [areaItems, setAreaItems] = useState<WorkItem[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [workItemDetail, setWorkItemDetail] = useState<WorkItem | null>(null);
  const [authUser, setAuthUser] = useState<AuthUser | null>(null);
  const [dataLoading, setDataLoading] = useState(false);
  const [dataError, setDataError] = useState("");
  const [weekStart, setWeekStart] = useState("");
  const [followCurrentWeek, setFollowCurrentWeek] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [authReady, setAuthReady] = useState(false);
  const [currentUser, setCurrentUser] = useState<CurrentUser>("COMMANDER");
  const [selectedWeeklySlot, setSelectedWeeklySlot] = useState<{ dayIndex: number; shiftIndex: number } | null>(null);
  const [view, setView] = useState<View>("overview");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [guardWeekOpen, setGuardWeekOpen] = useState(false);
  const [actionBusy, setActionBusy] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [toast, setToast] = useState("");
  const [mobileNav, setMobileNav] = useState(false);
  const isCommander = currentUser === "COMMANDER";
  const selected = schedules.find((item) => item.id === selectedId) ?? null;
  const weeklyRoster = buildWeeklyRoster(schedules, weekStart);

  async function loadData(user: AuthUser, requestedFrom?: string, background = false) {
    if (!background) { setDataLoading(true); setDataError(""); }
    try {
      const summary = await api.dashboard();
      const range = requestedFrom ? { from: requestedFrom, to: addDays(requestedFrom, 6) } : { from: summary.weekStartDate, to: summary.weekEndDate };
      const linkedShiftId = typeof window === "undefined" ? 0 : Number(new URLSearchParams(window.location.search).get("guardShift"));
      const linkedWorkItemId = typeof window === "undefined" ? 0 : Number(new URLSearchParams(window.location.search).get("workItem"));
      setWeekStart(range.from);
      if (user.role === "COMMANDER") {
        const [guards, roster, activities, work, area, notificationPage] = await Promise.all([
          api.guards.list(range.from, range.to), api.soldiers(), api.activityLogs({ size: 8 }),
          api.schedules("work-schedules").list(), api.schedules("area-monitoring").list(), api.notifications.list(0, 6),
        ]);
        const linked = linkedShiftId && !guards.content.some((item) => item.id === linkedShiftId) ? await api.guards.get(linkedShiftId) : null;
        setSchedules([...guards.content, ...(linked ? [linked] : [])].map((item) => mapGuardShift(item)));
        setDashboard(summary); setSoldiers(roster.content); setActivityLogs(activities.content);
        setWorkItems(dashboardItems(work.content, user)); setAreaItems(dashboardItems(area.content, user)); setNotifications(notificationPage.content);
      } else {
        const [guards, work, area, notificationPage] = await Promise.all([
          api.guards.list(range.from, range.to), api.schedules("work-schedules").list(),
          api.schedules("area-monitoring").list(), api.notifications.list(0, 6),
        ]);
        const linked = linkedShiftId && !guards.content.some((item) => item.id === linkedShiftId) ? await api.guards.get(linkedShiftId) : null;
        setSchedules([...guards.content, ...(linked ? [linked] : [])].map((item) => mapGuardShift(item, user.username)));
        setDashboard(summary); setSoldiers([]); setActivityLogs([]);
        setWorkItems(dashboardItems(work.content, user)); setAreaItems(dashboardItems(area.content, user)); setNotifications(notificationPage.content);
      }
      if (linkedShiftId) setSelectedId(linkedShiftId);
      if (linkedWorkItemId) setWorkItemDetail(await api.workItems.get(linkedWorkItemId));
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 401) { setAuthenticated(false); setAuthUser(null); }
      else if (!background) setDataError(cause instanceof Error ? cause.message : "Không thể tải dữ liệu từ Backend.");
    } finally { if (!background) setDataLoading(false); }
  }
  useEffect(() => {
    let active = true;
    api.auth.me()
      .then((user) => {
        if (!active) return;
        const restoredUser: CurrentUser = user.role === "COMMANDER" ? "COMMANDER" : user.fullName;
        setCurrentUser(restoredUser); setAuthUser(user);
        setAuthenticated(true);
        setView(getRequestedView(restoredUser));
        void loadData(user);
      })
      .catch(() => { if (active) setAuthenticated(false); })
      .finally(() => { if (active) setAuthReady(true); });
    return () => { active = false; };
  }, []);
  useVisibilityPolling(
    () => authUser ? loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined, true) : undefined,
    authenticated && Boolean(authUser),
  );
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(""), 2800); return () => window.clearTimeout(timer); }, [toast]);
  async function login(username: string, password: string) {
    const loggedIn = await api.auth.login(username, password);
    const user: CurrentUser = loggedIn.role === "COMMANDER" ? "COMMANDER" : loggedIn.fullName;
    setCurrentUser(user); setAuthUser(loggedIn);
    setAuthenticated(true);
    setView(getRequestedView(user));
    await loadData(loggedIn);
  }
  async function logout() {
    if (!window.confirm("Đăng xuất khỏi hệ thống?")) return;
    try { await api.auth.logout(); }
    finally { setAuthenticated(false); setAuthUser(null); setCurrentUser("COMMANDER"); setView("overview"); }
  }
  function openCreate() { setGuardWeekOpen(true); }
  function openEdit(id: number) { setEditingId(id); setSelectedId(null); setEditorOpen(true); }
  async function createGuardWeek(monday: string) {
    if (!authUser || actionBusy) return;
    setActionBusy(true); setDataError("");
    try {
      const result = await api.guards.generateWeek(monday);
      setGuardWeekOpen(false); setFollowCurrentWeek(false); setView("schedules");
      setToast(`Đã tạo ${result.createdShiftCount} ca gác và ${result.assignmentCount} lượt phân công`);
      await loadData(authUser, monday);
    } catch (cause) { handleActionError(cause, setAuthenticated, setDataError); }
    finally { setActionBusy(false); }
  }
  async function saveSchedule(values: GuardForm, publish: boolean) {
    if (!authUser || actionBusy) return;
    const soldierIds = values.assignedSoldiers.map((name) => soldiers.find((item) => item.fullName === name)?.id).filter((id): id is number => id !== undefined);
    if (!soldierIds.length) { setDataError("Cần phân công ít nhất một chiến sĩ."); return; }
    const endDate = values.endTime <= values.startTime ? addDays(values.date, 1) : values.date;
    const current = editingId ? schedules.find((item) => item.id === editingId) : null;
    const body = { startAt: `${values.date}T${values.startTime}:00+07:00`, endAt: `${endDate}T${values.endTime}:00+07:00`, location: "Cổng chính", note: values.note, status: (publish || current?.managementStatus === "PUBLISHED" ? "PUBLISHED" : "DRAFT") as "DRAFT" | "PUBLISHED", soldierIds };
    setActionBusy(true);
    try {
      const validation = await api.assignmentValidation.guard({ startAt: body.startAt, endAt: body.endAt, soldierIds, excludeGuardShiftId: editingId ?? undefined });
      if (validation.warnings.length && !window.confirm(formatConflictWarning(validation.warnings.map((warning) => warning.message)))) return;
      if (editingId) await api.guards.update(editingId, body); else await api.guards.create(body);
      setEditorOpen(false); setSelectedId(null); setView("schedules"); setToast(editingId ? "Đã cập nhật ca gác" : "Đã tạo ca gác");
      await loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined);
    } catch (cause) { handleActionError(cause, setAuthenticated, setDataError); }
    finally { setActionBusy(false); }
  }
  async function updateStatus(id: number, status: ScheduleStatus) {
    if (!authUser || actionBusy) return;
    const item = schedules.find((shift) => shift.id === id);
    const deleting = status === "CANCELLED" && item?.managementStatus === "DRAFT";
    if (!window.confirm(deleting ? "Xóa ca gác nháp này?" : status === "CANCELLED" ? "Hủy ca gác này?" : "Công bố ca gác này?")) return;
    setActionBusy(true);
    try {
      if (deleting) await api.guards.remove(id); else await api.guards.status(id, status as "PUBLISHED" | "CANCELLED");
      setSelectedId(null); setToast(deleting ? "Đã xóa ca nháp" : status === "CANCELLED" ? "Đã hủy ca gác" : "Đã công bố ca gác"); await loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined);
    } catch (cause) { handleActionError(cause, setAuthenticated, setDataError); }
    finally { setActionBusy(false); }
  }
  async function acknowledge(id: number) { if (!authUser || actionBusy) return; setActionBusy(true); try { await api.guards.acknowledge(id); setToast("Đã xác nhận xem lịch"); await loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined); } catch (cause) { handleActionError(cause, setAuthenticated, setDataError); } finally { setActionBusy(false); } }
  async function submitRecord(id: number, record: GuardRecord) {
    if (!authUser || actionBusy) return;
    const resultCondition = record.condition === "Bình thường" ? "NORMAL" : record.condition === "Có sự việc" ? "INCIDENT" : "REQUIRES_REPORT";
    setActionBusy(true);
    try { await api.guards.result(id, { resultCondition, resultContent: record.content, incidentNote: resultCondition === "NORMAL" ? null : record.incident }); setToast("Đã gửi ghi nhận ca gác"); setSelectedId(null); await loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined); }
    catch (cause) { handleActionError(cause, setAuthenticated, setDataError); }
    finally { setActionBusy(false); }
  }
  function changeWeek(days: number) {
    if (authUser && weekStart) {
      setFollowCurrentWeek(false);
      void loadData(authUser, addDays(weekStart, days));
    }
  }
  if (!authReady) return <main className="auth-loading" aria-label="Đang khôi phục phiên đăng nhập"><span/></main>;
  if (!authenticated) return <LoginScreen onLogin={login}/>
  return <div className="app-shell">
    <header className="topbar"><button className="mobile-menu" onClick={() => setMobileNav((v) => !v)} aria-label="Mở menu"><Icon name="menu" /></button><UnitLogo compact/><div className="brand-copy"><strong>QUẢN LÝ TRUNG ĐỘI DÂN QUÂN</strong><span>Ban CHQS phường Bắc Cam Ranh · Dân quân thường trực</span></div><div className="topbar-spacer"/><BusinessClock value={dashboard?.businessTime} frozen={dashboard?.simulationEnabled} className="business-clock"/>{isCommander && <SimulationControls onChanged={() => { setFollowCurrentWeek(true); if (authUser) void loadData(authUser); }}/>}<NotificationBell/><div className="user-select-wrap"><div className="avatar">{isCommander ? "CH" : currentUser.split(" ").slice(-2).map((word) => word[0]).join("")}</div><div className="select-copy"><span>{isCommander ? "Chỉ huy · Ban CHQS phường" : "Chiến sĩ · Dân quân thường trực"}</span><strong>{isCommander ? "Chỉ huy" : currentUser}</strong></div></div><button className="logout-button" onClick={() => void logout()}>Đăng xuất</button></header>
    <AppSidebar active={view as SidebarItem} commander={isCommander} open={mobileNav} onClose={() => setMobileNav(false)} onSelect={(item) => setView(item as View)}/>
    <main className="main-content">
      {dataLoading && <div className="panel data-state">Đang tải dữ liệu...</div>}
      {dataError && <div className="panel data-state error">{dataError} <button className="text-button" onClick={() => authUser && void loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined)}>Thử lại</button></div>}
      {!dataLoading && !dataError && view === "overview" && (isCommander ? <CommanderDashboard schedules={schedules} dashboard={dashboard} workItems={workItems} areaItems={areaItems} notifications={notifications} activities={activityLogs} onOpen={setSelectedId} onOpenWork={setWorkItemDetail} onNavigate={() => setView("schedules")}/> : <SoldierDashboard soldier={currentUser} dashboard={dashboard} schedules={schedules} workItems={workItems} areaItems={areaItems} notifications={notifications} onOpen={setSelectedId} onOpenWork={setWorkItemDetail} onNavigate={() => setView("schedules")}/>)}
      {view === "schedules" && (isCommander ? <><CommanderSchedules schedules={schedules} onOpen={setSelectedId} onEdit={openEdit} onCreate={openCreate}/><WeeklyRosterBoard roster={weeklyRoster} onOpen={(dayIndex, shiftIndex) => setSelectedWeeklySlot({ dayIndex, shiftIndex })} onPrevious={() => changeWeek(-7)} onNext={() => changeWeek(7)}/></> : <><MySchedules soldier={currentUser} schedules={schedules} onOpen={setSelectedId}/><div className="soldier-roster-heading"><span>THỜI KHÓA BIỂU TOÀN ĐƠN VỊ</span><h2>Toàn bộ ca trực trong tuần</h2><p>Chiến sĩ có thể xem phân công của tất cả các ca và mở từng ô để xem ghi chú bàn giao.</p></div><WeeklyRosterBoard roster={weeklyRoster} onOpen={(dayIndex, shiftIndex) => setSelectedWeeklySlot({ dayIndex, shiftIndex })} onPrevious={() => changeWeek(-7)} onNext={() => changeWeek(7)}/></>)}
      {!dataLoading && view === "soldiers" && isCommander && <SoldierDirectory soldiers={soldiers} onCreated={() => authUser ? loadData(authUser, followCurrentWeek ? undefined : weekStart || undefined) : Promise.resolve()}/>}
    </main>
    <SiteFooter withSidebar/>
    {guardWeekOpen && <WeekStartModal title="Tạo lịch gác tuần" initialDate={weekStart || dashboard?.weekStartDate || ""} busy={actionBusy} onCancel={() => setGuardWeekOpen(false)} onConfirm={(date) => void createGuardWeek(date)}/>}
    {editorOpen && <ScheduleEditor soldiers={soldiers} defaultDate={dashboard?.date} schedule={editingId ? schedules.find((item) => item.id === editingId) : undefined} busy={actionBusy} onClose={() => setEditorOpen(false)} onSave={saveSchedule}/>}
      {selectedWeeklySlot && <WeeklySlotDetail day={weeklyRoster[selectedWeeklySlot.dayIndex]} shiftIndex={selectedWeeklySlot.shiftIndex} onClose={() => setSelectedWeeklySlot(null)}/>}
    {selected && <ScheduleDetail schedule={!isCommander && selected.occurrenceStatus === "COMPLETED" && !selected.guardRecord ? { ...selected, status: "PUBLISHED" } : selected} isCommander={isCommander} onClose={() => setSelectedId(null)} onEdit={() => openEdit(selected.id)} onStatus={updateStatus} onAcknowledge={acknowledge} onRecord={submitRecord}/>}
    {selected && isCommander && <AssignmentProgress schedule={selected}/>}
    {workItemDetail && <WorkItemDetail
      item={workItemDetail}
      onClose={() => { setWorkItemDetail(null); window.history.replaceState({}, "", "/"); }}
    />}
    {actionBusy && !editorOpen && !guardWeekOpen && <div className="modal-layer"><button className="modal-backdrop" disabled aria-label="Đang xử lý"/><div className="module-confirm"><h2>Đang xử lý...</h2><p>Vui lòng chờ hệ thống cập nhật dữ liệu.</p></div></div>}
    {toast && <div className="toast"><span><Icon name="check" size={17}/></span>{toast}</div>}
  </div>;
}

function addDays(date: string, days: number) {
  const value = new Date(`${date}T12:00:00+07:00`); value.setUTCDate(value.getUTCDate() + days); return value.toISOString().slice(0, 10);
}

function mapGuardShift(item: GuardShift, currentUsername?: string): GuardSchedule {
  const relevant = currentUsername ? item.assignments.find((assignment) => assignment.soldier.username === currentUsername) : null;
  const result = relevant?.status === "COMPLETED" ? relevant : undefined;
  const condition = result?.resultCondition === "INCIDENT" ? "Có sự việc" : result?.resultCondition === "REQUIRES_REPORT" ? "Cần báo cáo" : "Bình thường";
  return {
    id: item.id, date: item.date, startTime: item.startTime.slice(0, 5), endTime: item.endTime.slice(0, 5), startAt: item.startAt, endAt: item.endAt,
    location: item.location, assignedSoldiers: item.assignedSoldiers.map((soldier) => soldier.fullName), soldierIds: item.assignedSoldiers.map((soldier) => soldier.id),
    status: item.status, managementStatus: item.managementStatus, occurrenceStatus: item.occurrenceStatus, note: item.note ?? "", acknowledged: relevant ? relevant.acknowledgedAt !== null : item.acknowledged,
    hasIncident: item.hasIncident, assignments: item.assignments.map((assignment) => ({ soldierId: assignment.soldier.id, username: assignment.soldier.username, fullName: assignment.soldier.fullName, acknowledgedAt: assignment.acknowledgedAt, status: assignment.status, resultCondition: assignment.resultCondition, resultContent: assignment.resultContent, incidentNote: assignment.incidentNote, completedAt: assignment.completedAt })),
    guardRecord: result ? { condition, content: result.resultContent ?? "", incident: result.incidentNote ?? "Không phát sinh.", submittedAt: result.completedAt ?? "" } : undefined,
  };
}

function buildWeeklyRoster(schedules: GuardSchedule[], from: string): WeeklyRosterDay[] {
  if (!from) return [];
  const starts = [7, 9, 11, 13, 15, 17, 19, 21, 23, 1, 3, 5];
  return Array.from({ length: 7 }, (_, dayIndex) => {
    const date = addDays(from, dayIndex);
    const assignments = Array(12).fill("") as string[];
    const slots: NonNullable<WeeklyRosterDay["slots"]> = Array.from({ length: 12 }, () => ({ id: null, status: "upcoming", label: "Chưa phân công", note: "Chưa có ca gác trong khung giờ này.", location: "Cổng chính" }));
    schedules.filter((item) => item.date === date && item.status !== "CANCELLED").forEach((item) => {
      const hour = Number(item.startTime.slice(0, 2));
      const index = starts.reduce((best, value, candidate) => Math.abs(value - hour) < Math.abs(starts[best] - hour) ? candidate : best, 0);
      assignments[index] = item.assignedSoldiers.join(", ");
      const incident = item.assignments.find((assignment) => assignment.incidentNote)?.incidentNote;
      slots[index] = { id: item.id, status: item.hasIncident ? "incident" : item.occurrenceStatus === "COMPLETED" ? "completed" : "upcoming", label: item.hasIncident ? "Có sự cố" : item.occurrenceStatus === "COMPLETED" ? "Đã trực" : item.occurrenceStatus === "ONGOING" ? "Đang trực" : "Chưa trực", note: incident || item.guardRecord?.content || item.note || "Không có ghi chú.", location: item.location };
    });
    return { date, dutyOfficer: "Dữ liệu ca gác", assignments, slots };
  });
}

function handleActionError(cause: unknown, setAuthenticated: (value: boolean) => void, setError: (value: string) => void) {
  if (cause instanceof ApiError && cause.status === 401) setAuthenticated(false);
  else setError(cause instanceof Error ? cause.message : "Thao tác không thành công.");
}

function formatConflictWarning(messages: string[]) {
  const unique = [...new Set(messages)];
  return `Phát hiện ${unique.length} xung đột phân công:\n\n${unique.map((message) => `• ${message}`).join("\n")}\n\nBạn vẫn muốn lưu phân công này?`;
}

function PageHeading({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: ReactNode }) { return <div className="page-heading"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</div>; }
function StatCard({ icon, label, value, note, tone }: { icon: IconName; label: string; value: number | string; note: string; tone: string }) { const display = typeof value === "number" ? value.toString().padStart(2, "0") : value; return <div className="stat-card"><div className={`stat-icon ${tone}`}><Icon name={icon}/></div><div className="stat-copy"><span>{label}</span><strong>{display}</strong><small>{note}</small></div></div>; }

type WeeklySlotStatus = "completed" | "incident" | "upcoming";

function getWeeklySlotInfo(day: WeeklyRosterDay, shiftIndex: number): { status: WeeklySlotStatus; label: string; note: string; id: number | null; location: string } {
  return day.slots?.[shiftIndex] ?? { status: "upcoming", label: "Chưa phân công", note: "Chưa có ca gác trong khung giờ này.", id: null, location: "Cổng chính" };
}

function WeeklyRosterBoard({ roster, onOpen, onPrevious, onNext }: { roster: WeeklyRosterDay[]; onOpen: (dayIndex: number, shiftIndex: number) => void; onPrevious: () => void; onNext: () => void }) {
  return <section className="panel weekly-roster">
    <div className="weekly-roster-head">
      <div><span>BAN CHQS PHƯỜNG BẮC CAM RANH · TRUNG ĐỘI DÂN QUÂN THƯỜNG TRỰC</span><h2>Lịch gác cổng</h2><p>Từ {shortDate(roster[0]?.date ?? "")} đến {shortDate(roster[6]?.date ?? "")}</p></div>
      <div><div className="week-legend"><span><i className="legend-completed"/>Đã trực</span><span><i className="legend-incident"/>Có sự cố</span><span><i className="legend-upcoming"/>Chưa trực</span></div><div className="week-navigation"><button className="btn btn-outline" onClick={onPrevious}>‹ Tuần trước</button><button className="btn btn-outline" onClick={onNext}>Tuần sau ›</button></div></div>
    </div>
    <div className="weekly-roster-scroll"><table className="weekly-table"><thead><tr><th>Thời gian</th>{roster.map((day) => <th key={day.date}>{shortDate(day.date)}<small>{day.dutyOfficer}</small></th>)}</tr></thead><tbody>{WEEKLY_SHIFT_TIMES.map((time, shiftIndex) => <tr key={time}><th>{time}</th>{roster.map((day, dayIndex) => {
      const info = getWeeklySlotInfo(day, shiftIndex);
      const assignedNames = day.assignments[shiftIndex]?.split(", ").filter(Boolean) ?? [];
      return <td key={`${day.date}-${time}`}><button type="button" className={`duty-cell duty-${info.status}`} onClick={() => onOpen(dayIndex, shiftIndex)}><strong className="duty-names">{assignedNames.length ? assignedNames.map((name) => <span key={name} title={name}>{name}</span>) : <span>—</span>}</strong><small title={info.label}>{info.label}</small></button></td>;
    })}</tr>)}</tbody></table></div>
    <div className="weekly-roster-note"><span><Icon name="alert" size={16}/></span><p><strong>Quy định bàn giao:</strong> Có mặt trước giờ nhận ca 10 phút, kiểm tra trang bị và ký nhận sổ trực.</p></div>
  </section>;
}
function dashboardItems(schedules: WorkSchedule[], user: AuthUser) {
  return schedules
    .filter((schedule) => schedule.status === "PUBLISHED")
    .flatMap((schedule) => schedule.items)
    .filter((item) => user.role === "COMMANDER" || item.assignedSoldiers.some((soldier) => soldier.id === user.id));
}

function taskPeriod(item: WorkItem) {
  if (item.allDay || item.period === "ALL_DAY") return "Cả ngày";
  const period = item.period === "MORNING" ? "Sáng" : item.period === "AFTERNOON" ? "Chiều" : item.period === "EVENING" ? "Tối" : "Theo giờ";
  return `${period} · ${item.startTime?.slice(0, 5) ?? "--:--"}–${item.endTime?.slice(0, 5) ?? "--:--"}`;
}

function prioritizedTasks(items: WorkItem[], today?: string) {
  const priority = { ONGOING: 0, UPCOMING: 1, COMPLETED: 2 } as const;
  return [...items]
    .filter((item) => item.workDate >= (today ?? "") || item.workDate === today)
    .sort((a, b) => Number(b.workDate === today) - Number(a.workDate === today)
      || priority[a.occurrenceStatus] - priority[b.occurrenceStatus]
      || `${a.workDate}${a.startTime ?? ""}`.localeCompare(`${b.workDate}${b.startTime ?? ""}`))
    .slice(0, 6);
}

function TaskOverview({ title, kicker, items, today, href, onOpen }: { title: string; kicker: string; items: WorkItem[]; today?: string; href?: string; onOpen: (item: WorkItem) => void }) {
  const visible = prioritizedTasks(items, today);
  const statusLabel = (status: WorkItem["occurrenceStatus"]) => status === "ONGOING" ? "Đang thực hiện" : status === "COMPLETED" ? "Đã hoàn thành" : "Sắp thực hiện";
  return <section className="panel dashboard-task-panel"><div className="panel-head"><div><span className="section-kicker">{kicker}</span><h2>{title}</h2></div>{href && <a className="text-button" href={href}>Xem chi tiết <Icon name="arrow" size={15}/></a>}</div><div className="dashboard-task-list">{visible.map((item) => <button key={item.id} onClick={() => onOpen(item)}><span className="task-date"><strong>{item.workDate === today ? "Hôm nay" : shortDate(item.workDate)}</strong><small>{taskPeriod(item)}</small></span><span className="task-content"><strong>{item.content}</strong><small><Icon name="pin" size={13}/>{item.location || "Chưa xác định địa điểm"}</small><small><Icon name="users" size={13}/>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ") || item.participantsDescription || "Chưa phân công"}</small></span><span className={`task-state state-${item.occurrenceStatus.toLowerCase()}`}>{statusLabel(item.occurrenceStatus)}</span><Icon name="chevron" size={16}/></button>)}</div>{!visible.length && <EmptyState>Chưa có nhiệm vụ phù hợp trong thời gian tới.</EmptyState>}</section>;
}

function NotificationOverview({ items }: { items: NotificationItem[] }) {
  const link = (item: NotificationItem) => item.referenceType === "GUARD_SHIFT" ? `/?view=schedules&guardShift=${item.referenceId}` : `/?workItem=${item.referenceId}`;
  return <section className="panel dashboard-notifications"><div className="panel-head"><div><span className="section-kicker">THÔNG TIN CẦN CHÚ Ý</span><h2>Thông báo mới</h2></div><span className="activity-count">{items.filter((item) => !item.readAt).length} chưa đọc</span></div><div>{items.slice(0, 5).map((item) => <a key={item.id} href={link(item)} className={item.readAt ? "read" : "unread"}><i/><span><strong>{item.title}</strong><small>{item.content}</small></span><Icon name="chevron" size={15}/></a>)}</div>{!items.length && <EmptyState>Chưa có thông báo mới.</EmptyState>}</section>;
}

function GuardOverview({ schedules, dashboard, today, onOpen, onNavigate }: { schedules: GuardSchedule[]; dashboard: Dashboard | null; today?: string; onOpen: (id: number) => void; onNavigate: () => void }) {
  const focus = schedules.filter((item) => item.status !== "CANCELLED" && (item.date === today || item.occurrenceStatus === "ONGOING" || item.occurrenceStatus === "UPCOMING")).slice(0, 3);
  return <section className="panel guard-overview"><div className="panel-head"><div><span className="section-kicker">THÔNG TIN TRỰC GÁC</span><h2>Lịch gác tóm tắt</h2></div><button className="text-button" onClick={onNavigate}>Xem lịch gác <Icon name="arrow" size={15}/></button></div><div className="guard-overview-metrics"><span><strong>{dashboard?.shiftsToday ?? 0}</strong>Ca hôm nay</span><span><strong>{dashboard?.ongoingShifts ?? 0}</strong>Đang diễn ra</span><span><strong>{dashboard?.unacknowledgedAssignments ?? 0}</strong>Thiếu xác nhận</span></div><div className="guard-overview-list">{focus.map((item) => <button key={item.id} onClick={() => onOpen(item.id)}><span>{shortDate(item.date)} · {item.startTime}–{item.endTime}</span><strong>{item.assignedSoldiers.join(", ")}</strong><StatusBadge status={item.status}/></button>)}</div>{!focus.length && <EmptyState>Không có ca gác đang diễn ra hoặc sắp tới.</EmptyState>}</section>;
}

function CommanderDashboard({ schedules, dashboard, workItems, areaItems, notifications, activities, onOpen, onOpenWork, onNavigate }: { schedules: GuardSchedule[]; dashboard: Dashboard | null; workItems: WorkItem[]; areaItems: WorkItem[]; notifications: NotificationItem[]; activities: ActivityLog[]; onOpen: (id: number) => void; onOpenWork: (item: WorkItem) => void; onNavigate: () => void }) {
  const dashboardDate = dashboard?.date;
  return <>
    <PageHeading eyebrow="TRUNG TÂM ĐIỀU HÀNH" title="Tổng quan hoạt động đơn vị" description={dashboardDate ? `${formatDate(dashboardDate)} · Theo dõi quân số, công tác tuần và nhiệm vụ địa bàn.` : "Theo dõi quân số, công tác tuần và nhiệm vụ địa bàn."} action={<a className="btn btn-primary" href="/work-schedule"><Icon name="file" size={17}/> Xem lịch công tác</a>}/>
    <section className="dashboard-section-heading"><div><span>01</span><div><strong>Quân số đơn vị</strong><small>Dữ liệu tài khoản chiến sĩ hiện có trong hệ thống</small></div></div></section>
    <section className="stats-grid personnel-stats"><StatCard icon="users" label="Tổng quân số" value={dashboard?.totalSoldiers ?? 0} note="Tổng số chiến sĩ trong đơn vị" tone="navy"/><StatCard icon="check" label="Đang hoạt động" value={dashboard?.totalActiveSoldiers ?? 0} note="Tài khoản chiến sĩ đang hoạt động" tone="green"/></section>
    <TaskOverview title="Lịch công tác tuần" kicker="02 · ƯU TIÊN NHIỆM VỤ HÔM NAY" items={workItems} today={dashboardDate} href="/work-schedule" onOpen={onOpenWork}/>
    <TaskOverview title="Nắm tình hình địa bàn" kicker="03 · PHÂN CÔNG ĐỊA BÀN" items={areaItems} today={dashboardDate} href="/area-monitoring" onOpen={onOpenWork}/>
    <div className="dashboard-information-grid"><NotificationOverview items={notifications}/><ActivityTimeline activities={activities}/></div>
    <GuardOverview schedules={schedules} dashboard={dashboard} today={dashboardDate} onOpen={onOpen} onNavigate={onNavigate}/>
  </>;
}

const ACTIVITY_LABELS: Record<ActivityAction, string> = {
  GUARD_CREATED: "Tạo ca trực",
  GUARD_SCHEDULE_CHANGED: "Đổi thông tin ca trực",
  GUARD_ASSIGNMENTS_CHANGED: "Đổi phân công ca trực",
  GUARD_CANCELLED: "Hủy ca trực",
  WORK_SCHEDULE_CREATED: "Tạo lịch công tác",
  WORK_SCHEDULE_UPDATED: "Chỉnh sửa lịch công tác",
  WORK_SCHEDULE_PUBLISHED: "Công bố lịch công tác",
  WORK_ITEM_CREATED: "Thêm nhiệm vụ",
  WORK_ITEM_UPDATED: "Chỉnh sửa nhiệm vụ",
  WORK_ITEM_REMOVED: "Xóa nhiệm vụ",
  GUARD_ACKNOWLEDGED: "Xác nhận nhận lịch",
  GUARD_RESULT_SUBMITTED: "Gửi kết quả nhiệm vụ",
  GUARD_INCIDENT_REPORTED: "Ghi nhận sự cố",
};

function ActivityTimeline({ activities }: { activities: ActivityLog[] }) {
  const targetLabel = (activity: ActivityLog) => activity.targetType === "GUARD_SHIFT" ? "Ca trực" : activity.targetType === "WORK_SCHEDULE" ? "Lịch công tác" : "Nhiệm vụ";
  const time = (value: string) => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
  return <section className="panel activity-panel">
    <div className="panel-head"><div><span className="section-kicker">TRUY VẾT NGHIỆP VỤ</span><h2>Lịch sử hoạt động gần đây</h2></div><span className="activity-count">{activities.length} hoạt động mới nhất</span></div>
    <div className="activity-list">{activities.map((activity) => <article className="activity-row" key={activity.id}>
      <span className="activity-icon"><Icon name={activity.action === "GUARD_INCIDENT_REPORTED" ? "alert" : "file"} size={17}/></span>
      <div className="activity-body"><div><strong>{activity.actor.fullName}</strong><span>{ACTIVITY_LABELS[activity.action]}</span></div><p>{activity.description}</p><div className="activity-meta"><span>{targetLabel(activity)} #{activity.targetId}</span><time dateTime={activity.createdAt}>{time(activity.createdAt)}</time></div></div>
    </article>)}</div>
    {!activities.length && <EmptyState>Chưa có hoạt động nghiệp vụ nào được ghi nhận.</EmptyState>}
  </section>;
}

function CommanderSchedules({ schedules, onOpen, onEdit, onCreate }: { schedules: GuardSchedule[]; onOpen: (id: number) => void; onEdit: (id: number) => void; onCreate: () => void }) {
  const [date, setDate] = useState(""); const [status, setStatus] = useState(""); const filtered = schedules.filter((item) => (!date || item.date === date) && (!status || item.status === status));
  return <><PageHeading eyebrow="QUẢN LÝ NHIỆM VỤ" title="Lịch gác" description="Tạo, phân công và theo dõi toàn bộ ca gác của đơn vị." action={<button className="btn btn-primary" onClick={onCreate}><Icon name="plus" size={17}/> Tạo ca gác</button>}/><section className="panel table-panel"><div className="filter-bar"><div className="filter-title"><Icon name="calendar"/><div><strong>Lịch gác đơn vị</strong><span>{filtered.length} ca được hiển thị</span></div></div><div className="filters"><label><span>Ngày trực</span><input type="date" value={date} onChange={(e) => setDate(e.target.value)}/></label><label><span>Trạng thái</span><select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Tất cả trạng thái</option><option value="DRAFT">Nháp</option><option value="PUBLISHED">Đã công bố</option><option value="COMPLETED">Đã hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></label>{(date || status) && <button className="clear-filter" onClick={() => { setDate(""); setStatus(""); }}>Xóa lọc</button>}</div></div><div className="table-scroll"><table><thead><tr><th>NGÀY TRỰC</th><th>THỜI GIAN</th><th>VỊ TRÍ</th><th>CHIẾN SĨ</th><th>XÁC NHẬN</th><th>TRẠNG THÁI</th><th/></tr></thead><tbody>{filtered.map((item) => <tr key={item.id} onClick={() => onOpen(item.id)}><td><strong>{shortDate(item.date)}</strong><small>{formatDate(item.date)}</small></td><td><span className="time-pill"><Icon name="clock" size={14}/>{item.startTime} – {item.endTime}</span></td><td>{item.location}</td><td><div className="soldier-cell"><span>{item.assignedSoldiers[0].split(" ").slice(-2).map((word) => word[0]).join("")}</span><strong>{item.assignedSoldiers.join(", ")}</strong></div></td><td><span className={item.acknowledged ? "ack yes" : "ack"}>{item.acknowledged ? "Đã xem" : "Chưa xem"}</span></td><td><StatusBadge status={item.status}/></td><td><button className="icon-button" onClick={(e) => { e.stopPropagation(); onEdit(item.id); }} aria-label="Chỉnh sửa"><Icon name="edit" size={17}/></button></td></tr>)}</tbody></table></div>{!filtered.length && <EmptyState>Không có ca gác phù hợp với bộ lọc.</EmptyState>}</section></>;
}

function SoldierDashboard({ soldier, dashboard, schedules, workItems, areaItems, notifications, onOpen, onOpenWork, onNavigate }: { soldier: string; dashboard: Dashboard | null; schedules: GuardSchedule[]; workItems: WorkItem[]; areaItems: WorkItem[]; notifications: NotificationItem[]; onOpen: (id: number) => void; onOpenWork: (item: WorkItem) => void; onNavigate: () => void }) {
  return <>
    <PageHeading eyebrow="TRANG CÁ NHÂN" title={`Chào đồng chí ${soldier.split(" ").slice(-2).join(" ")}`} description={dashboard?.date ? `${formatDate(dashboard.date)} · Các nhiệm vụ được phân công cho đồng chí.` : "Các nhiệm vụ được phân công cho đồng chí."}/>
    <section className="stats-grid soldier-dashboard-stats"><StatCard icon="file" label="Nhiệm vụ công tác" value={dashboard?.weeklyWorkItems ?? 0} note="Được phân công trong tuần" tone="navy"/><StatCard icon="eye" label="Cần xác nhận" value={dashboard?.unacknowledgedAssignments ?? 0} note="Lịch trực chưa tiếp nhận" tone="orange"/><StatCard icon="bell" label="Thông báo mới" value={dashboard?.unreadNotifications ?? 0} note="Thông báo chưa đọc của đồng chí" tone="gold"/></section>
    <TaskOverview title="Nhiệm vụ công tác của tôi" kicker="01 · CÔNG VIỆC HÔM NAY VÀ SẮP TỚI" items={workItems} today={dashboard?.date} onOpen={onOpenWork}/>
    <TaskOverview title="Phân công nắm tình hình địa bàn" kicker="02 · NHIỆM VỤ ĐỊA BÀN CỦA TÔI" items={areaItems} today={dashboard?.date} onOpen={onOpenWork}/>
    <NotificationOverview items={notifications}/>
    <GuardOverview schedules={schedules} dashboard={dashboard} today={dashboard?.date} onOpen={onOpen} onNavigate={onNavigate}/>
  </>;
}

function MySchedules({ soldier, schedules, onOpen }: { soldier: string; schedules: GuardSchedule[]; onOpen: (id: number) => void }) {
  const [tab, setTab] = useState<"upcoming" | "completed">("upcoming"); const own = schedules.filter((item) => item.assignedSoldiers.includes(soldier) && ["PUBLISHED", "COMPLETED"].includes(item.status)); const filtered = own.filter((item) => tab === "upcoming" ? item.status === "PUBLISHED" : item.status === "COMPLETED");
  return <><PageHeading eyebrow="NHIỆM VỤ CÁ NHÂN" title="Lịch gác của tôi" description="Các ca gác đã được Chỉ huy công bố và phân công cho đồng chí."/><div className="tabs"><button className={tab === "upcoming" ? "active" : ""} onClick={() => setTab("upcoming")}>Sắp tới <span>{own.filter((item) => item.status === "PUBLISHED").length}</span></button><button className={tab === "completed" ? "active" : ""} onClick={() => setTab("completed")}>Đã hoàn thành <span>{own.filter((item) => item.status === "COMPLETED").length}</span></button></div><div className="my-schedule-grid">{filtered.map((item) => <button className="my-shift-card" key={item.id} onClick={() => onOpen(item.id)}><div className="my-card-top"><div><span>{shortDate(item.date)}</span><strong>{formatDate(item.date)}</strong></div><StatusBadge status={item.status}/></div><div className="my-card-time"><Icon name="clock" size={21}/><strong>{item.startTime} – {item.endTime}</strong></div><div className="my-card-place"><Icon name="pin" size={17}/><div><span>Vị trí trực</span><strong>{item.location}</strong></div></div><div className="my-card-foot"><span className={item.acknowledged ? "ack yes" : "ack"}>{item.acknowledged ? <Icon name="check" size={14}/> : <Icon name="eye" size={14}/>} {item.acknowledged ? "Đã xác nhận xem" : "Chưa xác nhận xem"}</span><Icon name="chevron" size={17}/></div></button>)}</div>{!filtered.length && <div className="panel"><EmptyState>{tab === "upcoming" ? "Chưa có ca gác sắp tới." : "Chưa có ca gác đã hoàn thành."}</EmptyState></div>}</>;
}

function SoldierDirectory({ soldiers, onCreated }: { soldiers: Soldier[]; onCreated: () => Promise<unknown> }) {
  const [search, setSearch] = useState(""); const [activeOnly, setActiveOnly] = useState(false);
  const [createOpen, setCreateOpen] = useState(false); const [busy, setBusy] = useState(false); const [formError, setFormError] = useState("");
  const [form, setForm] = useState({ username: "", password: "Demo@123", fullName: "", rankOrPosition: "Chiến sĩ DQTT", active: true });
  async function createSoldier(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true); setFormError("");
    try {
      await api.createSoldier(form); await onCreated(); setCreateOpen(false);
      setForm({ username: "", password: "Demo@123", fullName: "", rankOrPosition: "Chiến sĩ DQTT", active: true });
    } catch (cause) { setFormError(cause instanceof Error ? cause.message : "Không thể tạo chiến sĩ."); }
    finally { setBusy(false); }
  }
  const filtered = soldiers.filter((item) => (!activeOnly || item.active) && (!search || item.fullName.toLocaleLowerCase("vi").includes(search.toLocaleLowerCase("vi")) || item.username.toLowerCase().includes(search.toLowerCase())));
  return <>
    <PageHeading eyebrow="QUẢN LÝ QUÂN SỐ" title="Danh sách chiến sĩ" description="Thông tin phân công lịch gác của Trung đội Dân quân thường trực." action={<button className="btn btn-primary" onClick={() => setCreateOpen(true)}><Icon name="plus" size={17}/> Thêm chiến sĩ</button>}/>
    <section className="panel soldier-directory">
      <div className="directory-head"><strong>Quân số trực</strong><span>{filtered.length}/{soldiers.length} đồng chí</span></div>
      <div className="filters directory-filters"><label><span>Tìm kiếm</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tên hoặc tài khoản"/></label><label><span>Trạng thái</span><select value={activeOnly ? "active" : "all"} onChange={(event) => setActiveOnly(event.target.value === "active")}><option value="all">Tất cả</option><option value="active">Đang hoạt động</option></select></label></div>
      <div className="directory-grid">{filtered.map((soldier) => <div className="person-card" key={soldier.id}><div className="person-avatar">{soldier.fullName.split(" ").slice(-2).map((word) => word[0]).join("")}</div><div><strong>{soldier.fullName}</strong><span>@{soldier.username} · {soldier.active ? "Đang hoạt động" : "Đã vô hiệu hóa"}</span></div><dl><div><dt>Ca được phân công</dt><dd>{soldier.assignedShiftCount}</dd></div><div><dt>Đã hoàn thành</dt><dd>{soldier.completedShiftCount}</dd></div></dl><small>{soldier.rankOrPosition || "Chiến sĩ dân quân"}</small></div>)}</div>
      {!filtered.length && <EmptyState>Không tìm thấy chiến sĩ phù hợp.</EmptyState>}
    </section>
    {createOpen && <div className="modal-layer"><button className="modal-backdrop" disabled={busy} onClick={() => setCreateOpen(false)} aria-label="Đóng"/><form className="modal soldier-create-modal" onSubmit={createSoldier}><div className="modal-head"><div><span>QUẢN LÝ QUÂN SỐ</span><h2>Thêm chiến sĩ mới</h2><p>Tạo tài khoản cơ bản cho chiến sĩ thuộc đơn vị.</p></div><button type="button" className="modal-close" disabled={busy} onClick={() => setCreateOpen(false)}><Icon name="close"/></button></div><div className="form-body"><div className="form-grid two"><label><span>Họ và tên <b>*</b></span><input required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })}/></label><label><span>Tên đăng nhập <b>*</b></span><input required minLength={3} pattern="[a-zA-Z0-9._-]+" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })}/></label><label><span>Mật khẩu <b>*</b></span><input required type="password" minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })}/></label><label><span>Chức vụ</span><input value={form.rankOrPosition} onChange={(event) => setForm({ ...form, rankOrPosition: event.target.value })}/></label></div>{formError && <p className="form-error">{formError}</p>}</div><div className="modal-actions"><button type="button" className="btn btn-secondary" disabled={busy} onClick={() => setCreateOpen(false)}>Hủy</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? "Đang tạo..." : "Tạo chiến sĩ"}</button></div></form></div>}
  </>;
}

function WeeklySlotDetail({ day, shiftIndex, onClose }: { day: WeeklyRosterDay; shiftIndex: number; onClose: () => void }) {
  const info = getWeeklySlotInfo(day, shiftIndex);
  return <div className="modal-layer"><button className="modal-backdrop" onClick={onClose} aria-label="Đóng"/><div className="modal slot-detail-modal"><div className="modal-head"><div><span>CHI TIẾT CA GÁC CỔNG</span><h2>{WEEKLY_SHIFT_TIMES[shiftIndex]}</h2><p>{formatDate(day.date)}</p></div><button type="button" className="modal-close" onClick={onClose}><Icon name="close"/></button></div><div className="slot-detail-body"><div className={`slot-status-banner ${info.status}`}><span><Icon name={info.status === "incident" ? "alert" : info.status === "completed" ? "check" : "clock"}/></span><div><strong>{info.label}</strong><small>{info.status === "incident" ? "Ca gác có nội dung cần Chỉ huy xem lại" : info.status === "completed" ? "Chiến sĩ đã hoàn thành ca trực" : info.id ? "Ca gác chưa đến thời gian thực hiện" : "Chưa có phân công"}</small></div></div><dl className="slot-facts"><div><dt>Chiến sĩ được phân công</dt><dd>{day.assignments[shiftIndex] || "Chưa phân công"}</dd></div><div><dt>Vị trí</dt><dd>{info.location}</dd></div></dl><div className="soldier-note"><span>GHI NHẬN CỦA CHIẾN SĨ</span><p>{info.note}</p></div></div><div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Đóng</button></div></div></div>;
}

function WorkItemDetail({ item, onClose }: { item: WorkItem; onClose: () => void }) {
  const period = item.allDay ? "Cả ngày" : item.period === "MORNING" ? "Buổi sáng" : item.period === "AFTERNOON" ? "Buổi chiều" : "Buổi tối";
  return <div className="modal-layer"><button className="modal-backdrop" onClick={onClose} aria-label="Đóng"/><div className="drawer"><div className="drawer-head"><div><span>CHI TIẾT NHIỆM VỤ</span><h2>{item.content}</h2></div><button className="modal-close" onClick={onClose}><Icon name="close"/></button></div><div className="drawer-body"><div className="detail-section"><span className="detail-label">THỜI GIAN VÀ ĐỊA ĐIỂM</span><div className="detail-list"><div><span className="detail-list-icon"><Icon name="calendar"/></span><p><small>Ngày thực hiện</small><strong>{formatDate(item.workDate)} · {period}</strong></p></div><div><span className="detail-list-icon"><Icon name="clock"/></span><p><small>Khung giờ</small><strong>{item.allDay ? "Cả ngày" : `${item.startTime?.slice(0, 5)} – ${item.endTime?.slice(0, 5)}`}</strong></p></div><div><span className="detail-list-icon"><Icon name="pin"/></span><p><small>Địa điểm / khu vực</small><strong>{item.location || "Chưa xác định"}</strong></p></div></div></div><div className="detail-section"><span className="detail-label">LỰC LƯỢNG THỰC HIỆN</span><div className="detail-list"><div><span className="detail-list-icon"><Icon name="users"/></span><p><small>Chiến sĩ được phân công</small><strong>{item.assignedSoldiers.map((soldier) => soldier.fullName).join(", ") || "Chưa phân công"}</strong></p></div><div><span className="detail-list-icon"><Icon name="file"/></span><p><small>Ghi chú</small><strong>{item.note || item.preparation || "Không có ghi chú"}</strong></p></div></div></div></div><div className="drawer-actions"><button className="btn btn-secondary" onClick={onClose}>Đóng</button></div></div></div>;
}

function AssignmentProgress({ schedule }: { schedule: GuardSchedule }) {
  return <aside className="assignment-progress-panel">
    <div className="assignment-progress-head"><span>THEO DÕI TỪNG CHIẾN SĨ</span><strong>{schedule.assignments.length} người được phân công</strong></div>
    <div className="assignment-progress-list">{schedule.assignments.map((assignment) => <article key={assignment.soldierId} className={assignment.incidentNote ? "has-incident" : ""}>
      <div><strong>{assignment.fullName}</strong><small>@{assignment.username}</small></div>
      <span className={assignment.acknowledgedAt ? "person-state confirmed" : "person-state"}>{assignment.acknowledgedAt ? "Đã xác nhận" : "Chưa xác nhận"}</span>
      <dl><div><dt>Thực hiện</dt><dd>{assignment.status === "COMPLETED" ? "Đã gửi kết quả" : "Chưa hoàn tất"}</dd></div>{assignment.resultCondition && <div><dt>Kết quả</dt><dd>{assignment.resultCondition === "NORMAL" ? "Bình thường" : assignment.resultCondition === "INCIDENT" ? "Có sự cố" : "Cần báo cáo"}</dd></div>}{assignment.resultContent && <div className="wide"><dt>Nội dung</dt><dd>{assignment.resultContent}</dd></div>}{assignment.incidentNote && <div className="wide incident"><dt>Sự cố / vấn đề</dt><dd>{assignment.incidentNote}</dd></div>}</dl>
    </article>)}</div>
  </aside>;
}

function ScheduleEditor({ soldiers, defaultDate, schedule, busy = false, onClose, onSave }: { soldiers: Soldier[]; defaultDate?: string; schedule?: GuardSchedule; busy?: boolean; onClose: () => void; onSave: (values: GuardForm, publish: boolean) => void }) {
  const soldierNames = soldiers.filter((item) => item.active).map((item) => item.fullName);
  const SOLDIERS = soldierNames;
  const [form, setForm] = useState<GuardForm>(schedule ? { date: schedule.date, startTime: schedule.startTime, endTime: schedule.endTime, location: schedule.location, assignedSoldiers: [...schedule.assignedSoldiers], status: schedule.status, note: schedule.note } : { ...EMPTY_FORM, date: defaultDate ?? vietnamToday(), assignedSoldiers: soldierNames.slice(0, 2) });
  function submit(event: FormEvent, publish: boolean) { event.preventDefault(); onSave(form, publish); }
  if (busy) return <div className="modal-layer"><button className="modal-backdrop" disabled aria-label="Đang lưu"/><div className="module-confirm"><div className="auth-loading"><span/></div><h2>Đang lưu thay đổi...</h2><p>Vui lòng chờ hệ thống cập nhật dữ liệu ca gác.</p></div></div>;
  return <div className="modal-layer"><button className="modal-backdrop" onClick={onClose} aria-label="Đóng"/><form className="modal editor-modal" onSubmit={(e) => submit(e, false)}><div className="modal-head"><div><span>{schedule ? "CẬP NHẬT PHÂN CÔNG" : "PHÂN CÔNG NHIỆM VỤ"}</span><h2>{schedule ? "Chỉnh sửa ca gác" : "Tạo ca gác mới"}</h2><p>Điền đầy đủ thông tin ca trực và chiến sĩ thực hiện.</p></div><button type="button" className="modal-close" onClick={onClose}><Icon name="close"/></button></div><div className="form-body"><div className="form-section-title"><span>1</span><div><strong>Thời gian thực hiện</strong><small>Ngày và khung giờ của ca gác</small></div></div><div className="form-grid three"><label><span>Ngày trực <b>*</b></span><input required type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })}/></label><label><span>Giờ bắt đầu <b>*</b></span><input required type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })}/></label><label><span>Giờ kết thúc <b>*</b></span><input required type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })}/></label></div><div className="form-section-title"><span>2</span><div><strong>Phân công nhiệm vụ</strong><small>Vị trí và chiến sĩ nhận ca</small></div></div><div className="form-grid three"><label><span>Địa điểm <b>*</b></span><select value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })}>{LOCATIONS.map((item) => <option key={item}>{item}</option>)}</select></label>{form.assignedSoldiers.map((soldier, index) => <label key={index}><span>Chiến sĩ {index + 1} <b>*</b></span><select value={soldier} onChange={(e) => setForm({ ...form, assignedSoldiers: form.assignedSoldiers.map((item, personIndex) => personIndex === index ? e.target.value : item) })}>{SOLDIERS.map((item) => <option key={item} disabled={form.assignedSoldiers.includes(item) && item !== soldier}>{item}</option>)}</select></label>)}</div><label className="full-field"><span>Ghi chú nhiệm vụ</span><textarea rows={3} placeholder="Nhập yêu cầu, lưu ý trong ca gác..." value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })}/></label></div><div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Hủy bỏ</button><div><button type="submit" className="btn btn-outline"><Icon name="file" size={16}/> Lưu nháp</button><button type="button" className="btn btn-primary" onClick={(e) => submit(e, true)}><Icon name="check" size={16}/> {schedule?.status === "PUBLISHED" ? "Lưu thay đổi" : "Công bố lịch"}</button></div></div></form></div>;
}

function ScheduleDetail({ schedule, isCommander, onClose, onEdit, onStatus, onAcknowledge, onRecord }: { schedule: GuardSchedule; isCommander: boolean; onClose: () => void; onEdit: () => void; onStatus: (id: number, status: ScheduleStatus) => void; onAcknowledge: (id: number) => void; onRecord: (id: number, record: GuardRecord) => void }) {
  const [recordMode, setRecordMode] = useState(false); const [condition, setCondition] = useState<GuardRecord["condition"]>("Bình thường"); const [content, setContent] = useState("Ca trực diễn ra bình thường."); const [incident, setIncident] = useState("Không phát sinh."); function sendRecord(event: FormEvent) { event.preventDefault(); onRecord(schedule.id, { condition, content, incident, submittedAt: new Date().toISOString() }); }
  return <div className="modal-layer"><button className="modal-backdrop" onClick={onClose} aria-label="Đóng"/><div className="drawer"><div className="drawer-head"><div><span>CHI TIẾT CA GÁC</span><h2>Mã ca #{schedule.id.toString().slice(-4)}</h2></div><button className="modal-close" onClick={onClose}><Icon name="close"/></button></div><div className="drawer-body"><div className="detail-date"><div><span>{new Date(`${schedule.date}T00:00:00`).toLocaleDateString("vi-VN", { month: "short" }).toUpperCase()}</span><strong>{new Date(`${schedule.date}T00:00:00`).getDate().toString().padStart(2, "0")}</strong></div><section><StatusBadge status={schedule.status}/><h3>{formatDate(schedule.date)}</h3><p><Icon name="clock" size={17}/>{schedule.startTime} – {schedule.endTime}</p></section></div><div className="detail-section"><span className="detail-label">THÔNG TIN PHÂN CÔNG</span><div className="detail-list"><div><span className="detail-list-icon"><Icon name="pin"/></span><p><small>Vị trí trực</small><strong>{schedule.location}</strong></p></div><div><span className="detail-list-icon"><Icon name="users"/></span><p><small>Chiến sĩ thực hiện</small><strong>{schedule.assignedSoldiers.join(", ")}</strong></p></div><div><span className="detail-list-icon"><Icon name="file"/></span><p><small>Ghi chú nhiệm vụ</small><strong>{schedule.note || "Không có ghi chú"}</strong></p></div></div></div><div className="ack-box"><div className={schedule.acknowledged ? "ack-large yes" : "ack-large"}><Icon name={schedule.acknowledged ? "check" : "eye"}/><div><strong>{schedule.acknowledged ? "Đã xác nhận xem lịch" : "Chưa xác nhận xem lịch"}</strong><span>{schedule.acknowledged ? "Chiến sĩ đã tiếp nhận thông tin ca gác." : "Đang chờ chiến sĩ xác nhận."}</span></div></div>{!isCommander && !schedule.acknowledged && <button className="btn btn-primary" onClick={() => onAcknowledge(schedule.id)}>Xác nhận đã xem</button>}</div>{schedule.guardRecord ? <div className="record-result"><div className="record-title"><span><Icon name="check"/></span><div><small>KẾT QUẢ CA GÁC</small><strong>{schedule.guardRecord.condition}</strong></div></div><dl><div><dt>Nội dung ghi nhận</dt><dd>{schedule.guardRecord.content}</dd></div><div><dt>Sự việc phát sinh</dt><dd>{schedule.guardRecord.incident}</dd></div></dl></div> : !isCommander && schedule.status === "PUBLISHED" && (recordMode ? <form className="record-form" onSubmit={sendRecord}><span className="detail-label">GHI NHẬN CA GÁC</span><label><span>Tình trạng</span><select value={condition} onChange={(e) => setCondition(e.target.value as GuardRecord["condition"])}><option>Bình thường</option><option>Có sự việc</option><option>Cần báo cáo</option></select></label><label><span>Nội dung <b>*</b></span><textarea required rows={3} value={content} onChange={(e) => setContent(e.target.value)}/></label><label><span>Sự việc phát sinh</span><textarea rows={2} value={incident} onChange={(e) => setIncident(e.target.value)}/></label><button className="btn btn-primary" type="submit"><Icon name="check" size={16}/> Gửi ghi nhận</button></form> : <button className="record-prompt" onClick={() => setRecordMode(true)}><span><Icon name="file"/></span><div><strong>Ghi nhận kết quả ca gác</strong><small>Hoàn thành báo cáo sau khi kết thúc nhiệm vụ</small></div><Icon name="chevron"/></button>)}</div>{isCommander && <div className="drawer-actions"><button className="btn btn-outline" onClick={onEdit}><Icon name="edit" size={16}/> Chỉnh sửa</button>{schedule.status === "DRAFT" && <button className="btn btn-primary" onClick={() => onStatus(schedule.id, "PUBLISHED")}><Icon name="check" size={16}/> Công bố</button>}{!["CANCELLED", "COMPLETED"].includes(schedule.status) && <button className="btn btn-danger" onClick={() => onStatus(schedule.id, "CANCELLED")}>Hủy lịch</button>}</div>}</div></div>;
}
