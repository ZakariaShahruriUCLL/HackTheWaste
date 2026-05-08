import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";

export default function LeaderboardPage() {
  const board = useApi(() => api.leaderboard(), []);
  const max = Math.max(1, ...(board.data ?? []).map((e) => e.points));

  return (
    <div className="container">
      <div className="spread">
        <div>
          <h1 style={{ fontSize: 28 }}>Faculty clans · Leuven Go</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            Every report scores points for your faculty. Cleaner streets win.
          </div>
        </div>
        <Link to="/report" className="btn btn-accent btn-sm">
          + Score for your clan
        </Link>
      </div>

      <section className="card" style={{ marginTop: 16 }}>
        <div className="stack">
          {(board.data ?? []).map((e, i) => (
            <div
              key={e.id}
              style={{
                display: "grid",
                gridTemplateColumns: "60px 60px 1fr 90px",
                alignItems: "center",
                gap: 16,
                padding: "12px 14px",
                borderRadius: 16,
                background:
                  i === 0
                    ? "linear-gradient(120deg, #fef3c7, #fde68a)"
                    : "var(--bg-soft)",
              }}
            >
              <div
                style={{
                  fontSize: 22,
                  fontWeight: 800,
                  textAlign: "center",
                  color: i < 3 ? "#b45309" : "var(--fg-soft)",
                }}
              >
                {i < 3 ? ["🥇", "🥈", "🥉"][i] : `#${e.rank}`}
              </div>
              <div style={{ fontSize: 32, textAlign: "center" }}>{e.emoji}</div>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{e.name}</div>
                <div className="muted" style={{ fontSize: 12 }}>
                  Clan {e.shortCode}
                </div>
                <div
                  style={{
                    marginTop: 8,
                    height: 6,
                    background: "rgba(0,0,0,0.06)",
                    borderRadius: 4,
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      width: `${(e.points / max) * 100}%`,
                      height: "100%",
                      background: e.color,
                    }}
                  />
                </div>
              </div>
              <div
                style={{
                  fontSize: 22,
                  fontWeight: 800,
                  color: e.color,
                  textAlign: "right",
                }}
              >
                {e.points}
                <div className="muted" style={{ fontSize: 11, fontWeight: 500 }}>
                  pts
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
