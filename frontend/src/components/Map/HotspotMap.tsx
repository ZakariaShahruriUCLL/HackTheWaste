import {
  CircleMarker,
  MapContainer,
  Popup,
  ScaleControl,
  TileLayer,
} from "react-leaflet";
import "./leafletSetup";
import {
  LEUVEN_BOUNDS,
  LEUVEN_CENTER,
  LEUVEN_MAX_ZOOM,
  LEUVEN_MIN_ZOOM,
} from "./leafletSetup";
import type { HotspotDto } from "../../api/types";

interface Props {
  hotspots: HotspotDto[];
  height?: number;
}

const STATUS_COLOR: Record<HotspotDto["status"], string> = {
  OPEN: "#f97316",
  ESCALATED: "#dc2626",
  DISPATCHED: "#2563eb",
  RESOLVED: "#16a34a",
};

export function HotspotMap({ hotspots, height = 520 }: Props) {
  return (
    <div className="map-shell" style={{ height }}>
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
        {hotspots.map((h) => {
          const radius = Math.min(28, 10 + (h.reportCount ?? 1) * 3);
          const color = STATUS_COLOR[h.status] ?? "#f97316";
          return (
            <CircleMarker
              key={h.id}
              center={[h.lat, h.lng]}
              radius={radius}
              pathOptions={{
                color,
                weight: 1.5,
                fillColor: color,
                fillOpacity: 0.45,
              }}
            >
              <Popup>
                <strong>{h.label ?? h.segmentName ?? "Hotspot"}</strong>
                <br />
                Reports: {h.reportCount}
                <br />
                Severity: {h.severity?.toFixed(1)} / 5
                <br />
                Status: {h.status}
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>
      <div className="legend">
        <div style={{ fontWeight: 600, marginBottom: 4 }}>Hotspot status</div>
        {Object.entries(STATUS_COLOR).map(([s, c]) => (
          <div key={s} className="legend-row">
            <div className="legend-swatch" style={{ background: c }} />
            <span>{s.toLowerCase()}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
