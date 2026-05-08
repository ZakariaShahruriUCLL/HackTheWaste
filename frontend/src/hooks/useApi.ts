import { useCallback, useEffect, useRef, useState } from "react";

export interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export interface UseApiOptions {
  /** If set, re-runs the loader every N ms (e.g. live-polling for new
   *  WhatsApp / in-app reports). Subsequent fetches don't toggle `loading`. */
  pollMs?: number;
}

export function useApi<T>(
  loader: () => Promise<T>,
  deps: unknown[] = [],
  options: UseApiOptions = {},
): AsyncState<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cancelledRef = useRef(false);
  const hasLoadedRef = useRef(false);

  const fetchOnce = useCallback(async () => {
    if (!hasLoadedRef.current) setLoading(true);
    try {
      const r = await loader();
      if (!cancelledRef.current) {
        setData(r);
        setError(null);
      }
    } catch (e) {
      if (!cancelledRef.current)
        setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      if (!cancelledRef.current) {
        setLoading(false);
        hasLoadedRef.current = true;
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    cancelledRef.current = false;
    hasLoadedRef.current = false;
    void fetchOnce();
    let timer: ReturnType<typeof setInterval> | null = null;
    if (options.pollMs && options.pollMs > 0) {
      timer = setInterval(() => {
        if (document.visibilityState === "visible") void fetchOnce();
      }, options.pollMs);
    }
    return () => {
      cancelledRef.current = true;
      if (timer) clearInterval(timer);
    };
  }, [fetchOnce, options.pollMs]);

  return { data, loading, error, refetch: () => void fetchOnce() };
}
