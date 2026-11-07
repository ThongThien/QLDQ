export type ModuleStatus = "DRAFT" | "PUBLISHED";

export type WorkItem = {
  id: number;
  date: string;
  time: string;
  content: string;
  leader: string;
  participants: string;
  location: string;
  preparation: string;
  dutyLeader: string;
  note: string;
};

export type WorkSchedule = {
  id: number;
  weekLabel: string;
  startDate: string;
  endDate: string;
  createdDate: string;
  updatedDate: string;
  creator: string;
  generalNote: string;
  status: ModuleStatus;
  items: WorkItem[];
};

export type AreaShift = {
  soldier1: string;
  soldier2: string;
  time: string;
};

export type AreaDay = {
  date: string;
  morning: AreaShift;
  afternoon: AreaShift;
};

export type AreaSchedule = {
  id: number;
  weekLabel: string;
  startDate: string;
  endDate: string;
  updatedDate: string;
  creator: string;
  status: ModuleStatus;
  note: string;
  days: AreaDay[];
};
