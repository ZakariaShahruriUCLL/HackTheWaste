import type {
  AuthUser,
  CityConfig,
  CreateReportPayload,
  FacultyDto,
  FeedPage,
  HotspotDto,
  LeaderboardEntry,
  ReportDto,
  RewardItemDto,
  StatsDto,
  StreetSegmentDto,
  WorkOrderDto,
} from "./types";

const BASE = "/api";
const AUTH_KEY = "auth_user";

function storedToken(): string | null {
  try {
    const raw = localStorage.getItem(AUTH_KEY);
    return raw ? (JSON.parse(raw) as AuthUser).token : null;
  } catch {
    return null;
  }
}

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
  const token = storedToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(`${BASE}${path}`, { ...init, headers, body });
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try {
      const err = (await res.json()) as { message?: string };
      if (err.message) message = err.message;
    } catch { /* use default */ }
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  register: (email: string, password: string, facultyShortCode?: string) =>
    request<AuthUser>("/auth/register", { method: "POST", json: { email, password, facultyShortCode, consentGiven: true } }),
  login: (email: string, password: string) =>
    request<AuthUser>("/auth/login", { method: "POST", json: { email, password } }),
  me: () => request<AuthUser>("/auth/me"),
  logout: () => request<void>("/auth/logout", { method: "POST" }),

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
  feed: (page = 0, size = 12, clan?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (clan) params.set("clan", clan);
    return request<FeedPage>(`/feed?${params}`);
  },
};
