import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";

export default function MarketplacePage() {
  const rewards = useApi(() => api.rewards(), []);
  const board = useApi(() => api.leaderboard(), []);
  const [category, setCategory] = useState("ALL");

  const categories = useMemo(() => {
    const set = new Set<string>();
    rewards.data?.forEach((r) => set.add(r.category));
    return ["ALL", ...Array.from(set).sort()];
  }, [rewards.data]);

  const visible = (rewards.data ?? []).filter(
    (r) => category === "ALL" || r.category === category,
  );

  const top = board.data?.[0];

  return (
    <div className="container">
      <div className="spread" style={{ flexWrap: "wrap", gap: 12 }}>
        <div>
          <h1 style={{ fontSize: 28 }}>Reward marketplace</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            Burn faculty points on real Leuven perks. Refreshed weekly.
          </div>
        </div>
        <div className="row">
          {top && (
            <div
              className="card"
              style={{ padding: "10px 14px", display: "flex", gap: 10 }}
            >
              <div style={{ fontSize: 22 }}>{top.emoji}</div>
              <div>
                <div style={{ fontSize: 11, color: "var(--fg-soft)" }}>
                  Leading clan
                </div>
                <div style={{ fontWeight: 700 }}>
                  {top.shortCode} · {top.points} pts
                </div>
              </div>
            </div>
          )}
          <Link to="/student" className="btn btn-ghost btn-sm">
            Back to map
          </Link>
        </div>
      </div>

      <div className="tabbar" style={{ marginTop: 16 }}>
        {categories.map((c) => (
          <button
            key={c}
            className={c === category ? "active" : ""}
            onClick={() => setCategory(c)}
          >
            {c === "ALL" ? "Everything" : c}
          </button>
        ))}
      </div>

      <section
        className="grid"
        style={{
          gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
          marginTop: 16,
        }}
      >
        {visible.map((r) => (
          <article key={r.id} className="card">
            <div
              style={{
                height: 140,
                borderRadius: 12,
                background: gradientFor(r.id),
                display: "grid",
                placeItems: "center",
                fontSize: 56,
                color: "white",
              }}
            >
              {iconFor(r.category)}
            </div>
            <div className="spread" style={{ marginTop: 14 }}>
              <span className="tag">{r.category}</span>
              <span style={{ fontSize: 12, color: "var(--fg-soft)" }}>
                stock {r.stock}
              </span>
            </div>
            <h3 style={{ fontSize: 18, marginTop: 8 }}>{r.title}</h3>
            <p className="muted" style={{ fontSize: 13, marginTop: 6 }}>
              {r.description}
            </p>
            <div className="spread" style={{ marginTop: 14 }}>
              <span style={{ fontSize: 13, color: "var(--fg-soft)" }}>
                by {r.sponsor}
              </span>
              <button className="btn btn-accent btn-sm">
                Redeem · {r.costPoints} pts
              </button>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}

function gradientFor(seed: number): string {
  const palettes = [
    "linear-gradient(135deg, #f59e0b, #fb923c)",
    "linear-gradient(135deg, #2563eb, #38bdf8)",
    "linear-gradient(135deg, #16a34a, #22d3ee)",
    "linear-gradient(135deg, #9333ea, #ec4899)",
    "linear-gradient(135deg, #f43f5e, #fb7185)",
    "linear-gradient(135deg, #0891b2, #14b8a6)",
  ];
  return palettes[seed % palettes.length];
}

function iconFor(category: string): string {
  const map: Record<string, string> = {
    Drinks: "🍺",
    Food: "🍱",
    Transport: "🚲",
    Study: "📚",
    Entertainment: "🎟",
    Experience: "🏛",
    Merch: "👕",
  };
  return map[category] ?? "🎁";
}
