import { api, type WorkItem } from "./api-client";

export async function confirmWorkAssignmentConflicts(items: WorkItem[]) {
  const assignedItems = items.filter((item) => item.assignedSoldiers.length > 0);
  const responses = await Promise.all(assignedItems.map((item) => {
    const allDay = item.allDay || item.period === "ALL_DAY";
    return api.assignmentValidation.work({
      workDate: item.workDate,
      startTime: allDay ? null : item.startTime?.slice(0, 5) ?? null,
      endTime: allDay ? null : item.endTime?.slice(0, 5) ?? null,
      allDay,
      soldierIds: item.assignedSoldiers.map((soldier) => soldier.id),
      excludeWorkItemId: item.id > 0 ? item.id : undefined,
    });
  }));
  const messages = responses.flatMap((response) => response.warnings.map((warning) => warning.message));
  messages.push(...localConflicts(assignedItems));
  const unique = [...new Set(messages)];
  if (!unique.length) return true;
  return window.confirm(`Phát hiện ${unique.length} xung đột phân công:\n\n${unique.map((message) => `• ${message}`).join("\n")}\n\nBạn vẫn muốn lưu lịch này?`);
}

function localConflicts(items: WorkItem[]) {
  const messages: string[] = [];
  for (let firstIndex = 0; firstIndex < items.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < items.length; secondIndex += 1) {
      const first = items[firstIndex];
      const second = items[secondIndex];
      if (first.workDate !== second.workDate || !overlaps(first, second)) continue;
      const secondSoldiers = new Map(second.assignedSoldiers.map((soldier) => [soldier.id, soldier.fullName]));
      first.assignedSoldiers.forEach((soldier) => {
        if (secondSoldiers.has(soldier.id)) {
          messages.push(`${soldier.fullName} được phân công hai nhiệm vụ trùng thời gian ngày ${formatDate(first.workDate)}.`);
        }
      });
    }
  }
  return messages;
}

function overlaps(first: WorkItem, second: WorkItem) {
  const firstRange = minutes(first);
  const secondRange = minutes(second);
  return firstRange.start < secondRange.end && firstRange.end > secondRange.start;
}

function minutes(item: WorkItem) {
  if (item.allDay || item.period === "ALL_DAY") return { start: 0, end: 24 * 60 };
  return { start: toMinutes(item.startTime), end: toMinutes(item.endTime) };
}

function toMinutes(value: string | null) {
  if (!value) return 0;
  const [hour, minute] = value.split(":").map(Number);
  return hour * 60 + minute;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })
    .format(new Date(`${value}T00:00:00+07:00`));
}
