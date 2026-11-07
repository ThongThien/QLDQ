const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";
const CACHE_PREFIX = "qldq-cache:v1:";
const memoryCache = new Map<string, { savedAt: number; data: unknown }>();
let cacheUser = "anonymous";

function cacheKey(path: string) { return `${CACHE_PREFIX}${cacheUser}:${path}`; }
function setCacheUser(username: string) {
  if (cacheUser !== username) memoryCache.clear();
  cacheUser = username;
}
function clearCurrentUserCache() {
  const prefix = `${CACHE_PREFIX}${cacheUser}:`;
  memoryCache.clear();
  if (typeof window !== "undefined") {
    Object.keys(window.localStorage).filter((key) => key.startsWith(prefix)).forEach((key) => window.localStorage.removeItem(key));
  }
}
function readCache<T>(path: string) {
  const key = cacheKey(path);
  const memory = memoryCache.get(key) as { savedAt: number; data: T } | undefined;
  if (memory) return memory;
  if (typeof window === "undefined") return undefined;
  try {
    const stored = JSON.parse(window.localStorage.getItem(key) ?? "null") as { savedAt: number; data: T } | null;
    if (stored) memoryCache.set(key, stored);
    return stored ?? undefined;
  } catch { window.localStorage.removeItem(key); return undefined; }
}
function writeCache<T>(path: string, data: T, persist = true) {
  const entry = { savedAt: Date.now(), data };
  const key = cacheKey(path);
  memoryCache.set(key, entry);
  if (persist && typeof window !== "undefined") {
    try { window.localStorage.setItem(key, JSON.stringify(entry)); } catch { /* cache is optional */ }
  }
}

export type UserRole = "COMMANDER" | "SOLDIER";
export type AuthUser = { id: number; username: string; fullName: string; rankOrPosition: string | null; role: UserRole };
export type SoldierBrief = { id: number; username: string; fullName: string; rankOrPosition: string | null; active: boolean };
export type Assignment = { id: number; soldier: SoldierBrief; acknowledgedAt: string | null; status: "ASSIGNED" | "COMPLETED"; resultCondition: "NORMAL" | "INCIDENT" | "REQUIRES_REPORT" | null; resultContent: string | null; incidentNote: string | null; completedAt: string | null };
export type GuardShift = { id: number; startAt: string; endAt: string; date: string; startTime: string; endTime: string; location: string; note: string | null; managementStatus: "DRAFT" | "PUBLISHED" | "CANCELLED"; occurrenceStatus: "UPCOMING" | "ONGOING" | "COMPLETED"; status: "DRAFT" | "PUBLISHED" | "COMPLETED" | "CANCELLED"; createdBy: SoldierBrief; assignedSoldiers: SoldierBrief[]; assignments: Assignment[]; acknowledged: boolean; hasIncident: boolean; createdAt: string; updatedAt: string };
export type GuardShiftInput = { startAt: string; endAt: string; location: string; note: string; status: "DRAFT" | "PUBLISHED"; soldierIds: number[] };
export type GuardWeekResponse = { weekStartDate: string; weekEndDate: string; createdShiftCount: number; assignmentCount: number };
export type Dashboard = { role: UserRole; businessTime: string; simulationEnabled: boolean; date: string; weekStartDate: string; weekEndDate: string; totalSoldiers: number; totalActiveSoldiers: number; shiftsToday: number; shiftsThisWeek: number; completedShifts: number; ongoingShifts: number; upcomingShifts: number; cancelledShifts: number; unacknowledgedAssignments: number; incidentOrReviewAssignments: number; weeklyWorkItems: number; completedWorkItems: number; workCompletionPercent: number; unreadNotifications: number };
export type Soldier = { id: number; username: string; fullName: string; role: "SOLDIER"; rankOrPosition: string | null; active: boolean; assignedShiftCount: number; completedShiftCount: number; createdAt: string; updatedAt: string };
export type SoldierCreateInput = { username: string; password: string; fullName: string; rankOrPosition: string; active: boolean };
export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
export type ScheduleStatus = "DRAFT" | "PUBLISHED";
export type WorkPeriod = "MORNING" | "AFTERNOON" | "EVENING" | "ALL_DAY" | "CUSTOM";
export type WorkItem = { id: number; scheduleId: number; workDate: string; period: WorkPeriod; startTime: string | null; endTime: string | null; allDay: boolean; content: string; leader: string | null; participantsDescription: string | null; location: string | null; preparation: string | null; dutyLeader: string | null; note: string | null; sortOrder: number; occurrenceStatus: "UPCOMING" | "ONGOING" | "COMPLETED"; assignedSoldiers: SoldierBrief[]; createdAt: string; updatedAt: string };
export type WorkSchedule = { id: number; scheduleType: "WEEKLY_WORK" | "AREA_MONITORING"; title: string; weekStartDate: string; weekEndDate: string; weekLabel: string; scopeArea: string | null; generalNote: string | null; status: ScheduleStatus; createdBy: SoldierBrief; items: WorkItem[]; createdAt: string; updatedAt: string };
export type ScheduleInput = { title: string; weekStartDate: string; scopeArea: string | null; generalNote: string; status: ScheduleStatus };
export type WorkItemInput = { workDate: string; period: WorkPeriod; startTime: string | null; endTime: string | null; allDay: boolean; content: string; leader: string | null; participantsDescription: string | null; location: string | null; preparation: string | null; dutyLeader: string | null; note: string | null; sortOrder: number; soldierIds: number[] };
export type NotificationReferenceType = "GUARD_SHIFT" | "WORK_ITEM" | "AREA_MONITORING_ITEM" | "WORK_SCHEDULE";
export type NotificationItem = { id: number; title: string; content: string; type: string; referenceType: NotificationReferenceType; referenceId: number; createdAt: string; readAt: string | null };
export type ConflictWarning = { soldierId: number; soldierName: string; conflictType: "GUARD_SHIFT" | "WORK_ITEM"; referenceId: number; startAt: string; endAt: string; message: string };
export type AssignmentValidationResponse = { warnings: ConflictWarning[]; hasWarnings: boolean };
export type ActivityAction = "GUARD_CREATED" | "GUARD_SCHEDULE_CHANGED" | "GUARD_ASSIGNMENTS_CHANGED" | "GUARD_CANCELLED" | "WORK_SCHEDULE_CREATED" | "WORK_SCHEDULE_UPDATED" | "WORK_SCHEDULE_PUBLISHED" | "WORK_ITEM_CREATED" | "WORK_ITEM_UPDATED" | "WORK_ITEM_REMOVED" | "GUARD_ACKNOWLEDGED" | "GUARD_RESULT_SUBMITTED" | "GUARD_INCIDENT_REPORTED";
export type ActivityLog = { id: number; actor: { id: number; username: string; fullName: string; role: UserRole }; action: ActivityAction; targetType: "GUARD_SHIFT" | "WORK_SCHEDULE" | "WORK_ITEM"; targetId: number; description: string; createdAt: string };
export type TimeSimulationStatus = { enabled: boolean; currentTime: string; zoneId: string };

type CsrfToken = { headerName: string; parameterName: string; token: string };
let csrfToken: CsrfToken | null = null;

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly fieldErrors: Record<string, string> = {}) { super(message); }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const changing = !["GET", "HEAD", "OPTIONS"].includes(method);
  if (changing && !csrfToken) csrfToken = await request<CsrfToken>("/auth/csrf");
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...(changing && csrfToken ? { [csrfToken.headerName]: csrfToken.token } : {}),
      ...init.headers,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string; fieldErrors?: Record<string, string> } | null;
    if (response.status === 401) csrfToken = null;
    throw new ApiError(response.status, body?.message ?? `Yêu cầu thất bại (${response.status})`, body?.fieldErrors);
  }
  if (response.status === 204) {
    if (changing) clearCurrentUserCache();
    return undefined as T;
  }
  const body = await response.json() as T;
  if (changing) clearCurrentUserCache();
  return body;
}

async function cachedRequest<T>(path: string, staleTimeMs: number, persist = true): Promise<T> {
  const cached = readCache<T>(path);
  if (cached && Date.now() - cached.savedAt < staleTimeMs) return cached.data;
  try {
    const data = await request<T>(path);
    writeCache(path, data, persist);
    return data;
  } catch (cause) {
    if (cached && !(cause instanceof ApiError && (cause.status === 401 || cause.status === 403))) return cached.data;
    throw cause;
  }
}

const query = (values: Record<string, string | number | boolean | null | undefined>) => {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== null && value !== undefined && value !== "") params.set(key, String(value)); });
  const result = params.toString();
  return result ? `?${result}` : "";
};

export const api = {
  auth: {
    login: async (username: string, password: string) => { const user = await request<AuthUser>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) }); setCacheUser(user.username); return user; },
    me: async () => { const user = await request<AuthUser>("/auth/me"); setCacheUser(user.username); return user; },
    logout: async () => { await request<void>("/auth/logout", { method: "POST" }); clearCurrentUserCache(); cacheUser = "anonymous"; csrfToken = null; },
  },
  dashboard: () => cachedRequest<Dashboard>("/dashboard", 15_000),
  timeSimulation: {
    status: () => request<TimeSimulationStatus>("/dev/time"),
    set: (simulatedAt: string) => request<TimeSimulationStatus>("/dev/time", { method: "PUT", body: JSON.stringify({ simulatedAt }) }),
    advance: (hours = 0, days = 0) => request<TimeSimulationStatus>("/dev/time/advance", { method: "POST", body: JSON.stringify({ hours, days }) }),
    reset: () => request<TimeSimulationStatus>("/dev/time", { method: "DELETE" }),
  },
  activityLogs: (filters: { from?: string; to?: string; action?: ActivityAction; targetType?: string; targetId?: number; page?: number; size?: number } = {}) => {
    const path = `/activity-logs${query({ page: 0, size: 10, ...filters })}`;
    return cachedRequest<Page<ActivityLog>>(path, 30_000, false);
  },
  workItems: { get: (id: number) => cachedRequest<WorkItem>(`/work-items/${id}`, 30_000) },
  notifications: {
    list: (page = 0, size = 20) => request<Page<NotificationItem>>(`/notifications${query({ page, size })}`),
    unreadCount: () => request<{ unreadCount: number }>("/notifications/unread-count"),
    markRead: (id: number) => request<NotificationItem>(`/notifications/${id}/read`, { method: "PATCH" }),
    markAllRead: () => request<void>("/notifications/read-all", { method: "PATCH" }),
  },
  assignmentValidation: {
    guard: (body: { startAt: string; endAt: string; soldierIds: number[]; excludeGuardShiftId?: number }) =>
      request<AssignmentValidationResponse>("/assignment-validation/guard-shift", { method: "POST", body: JSON.stringify(body) }),
    work: (body: { workDate: string; startTime: string | null; endTime: string | null; allDay: boolean; soldierIds: number[]; excludeWorkItemId?: number }) =>
      request<AssignmentValidationResponse>("/assignment-validation/work-item", { method: "POST", body: JSON.stringify(body) }),
  },
  soldiers: (search = "", active?: boolean) => {
    const path = `/soldiers${query({ search, active, size: 100 })}`;
    return cachedRequest<Page<Soldier>>(path, 5 * 60_000);
  },
  createSoldier: (body: SoldierCreateInput) => request<Soldier>("/soldiers", { method: "POST", body: JSON.stringify(body) }),
  guards: {
    list: (from: string, to: string, soldierId?: number) => { const path = `/guard-shifts${query({ from, to, soldierId, size: 100 })}`; return cachedRequest<Page<GuardShift>>(path, 30_000); },
    get: (id: number) => cachedRequest<GuardShift>(`/guard-shifts/${id}`, 30_000),
    create: (body: GuardShiftInput) => request<GuardShift>("/guard-shifts", { method: "POST", body: JSON.stringify(body) }),
    generateWeek: (weekStartDate: string) => request<GuardWeekResponse>("/guard-shifts/generate-week", { method: "POST", body: JSON.stringify({ weekStartDate }) }),
    update: (id: number, body: GuardShiftInput) => request<GuardShift>(`/guard-shifts/${id}`, { method: "PUT", body: JSON.stringify(body) }),
    status: (id: number, status: "PUBLISHED" | "CANCELLED") => request<GuardShift>(`/guard-shifts/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    remove: (id: number) => request<void>(`/guard-shifts/${id}`, { method: "DELETE" }),
    acknowledge: (id: number) => request<GuardShift>(`/guard-shifts/${id}/acknowledgement`, { method: "POST" }),
    result: (id: number, body: { resultCondition: "NORMAL" | "INCIDENT" | "REQUIRES_REPORT"; resultContent: string; incidentNote: string | null }) => request<GuardShift>(`/guard-shifts/${id}/result`, { method: "POST", body: JSON.stringify(body) }),
  },
  schedules: (kind: "work-schedules" | "area-monitoring") => ({
    list: (mine = false) => { const path = `/${kind}${query({ size: 100, mine })}`; return cachedRequest<Page<WorkSchedule>>(path, 30_000); },
    get: (id: number) => cachedRequest<WorkSchedule>(`/${kind}/${id}`, 30_000),
    create: (body: ScheduleInput) => request<WorkSchedule>(`/${kind}`, { method: "POST", body: JSON.stringify(body) }),
    update: (id: number, body: ScheduleInput) => request<WorkSchedule>(`/${kind}/${id}`, { method: "PUT", body: JSON.stringify(body) }),
    publish: (id: number) => request<WorkSchedule>(`/${kind}/${id}/publish`, { method: "POST" }),
    remove: (id: number) => request<void>(`/${kind}/${id}`, { method: "DELETE" }),
    addItem: (id: number, body: WorkItemInput) => request<WorkItem>(`/${kind}/${id}/items`, { method: "POST", body: JSON.stringify(body) }),
    updateItem: (id: number, itemId: number, body: WorkItemInput) => request<WorkItem>(`/${kind}/${id}/items/${itemId}`, { method: "PUT", body: JSON.stringify(body) }),
    removeItem: (id: number, itemId: number) => request<void>(`/${kind}/${id}/items/${itemId}`, { method: "DELETE" }),
  }),
};

export function vietnamToday() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
}

export function weekRange(date = vietnamToday()) {
  const value = new Date(`${date}T12:00:00+07:00`);
  const day = value.getUTCDay();
  value.setUTCDate(value.getUTCDate() - (day === 0 ? 6 : day - 1));
  const format = (item: Date) => item.toISOString().slice(0, 10);
  const from = format(value);
  value.setUTCDate(value.getUTCDate() + 6);
  return { from, to: format(value) };
}
