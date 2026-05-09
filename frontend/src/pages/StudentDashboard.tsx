import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { StudentMap } from "../components/Map/StudentMap";
import { ScoreChip } from "../components/ScoreChip";

export default function StudentDashboard() {
  const POLL = { pollMs: 8000 };
  const faculties = useApi(() => api.faculties(), [], POLL);
  const segments = useApi(() => api.segments(), [], POLL);
  const hotspots = useApi(() => api.hotspots(), [], POLL);
  const leaderboard = useApi(() => api.leaderboard(), [], POLL);
  const [selected, setSelected] = useState<string | undefined>(undefined);

  const myFaculty = selected
    ? faculties.data?.find((f) => f.shortCode === selected)
    : undefined;

  const myRank = useMemo(() => {
    if (!leaderboard.data || !myFaculty) return null;
    return leaderboard.data.find((e) => e.shortCode === myFaculty.shortCode)?.rank;
  }, [leaderboard.data, myFaculty]);

  const toggle = (code: string) =>
    setSelected((s) => (s === code ? undefined : code));

  return (
    <div className="container">
      {/* ── Hero card ── */}
      <section className="card" style={{ overflow: "hidden", padding: 0 }}>
        {/* Title */}
        <div
          style={{
            padding: "24px 28px 20px",
            background: "linear-gradient(120deg, #fffbeb 0%, #fef3c7 100%)",
            borderBottom: "1px solid #f1e9d5",
          }}
        >
          <span
            style={{
              fontSize: 11,
              fontWeight: 600,
              letterSpacing: "0.1em",
              textTransform: "uppercase",
              color: "#92400e",
            }}
          >
            🎓 Leuven Go · student crew
          </span>
          <h1 style={{ marginTop: 6, fontSize: 28 }}>
            {myFaculty
              ? `${myFaculty.emoji} ${myFaculty.name}`
              : "Pick your clan, claim your streets"}
          </h1>
          {myFaculty && myRank && (
            <div style={{ marginTop: 6, fontSize: 14, color: "#78350f", fontWeight: 500 }}>
              Rank #{myRank} · {myFaculty.points} pts
            </div>
          )}
        </div>

        {/* ── Clan picker scroller ── */}
        <div
          style={{
            overflowX: "auto",
            display: "flex",
            gap: 8,
            padding: "14px 28px",
            scrollbarWidth: "none",
            borderBottom: "1px solid #f3f4f6",
          }}
        >
          {(faculties.data ?? []).map((f) => {
            const active = selected === f.shortCode;
            return (
              <button
                key={f.id}
                onClick={() => toggle(f.shortCode)}
                style={{
                  flexShrink: 0,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: 5,
                  padding: "10px 14px",
                  borderRadius: 12,
                  border: `2px solid ${active ? f.color : "#e5e7eb"}`,
                  background: active ? f.color + "18" : "white",
                  cursor: "pointer",
                  transition: "border-color 0.12s, background 0.12s",
                  outline: "none",
                  minWidth: 60,
                }}
              >
                <span style={{ fontSize: 20, lineHeight: 1 }}>{f.emoji}</span>
                <span
                  style={{
                    fontSize: 10,
                    fontWeight: 700,
                    letterSpacing: "0.06em",
                    color: active ? f.color : "#9ca3af",
                  }}
                >
                  {f.shortCode}
                </span>
              </button>
            );
          })}
        </div>

        {/* ── Stats ── */}
        <div
          className="grid"
          style={{
            gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
            padding: "16px 28px 20px",
            gap: 12,
          }}
        >
          <Stat label="Clan rank" value={myRank ? `#${myRank}` : "—"} sub={myFaculty?.name ?? "select a clan"} />
          <Stat label="Clan points" value={myFaculty?.points ?? 0} sub="spend in rewards" />
          <Stat
            label="Active hotspots"
            value={(hotspots.data ?? []).filter((h) => h.status !== "RESOLVED").length}
            sub="across Leuven"
          />
          <Stat
            label="Cleanest street"
            value={
              [...(segments.data ?? [])].sort((a, b) => b.aiCleanlinessScore - a.aiCleanlinessScore)[0]
                ?.name ?? "—"
            }
            sub="goals for everyone"
          />
        </div>
      </section>

      {/* ── Map + rankings ── */}
      <section
        className="grid"
        style={{ gridTemplateColumns: "minmax(0, 1.6fr) minmax(0, 1fr)", marginTop: 16 }}
      >
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Faculty territories</div>
              <h3 style={{ fontSize: 18, marginTop: 4 }}>
                {myFaculty
                  ? `${myFaculty.emoji} ${myFaculty.name}'s patch`
                  : "Tap a clan to highlight their patch"}
              </h3>
            </div>
            <Link to="/student/leaderboard" className="btn btn-ghost btn-sm">
              Full leaderboard →
            </Link>
          </div>
          <StudentMap
            faculties={faculties.data ?? []}
            segments={segments.data ?? []}
            hotspots={hotspots.data ?? []}
            selectedFaculty={selected}
          />
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">Clan rankings</div>
          </div>
          <div className="stack">
            {(leaderboard.data ?? []).map((e) => (
              <div
                key={e.id}
                className={`lb-row${e.shortCode === selected ? " me" : ""}`}
                onClick={() => toggle(e.shortCode)}
                style={{
                  border: `1px solid ${e.shortCode === selected ? e.color : "var(--border)"}`,
                  cursor: "pointer",
                }}
              >
                <span className={`lb-rank${e.rank <= 3 ? " top" : ""}`}>
                  {e.rank <= 3 ? ["🥇", "🥈", "🥉"][e.rank - 1] : e.rank}
                </span>
                <span className="lb-emoji">{e.emoji}</span>
                <div>
                  <div style={{ fontWeight: 700 }}>{e.name}</div>
                  <div className="muted" style={{ fontSize: 12 }}>Clan {e.shortCode}</div>
                </div>
                <span className="lb-points" style={{ color: e.color }}>{e.points}</span>
              </div>
            ))}
            {(leaderboard.data?.length ?? 0) === 0 && (
              <div className="muted">No clans yet</div>
            )}
          </div>
        </div>
      </section>

      {/* ── Streets table ── */}
      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-header">
          <div>
            <div className="card-title">Streets to clean up</div>
            <h3 style={{ fontSize: 18, marginTop: 4 }}>Lowest scoring spots near you</h3>
          </div>
          <Link to="/market" className="btn btn-accent btn-sm">🛍 Spend points</Link>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Street</th>
              <th>Held by</th>
              <th>Reports (72h)</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {[...(segments.data ?? [])]
              .sort((a, b) => a.aiCleanlinessScore - b.aiCleanlinessScore)
              .slice(0, 6)
              .map((s) => (
                <tr key={s.id}>
                  <td>
                    <strong>{s.name}</strong>
                    <div className="muted" style={{ fontSize: 12 }}>{s.district}</div>
                  </td>
                  <td>
                    {s.facultyName ? (
                      <span style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
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
                  <td>{s.reportCount30d}</td>
                  <td>
                    <ScoreChip score={s.aiCleanlinessScore ?? 0} />
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function Stat({ label, value, sub }: { label: string; value: string | number; sub: string }) {
  return (
    <div
      style={{
        background: "white",
        borderRadius: 12,
        padding: 14,
        border: "1px solid #e5e7eb",
      }}
    >
      <div className="card-title" style={{ fontSize: 11 }}>{label}</div>
      <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{value}</div>
      <div className="muted" style={{ fontSize: 12 }}>{sub}</div>
    </div>
  );
}
