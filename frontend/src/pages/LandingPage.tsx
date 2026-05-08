import { Link } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";

export default function LandingPage() {
  const { data: stats } = useApi(() => api.stats(), []);
  const { data: leaderboard } = useApi(() => api.leaderboard(), []);
  const top = leaderboard?.[0];

  return (
    <div className="container landing">
      <section className="landing-hero">
        <span className="landing-eyebrow">Stad Leuven · Hack the Waste 2026</span>
        <h1>
          One photo. One tap.
          <br />A cleaner Leuven.
        </h1>
        <p>Cleanliness signals from the street, dispatched to the right Planon crew.</p>
      </section>

      <section className="landing-stats card">
        <Stat label="Reports" value={stats?.totalReports ?? "—"} />
        <Sep />
        <Stat label="Live hotspots" value={stats?.openHotspots ?? "—"} />
        <Sep />
        <Stat
          label="City avg cleanliness"
          value={stats?.avgCleanliness?.toFixed(1) ?? "—"}
          suffix="/100"
        />
        <Sep />
        <Stat
          label="Top clan"
          value={
            top ? (
              <span>
                {top.emoji} {top.shortCode}
              </span>
            ) : (
              "—"
            )
          }
          sub={top ? `${top.points} pts` : undefined}
        />
      </section>

      <section className="landing-choice">
        <Link to="/pro" className="landing-card landing-card-pro">
          <div className="landing-tag">For city teams</div>
          <h2>Personnel</h2>
          <p>
            Heatmap, AI cleanliness scores per street segment, and Planon work-order
            monitoring.
          </p>
          <span className="landing-arrow">→</span>
        </Link>

        <Link to="/student" className="landing-card landing-card-student">
          <div className="landing-tag">For students</div>
          <h2>Students</h2>
          <p>
            Pick your clan, claim your streets, climb the 24 Uur leaderboard, redeem
            rewards.
          </p>
          <span className="landing-arrow">→</span>
        </Link>
      </section>
    </div>
  );
}

function Stat({
  label,
  value,
  sub,
  suffix,
}: {
  label: string;
  value: React.ReactNode;
  sub?: string;
  suffix?: string;
}) {
  return (
    <div className="landing-stat">
      <div className="landing-stat-label">{label}</div>
      <div className="landing-stat-value">
        {value}
        {suffix && <span className="landing-stat-suffix">{suffix}</span>}
      </div>
      {sub && <div className="landing-stat-sub">{sub}</div>}
    </div>
  );
}

function Sep() {
  return <div className="landing-stat-sep" aria-hidden />;
}
