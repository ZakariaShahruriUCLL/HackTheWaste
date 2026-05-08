import { useMemo } from "react";
import {
  CircleMarker,
  MapContainer,
  Polygon,
  Popup,
  ScaleControl,
  TileLayer,
  Tooltip,
} from "react-leaflet";
import "./leafletSetup";
import {
  LEUVEN_BOUNDS,
  LEUVEN_CENTER,
  LEUVEN_MAX_ZOOM,
  LEUVEN_MIN_ZOOM,
} from "./leafletSetup";
import type {
  FacultyDto,
  HotspotDto,
  StreetSegmentDto,
} from "../../api/types";

interface Props {
  faculties: FacultyDto[];
  segments: StreetSegmentDto[];
  hotspots: HotspotDto[];
  selectedFaculty?: string;
}

interface Polygon3D {
  coordinates: number[][][];
}

function polygonToLatLng(geo: string): [number, number][] | null {
  try {
    const parsed = JSON.parse(geo) as Polygon3D;
    if (!parsed.coordinates?.[0]) return null;
    return parsed.coordinates[0].map(
      (c) => [c[1], c[0]] as [number, number],
    );
  } catch {
    return null;
  }
}

function scoreColor(score: number): string {
  if (score >= 80) return "#16a34a";
  if (score >= 60) return "#f59e0b";
  if (score >= 40) return "#f97316";
  return "#dc2626";
}

export function StudentMap({
  faculties,
  segments,
  hotspots,
  selectedFaculty,
}: Props) {
  const territories = useMemo(
    () =>
      faculties
        .map((f) => ({
          faculty: f,
          coords: f.territoryGeoJson
            ? polygonToLatLng(f.territoryGeoJson)
            : null,
        }))
        .filter((t) => t.coords && t.coords.length >= 3),
    [faculties],
  );

  return (
    <div className="map-shell" style={{ height: 560 }}>
      <MapContainer
        center={LEUVEN_CENTER}
        zoom={14}
        minZoom={LEUVEN_MIN_ZOOM}
        maxZoom={LEUVEN_MAX_ZOOM}
        maxBounds={LEUVEN_BOUNDS}
        maxBoundsViscosity={0.9}
        scrollWheelZoom
        style={{ height: "100%", width: "100%" }}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; CARTO'
          subdomains="abcd"
          maxZoom={LEUVEN_MAX_ZOOM}
        />
        <ScaleControl position="bottomleft" imperial={false} />

        {territories.map(({ faculty, coords }) => {
          const dimmed =
            selectedFaculty && selectedFaculty !== faculty.shortCode;
          return (
            <Polygon
              key={faculty.id}
              positions={coords as [number, number][]}
              pathOptions={{
                color: faculty.color,
                fillColor: faculty.color,
                fillOpacity: dimmed ? 0.06 : 0.18,
                weight: dimmed ? 1 : 2,
              }}
            >
              <Tooltip sticky>
                <strong>
                  {faculty.emoji} {faculty.name}
                </strong>
                <br />
                {faculty.points} pts · {faculty.members} students
              </Tooltip>
            </Polygon>
          );
        })}

        {segments.map((s) => (
          <CircleMarker
            key={s.id}
            center={[s.lat, s.lng]}
            radius={10}
            pathOptions={{
              color: "white",
              weight: 2,
              fillColor: scoreColor(s.aiCleanlinessScore),
              fillOpacity: 0.95,
            }}
          >
            <Popup>
              <strong>{s.name}</strong>
              <br />
              Score: {s.aiCleanlinessScore?.toFixed(1)} / 100
              <br />
              Held by: {s.facultyName ?? "—"}
              <br />
              Reports (recent): {s.reportCount30d}
            </Popup>
          </CircleMarker>
        ))}

        {hotspots
          .filter((h) => h.status !== "RESOLVED")
          .map((h) => (
            <CircleMarker
              key={`h${h.id}`}
              center={[h.lat, h.lng]}
              radius={Math.min(20, 6 + (h.reportCount ?? 1) * 2)}
              pathOptions={{
                color: "#7c2d12",
                weight: 1,
                dashArray: "4 3",
                fillColor: "#fb923c",
                fillOpacity: 0.5,
              }}
            >
              <Popup>
                <strong>🔥 {h.label}</strong>
                <br />
                {h.reportCount} reports merged
                <br />
                Severity {h.severity?.toFixed(1)} / 5
              </Popup>
            </CircleMarker>
          ))}
      </MapContainer>
      <div className="legend">
        <div style={{ fontWeight: 600, marginBottom: 4 }}>Cleanliness</div>
        <div className="legend-row">
          <div className="legend-swatch" style={{ background: "#16a34a" }} />
          80–100 sparkling
        </div>
        <div className="legend-row">
          <div className="legend-swatch" style={{ background: "#f59e0b" }} />
          60–80 ok
        </div>
        <div className="legend-row">
          <div className="legend-swatch" style={{ background: "#f97316" }} />
          40–60 grimey
        </div>
        <div className="legend-row">
          <div className="legend-swatch" style={{ background: "#dc2626" }} />
          0–40 needs love
        </div>
      </div>
    </div>
  );
}
