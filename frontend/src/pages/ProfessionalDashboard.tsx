import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { HotspotMap } from "../components/Map/HotspotMap";
import { PredictiveMap } from "../components/Map/PredictiveMap";
import { Sparkbar } from "../components/Sparkbar";
import { ScoreChip } from "../components/ScoreChip";
import { StatusTag } from "../components/StatusTag";

export default function ProfessionalDashboard() {
  const POLL = { pollMs: 5000 };
  const stats = useApi(() => api.stats(), [], POLL);
  const hotspots = useApi(() => api.hotspots(), [], POLL);
  const segments = useApi(() => api.segments(), [], POLL);
  const orders = useApi(() => api.workOrders(), [], POLL);
  const [district, setDistrict] = useState<string>("ALL");
  const [mapMode, setMapMode] = useState<"live" | "predictive">("predictive");

  const districts = useMemo(() => {
    const set = new Set<string>();
    segments.data?.forEach((s) => s.district && set.add(s.district));
    return ["ALL", ...Array.from(set)];
  }, [segments.data]);

  const filteredSegments = useMemo(() => {
    if (!segments.data) return [];
    if (district === "ALL") return segments.data;
    return segments.data.filter((s) => s.district === district);
  }, [segments.data, district]);

  return (
    <div className="container">
      <div className="spread" style={{ marginBottom: 16 }}>
        <div>
          <h1 style={{ fontSize: 28 }}>Operations Overview</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            Live cleanliness signals across Leuven · last refresh{" "}
            {new Date().toLocaleTimeString()}
          </div>
        </div>
        <div className="row">
          <div className="tabbar">
            {districts.map((d) => (
              <button
                key={d}
                className={d === district ? "active" : ""}
                onClick={() => setDistrict(d)}
              >
                {d === "ALL" ? "All districts" : d}
              </button>
            ))}
          </div>
          <Link to="/pro/operations" className="btn btn-ghost btn-sm">
            Operations queue
          </Link>
        </div>
      </div>

      <section
        className="grid"
        style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}
      >
        <Kpi
          label="Total reports (72h)"
          value={stats.data?.totalReports ?? "—"}
          sub="anonymized signals"
        />
        <Kpi
          label="Active hotspots"
          value={stats.data?.openHotspots ?? "—"}
          sub="post-clustering"
        />
        <Kpi
          label="Planon orders dispatched"
          value={stats.data?.dispatchedOrders ?? "—"}
          sub={`${stats.data?.completedOrders ?? 0} completed`}
        />
        <Kpi
          label="City AI cleanliness"
          value={stats.data?.avgCleanliness?.toFixed(1) ?? "—"}
          sub="0–100 weighted score"
        />
      </section>

      {/* ── Main map card (full width) ── */}
      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-header">
          <div>
            <div className="card-title">
              {mapMode === "live" ? "Live hotspot map" : "Predictive heatmap · Random Forest ML"}
            </div>
            <h3 style={{ fontSize: 18, marginTop: 4 }}>
              {mapMode === "live"
                ? "Where to send crews now"
                : "Where trash will accumulate — pick a day & time"}
            </h3>
          </div>
          <div className="tabbar">
            <button
              className={mapMode === "predictive" ? "active" : ""}
              onClick={() => setMapMode("predictive")}
            >
              Predictive
            </button>
            <button
              className={mapMode === "live" ? "active" : ""}
              onClick={() => setMapMode("live")}
            >
              Live hotspots
            </button>
          </div>
        </div>
        {mapMode === "live" ? (
          <HotspotMap hotspots={hotspots.data ?? []} height={560} />
        ) : (
          <PredictiveMap height={560} />
        )}
      </section>

      {/* ── Secondary row: trend + top clusters ── */}
      <section
        className="grid"
        style={{ marginTop: 16, gridTemplateColumns: "minmax(0, 1.6fr) minmax(0, 1fr)" }}
      >
        <div className="card">
          <div className="card-header">
            <div className="card-title">Reports trend (7d)</div>
            <span className="muted" style={{ fontSize: 12 }}>ingested signals</span>
          </div>
          <Sparkbar data={stats.data?.reportsLast7Days ?? []} />
        </div>

        <div className="card">
          <div className="card-title">Top clusters</div>
          <div className="stack" style={{ marginTop: 8 }}>
            {(stats.data?.topHotspots ?? []).slice(0, 5).map((h) => (
              <div key={h.id} className="spread">
                <div>
                  <div style={{ fontWeight: 600 }}>{h.label ?? "Hotspot"}</div>
                  <div className="muted" style={{ fontSize: 12 }}>
                    {h.reportCount} reports · severity {h.severity?.toFixed(1)}
                  </div>
                </div>
                <StatusTag value={h.status} />
              </div>
            ))}
            {(stats.data?.topHotspots?.length ?? 0) === 0 && (
              <div className="muted">All clear ✨</div>
            )}
          </div>
        </div>
      </section>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-header">
          <div>
            <div className="card-title">AI cleanliness scores by segment</div>
            <h3 style={{ fontSize: 18, marginTop: 4 }}>
              Streets ranked by current need
            </h3>
          </div>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Segment</th>
              <th>District</th>
              <th>Reports (72h)</th>
              <th>Held by</th>
              <th>AI score</th>
            </tr>
          </thead>
          <tbody>
            {[...filteredSegments]
              .sort((a, b) => a.aiCleanlinessScore - b.aiCleanlinessScore)
              .map((s) => (
                <tr key={s.id}>
                  <td>
                    <strong>{s.name}</strong>
                  </td>
                  <td className="muted">{s.district ?? "—"}</td>
                  <td>{s.reportCount30d ?? 0}</td>
                  <td>
                    {s.facultyName ? (
                      <span
                        style={{
                          display: "inline-flex",
                          gap: 6,
                          alignItems: "center",
                        }}
                      >
                        <span
                          style={{
                            width: 10,
                            height: 10,
                            borderRadius: 3,
                            background: s.facultyColor ?? "#ccc",
                          }}
                        />
                        {s.facultyName}
                      </span>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  <td>
                    <ScoreChip score={s.aiCleanlinessScore ?? 0} />
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </section>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-header">
          <div>
            <div className="card-title">Recent Planon work orders</div>
            <h3 style={{ fontSize: 18, marginTop: 4 }}>
              Auto-dispatched from clustered reports
            </h3>
          </div>
          <Link to="/pro/operations" className="btn btn-ghost btn-sm">
            See all
          </Link>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Planon ref</th>
              <th>Summary</th>
              <th>Crew</th>
              <th>Priority</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {(orders.data ?? []).slice(0, 6).map((w) => (
              <tr key={w.id}>
                <td style={{ fontFamily: "ui-monospace, Menlo, monospace" }}>
                  {w.planonRef ?? "PENDING"}
                </td>
                <td>{w.summary}</td>
                <td>{w.crew ?? "—"}</td>
                <td>
                  <StatusTag value={w.priority} />
                </td>
                <td>
                  <StatusTag value={w.status} />
                </td>
              </tr>
            ))}
            {(orders.data?.length ?? 0) === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No orders yet - report something to see the pipeline fire.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function Kpi({
  label,
  value,
  sub,
}: {
  label: string;
  value: string | number;
  sub: string;
}) {
  return (
    <div className="card">
      <div className="card-title">{label}</div>
      <div className="kpi-value">{value}</div>
      <div className="kpi-sub">{sub}</div>
    </div>
  );
}
