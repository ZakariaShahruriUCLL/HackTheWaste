export interface ReportDto {
  id: number;
  lat: number;
  lng: number;
  rating: number;
  note?: string;
  imageRef?: string;
  signalTags?: string;
  faculty?: string;
  facultyColor?: string;
  hotspotId?: number;
  reportedAt: string;
}

export interface HotspotDto {
  id: number;
  lat: number;
  lng: number;
  severity: number;
  reportCount: number;
  status: "OPEN" | "ESCALATED" | "DISPATCHED" | "RESOLVED";
  label?: string;
  segmentName?: string;
  createdAt: string;
  lastUpdatedAt: string;
}

export interface StreetSegmentDto {
  id: number;
  name: string;
  district?: string;
  lat: number;
  lng: number;
  aiCleanlinessScore: number;
  reportCount30d: number;
  facultyShortCode?: string;
  facultyName?: string;
  facultyColor?: string;
  lastEvaluatedAt: string;
}

export interface FacultyDto {
  id: number;
  name: string;
  shortCode: string;
  color: string;
  emoji: string;
  members: number;
  points: number;
  territoryGeoJson?: string;
}

export interface LeaderboardEntry {
  id: number;
  name: string;
  shortCode: string;
  color: string;
  emoji: string;
  points: number;
  rank: number;
}

export interface WorkOrderDto {
  id: number;
  planonRef?: string;
  hotspotId?: number;
  status: "PENDING" | "DISPATCHED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  crew?: string;
  summary?: string;
  lat?: number;
  lng?: number;
  createdAt: string;
  dispatchedAt?: string;
  completedAt?: string;
}

export interface RewardItemDto {
  id: number;
  title: string;
  description: string;
  sponsor: string;
  imageRef?: string;
  category: string;
  costPoints: number;
  stock: number;
}

export interface TimeBucket {
  label: string;
  count: number;
}

export interface StatsDto {
  totalReports: number;
  openHotspots: number;
  dispatchedOrders: number;
  completedOrders: number;
  avgCleanliness: number;
  reportsLast7Days: TimeBucket[];
  topHotspots: HotspotDto[];
}

export interface CityConfig {
  name: string;
  centerLat: number;
  centerLng: number;
  clusterRadiusM: number;
  workOrderThreshold: number;
}

export interface CreateReportPayload {
  lat: number;
  lng: number;
  rating: number;
  note?: string;
  imageRef?: string;
  signalTags?: string;
  facultyShortCode?: string;
  reporterPseudoId?: string;
}
