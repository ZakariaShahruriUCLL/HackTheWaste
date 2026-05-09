import { useEffect, useState } from "react";
import { MapContainer, TileLayer, ScaleControl } from "react-leaflet";
import "./leafletSetup";
import {
  LEUVEN_BOUNDS,
  LEUVEN_CENTER,
  LEUVEN_MAX_ZOOM,
  LEUVEN_MIN_ZOOM,
} from "./leafletSetup";
import { PredictiveHeatmapLayer } from "./PredictiveHeatmapLayer";
import type { PredictPoint } from "../../api/types";

const ML_URL = import.meta.env.VITE_ML_URL ?? "/ml";
const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function toIso(dayOffset: number, hour: number): string {
  const d = new Date();
  const currentDow = (d.getDay() + 6) % 7; // 0 = Mon
  d.setDate(d.getDate() + ((dayOffset - currentDow + 7) % 7));
  d.setHours(hour, 0, 0, 0);
  return (
    d.getFullYear() +
    "-" + String(d.getMonth() + 1).padStart(2, "0") +
    "-" + String(d.getDate()).padStart(2, "0") +
    "T" + String(hour).padStart(2, "0") + ":00:00"
  );
}

interface Props {
  height?: number;
}

export function PredictiveMap({ height = 540 }: Props) {
  const today = (new Date().getDay() + 6) % 7;
  const [day, setDay]   = useState(today);
  const [hour, setHour] = useState(new Date().getHours());
  const [points, setPoints] = useState<PredictPoint[]>([]);
  const [loading, setLoading]   = useState(true);
  const [mlOffline, setMlOffline] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setMlOffline(false);

    fetch(`${ML_URL}/predict/grid`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        time: toIso(day, hour),
        lat_steps: 35,
        lng_steps: 35,
      }),
    })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data: { points: PredictPoint[] }) => {
        if (!cancelled) {
          setPoints(data.points ?? []);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setMlOffline(true);
          setLoading(false);
        }
      });

    return () => { cancelled = true; };
  }, [day, hour]);

  const peak = points.length ? Math.max(...points.map((p) => p.likelihood)) : 0;
  const peakPct = Math.round(peak * 100);
  const peakColor =
    peak > 0.65 ? "#dc2626"
    : peak > 0.4 ? "#f97316"
    : peak > 0.2 ? "#eab308"
    : "#22c55e";

  return (
    <div>
      {/* Time scrubber */}
      <div className="predictive-controls">
        <div className="predictive-day-row">
          {DAYS.map((d, i) => (
            <button
              key={d}
              className={`predictive-day-btn${day === i ? " active" : ""}`}
              onClick={() => setDay(i)}
            >
              {d}
            </button>
          ))}
        </div>

        <div className="predictive-hour-row">
          <span className="predictive-hour-label">
            {String(hour).padStart(2, "0")}:00
          </span>
          <input
            type="range"
            min={0}
            max={23}
            step={1}
            value={hour}
            onChange={(e) => setHour(Number(e.target.value))}
            className="predictive-slider"
          />
          <span className="predictive-status">
            {loading ? (
              <span className="predictive-loading">Predicting…</span>
            ) : mlOffline ? (
              <span className="predictive-error">
                ML server offline — start it first
              </span>
            ) : (
              <span>
                Peak: <strong style={{ color: peakColor }}>{peakPct}%</strong>
              </span>
            )}
          </span>
        </div>
      </div>

      {/* Map */}
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
          {!mlOffline && <PredictiveHeatmapLayer points={points} />}
        </MapContainer>

        <div className="legend">
          <div style={{ fontWeight: 600, marginBottom: 6, fontSize: 12 }}>
            Trash likelihood
          </div>
          {[
            { color: "#dc2626", label: "Very high" },
            { color: "#f97316", label: "High" },
            { color: "#eab308", label: "Moderate" },
            { color: "#84cc16", label: "Low" },
            { color: "#22c55e", label: "Clean" },
          ].map(({ color, label }) => (
            <div key={label} className="legend-row">
              <div className="legend-swatch" style={{ background: color }} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
