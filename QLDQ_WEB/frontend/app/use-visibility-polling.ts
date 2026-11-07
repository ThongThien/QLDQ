"use client";

import { useEffect, useRef } from "react";

export const DATA_SYNC_INTERVAL_MS = 45_000;

type RefreshCallback = () => void | Promise<unknown>;

export function useVisibilityPolling(
  refresh: RefreshCallback,
  enabled = true,
  intervalMs = DATA_SYNC_INTERVAL_MS,
) {
  const refreshRef = useRef(refresh);
  const inFlightRef = useRef(false);
  const lastStartedAtRef = useRef(0);

  useEffect(() => {
    refreshRef.current = refresh;
  }, [refresh]);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    let disposed = false;
    const run = async () => {
      if (
        disposed ||
        document.visibilityState !== "visible" ||
        inFlightRef.current ||
        Date.now() - lastStartedAtRef.current < 1_000
      ) {
        return;
      }

      inFlightRef.current = true;
      lastStartedAtRef.current = Date.now();
      try {
        await refreshRef.current();
      } finally {
        inFlightRef.current = false;
      }
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void run();
      }
    };
    const handleFocus = () => void run();
    const intervalId = window.setInterval(() => void run(), intervalMs);

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("focus", handleFocus);

    return () => {
      disposed = true;
      window.clearInterval(intervalId);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("focus", handleFocus);
    };
  }, [enabled, intervalMs]);
}
