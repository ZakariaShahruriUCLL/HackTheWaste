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
    return leaderboard.data.find((e) => e.shortCode === myFaculty.shortCode)
      ?.rank;
  }, [leaderboard.data, myFaculty]);

  return (
    <div className="container">
      <section className="card" style={{ overflow: "hidden", padding: 0 }}>
        <div
          style={{
            padding: "26px 28px",
            background:
              "linear-gradient(120deg, #fde68a 0%, #fcd34d 50%, #fb923c 100%)",
          }}
        >
          <span
            style={{
              fontSize: 11,
              fontWeight: 600,
              letterSpacing: "0.1em",
              textTransform: "uppercase",
              opacity: 0.7,
            }}
          >
            🎓 Leuven Go · student crew
          </span>
          <h1 style={{ marginTop: 6, fontSize: 28 }}>
            {myFaculty
              ? `Welcome, ${myFaculty.emoji} ${myFaculty.name}`
              : "Pick your clan, claim your streets"}
          </h1>
          <div className="row" style={{ gap: 8, marginTop: 14, flexWrap: "wrap" }}>
            {(faculties.data ?? []).map((f) => (
              <button
                key={f.id}
                onClick={() => setSelected(f.shortCode)}
                className="btn btn-sm"
                style={{
                  background: selected === f.shortCode ? f.color : "white",
                  color: selected === f.shortCode ? "white" : "#1f2937",
                  border: `1px solid ${f.color}`,
                  fontWeight: 600,
                }}
              >
                {f.emoji} {f.shortCode}
              </button>
            ))}
            <Link to="/report" className="btn btn-primary btn-sm">
              📸 Report a spot
            </Link>
          </div>
        </div>

        <div
          className="grid"
          style={{
            gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
            padding: "20px 28px",
            gap: 12,
          }}
        >
          <Stat
            label="Your clan rank"
            value={myRank ? `#${myRank}` : "—"}
            sub={myFaculty?.name ?? "select your clan"}
          />
          <Stat
            label="Clan points"
            value={myFaculty?.points ?? 0}
            sub="convert to rewards"
          />
          <Stat
            label="Active hotspots"
            value={
              (hotspots.data ?? []).filter((h) => h.status !== "RESOLVED")
                .length
            }
            sub="across Leuven"
          />
          <Stat
            label="Cleanest segment"
            value={
              [...(segments.data ?? [])]
                .sort((a, b) => b.aiCleanlinessScore - a.aiCleanlinessScore)[0]
                ?.name ?? "—"
            }
            sub="goals for everyone else"
          />
        </div>
      </section>

      <section
        className="grid"
        style={{
          gridTemplateColumns: "minmax(0, 1.6fr) minmax(0, 1fr)",
          marginTop: 16,
        }}
      >
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Faculty territories</div>
              <h3 style={{ fontSize: 18, marginTop: 4 }}>
                Tap a clan above to highlight their patch
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
                style={{
                  border: `1px solid ${e.shortCode === selected ? e.color : "var(--border)"}`,
                }}
              >
                <span className={`lb-rank${e.rank <= 3 ? " top" : ""}`}>
                  {e.rank <= 3 ? ["🥇", "🥈", "🥉"][e.rank - 1] : e.rank}
                </span>
                <span className="lb-emoji">{e.emoji}</span>
                <div>
                  <div style={{ fontWeight: 700 }}>{e.name}</div>
                  <div className="muted" style={{ fontSize: 12 }}>
                    Clan {e.shortCode}
                  </div>
                </div>
                <span className="lb-points" style={{ color: e.color }}>
                  {e.points}
                </span>
              </div>
            ))}
            {(leaderboard.data?.length ?? 0) === 0 && (
              <div className="muted">No clans yet</div>
            )}
          </div>
        </div>
      </section>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="card-header">
          <div>
            <div className="card-title">Streets to clean up</div>
            <h3 style={{ fontSize: 18, marginTop: 4 }}>
              Lowest scoring spots near you
            </h3>
          </div>
          <Link to="/market" className="btn btn-accent btn-sm">
            🛍 Spend points
          </Link>
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
                    <div className="muted" style={{ fontSize: 12 }}>
                      {s.district}
                    </div>
                  </td>
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

function Stat({
  label,
  value,
  sub,
}: {
  label: string;
  value: string | number;
  sub: string;
}) {
  return (
    <div
      style={{
        background: "white",
        borderRadius: 14,
        padding: 14,
        border: "1px solid #f1e9d5",
      }}
    >
      <div className="card-title" style={{ fontSize: 11 }}>
        {label}
      </div>
      <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>{value}</div>
      <div className="muted" style={{ fontSize: 12 }}>
        {sub}
      </div>
    </div>
  );
}
