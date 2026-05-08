import type {
  CityConfig,
  CreateReportPayload,
  FacultyDto,
  HotspotDto,
  LeaderboardEntry,
  ReportDto,
  RewardItemDto,
  StatsDto,
  StreetSegmentDto,
  WorkOrderDto,
} from "./types";

const BASE = "/api";

async function request<T>(
  path: string,
  init?: RequestInit & { json?: unknown },
): Promise<T> {
  const headers = new Headers(init?.headers ?? {});
  let body: BodyInit | undefined = init?.body as BodyInit | undefined;
  if (init?.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.json);
  }
  const res = await fetch(`${BASE}${path}`, { ...init, headers, body });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  city: () => request<CityConfig>("/stats/city"),
  stats: () => request<StatsDto>("/stats"),
  reports: () => request<ReportDto[]>("/reports/recent"),
  submitReport: (p: CreateReportPayload) =>
    request<ReportDto>("/reports", { method: "POST", json: p }),
  hotspots: () => request<HotspotDto[]>("/hotspots"),
  activeHotspots: () => request<HotspotDto[]>("/hotspots/active"),
  segments: () => request<StreetSegmentDto[]>("/segments"),
  faculties: () => request<FacultyDto[]>("/faculties"),
  leaderboard: () => request<LeaderboardEntry[]>("/leaderboard"),
  workOrders: () => request<WorkOrderDto[]>("/work-orders"),
  completeWorkOrder: (id: number) =>
    request<WorkOrderDto>(`/work-orders/${id}/complete`, { method: "POST" }),
  dispatchWorkOrder: (hotspotId: number) =>
    request<WorkOrderDto>(`/work-orders/dispatch/${hotspotId}`, {
      method: "POST",
    }),
  rewards: () => request<RewardItemDto[]>("/rewards"),
};
