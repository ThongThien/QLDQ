export const LOCATIONS = ["Cổng chính"];
export const WEEKLY_SHIFT_TIMES = ["07:00–09:00", "09:00–11:00", "11:00–13:00", "13:00–15:00", "15:00–17:00", "17:00–19:00", "19:00–21:00", "21:00–23:00", "23:00–01:00", "01:00–03:00", "03:00–05:00", "05:00–07:00"];

export type WeeklyRosterDay = {
  date: string;
  dutyOfficer: string;
  assignments: string[];
  slots?: Array<{ id: number | null; status: "completed" | "incident" | "upcoming"; label: string; note: string; location: string }>;
};

export const STATUS_LABEL = { DRAFT: "Nháp", PUBLISHED: "Đã công bố", COMPLETED: "Đã hoàn thành", CANCELLED: "Đã hủy" } as const;
export const formatDate = (date: string) => new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", weekday: "long", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(`${date}T12:00:00+07:00`));
export const shortDate = (date: string) => date ? new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", timeZone: "Asia/Ho_Chi_Minh" }).format(new Date(`${date}T12:00:00+07:00`)) : "—";
