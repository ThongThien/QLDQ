"use client";

import { useEffect, useState } from "react";

interface BusinessClockProps {
  value?: string;
  frozen?: boolean;
  className?: string;
}

const formatter = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

function ClockTicker({ value, frozen = false, className }: BusinessClockProps) {
  const [displayTime, setDisplayTime] = useState<Date | null>(() => value ? new Date(value) : null);
  useEffect(() => {
    if (!value || frozen) {
      return;
    }

    const intervalId = window.setInterval(() => {
      setDisplayTime((current) => current ? new Date(current.getTime() + 1_000) : null);
    }, 1_000);
    return () => window.clearInterval(intervalId);
  }, [frozen, value]);

  if (!displayTime || Number.isNaN(displayTime.getTime())) {
    return null;
  }

  return (
    <time className={className} dateTime={displayTime.toISOString()}>
      {formatter.format(displayTime)}
    </time>
  );
}

export function BusinessClock(props: BusinessClockProps) {
  return <ClockTicker key={`${props.value ?? "empty"}:${Boolean(props.frozen)}`} {...props}/>;
}
