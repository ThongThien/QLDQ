export type ScheduleStatus = "DRAFT" | "PUBLISHED" | "COMPLETED" | "CANCELLED";

export type GuardRecord = {
  condition: "Bình thường" | "Có sự việc" | "Cần báo cáo";
  content: string;
  incident: string;
  submittedAt: string;
};

export type GuardSchedule = {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  location: string;
  assignedSoldiers: string[];
  status: ScheduleStatus;
  note: string;
  acknowledged: boolean;
  guardRecord?: GuardRecord;
  startAt: string;
  endAt: string;
  managementStatus: "DRAFT" | "PUBLISHED" | "CANCELLED";
  occurrenceStatus: "UPCOMING" | "ONGOING" | "COMPLETED";
  soldierIds: number[];
  assignments: Array<{
    soldierId: number;
    username: string;
    fullName: string;
    acknowledgedAt: string | null;
    status: "ASSIGNED" | "COMPLETED";
    resultCondition: "NORMAL" | "INCIDENT" | "REQUIRES_REPORT" | null;
    resultContent: string | null;
    incidentNote: string | null;
    completedAt: string | null;
  }>;
  hasIncident: boolean;
};

export type CurrentUser = "COMMANDER" | string;

export type View = "overview" | "schedules" | "soldiers";
