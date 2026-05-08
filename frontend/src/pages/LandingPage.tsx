import { Link } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";

export default function LandingPage() {
  const { data: stats } = useApi(() => api.stats(), []);
  const { data: leaderboard } = useApi(() => api.leaderboard(), []);

  return (
    <div className="container">
      <section className="hero">
        <span
          style={{
            display: "inline-block",
            padding: "4px 12px",
            background: "rgba(255,255,255,0.15)",
            borderRadius: 999,
            fontSize: 12,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            fontWeight: 600,
          }}
        >
          Stad Leuven · Hack the Waste 2026
        </span>
        <h1>One photo. One tap. A cleaner Leuven.</h1>
        <p>
          Leuven Go bridges what residents <em>see</em> on the street with what
          city teams <em>do</em> about it. Citizens send a photo + location.
          Our traffic logic merges duplicates into a single hotspot, scores it,
          and dispatches a Planon work order to the right crew - all in one
          flow.
        </p>
        <div className="row" style={{ marginTop: 28 }}>
          <Link to="/student" className="btn btn-accent">
            🎓 I'm a student
          </Link>
          <Link to="/pro" className="btn btn-ghost" style={{ color: "white" }}>
            🏛 City team dashboard
          </Link>
          <Link to="/report" className="btn btn-primary">
            📸 Report something now
          </Link>
        </div>
      </section>

      <section className="role-grid">
        <Link to="/pro" className="role-card role-pro">
          <div className="role-tag">For city operations</div>
          <h3>Professional Dashboard</h3>
          <p>
            Heatmap of active hotspots, AI cleanliness scores per street
            segment, Planon work-order monitoring and crew routing - the same
            pane of glass for planners and dispatchers.
          </p>
          <div className="row" style={{ marginTop: 18, gap: 8 }}>
            <span className="tag tag-info">AI scoring</span>
            <span className="tag">Planon API</span>
            <span className="tag">GDPR-friendly</span>
          </div>
        </Link>

        <Link to="/student" className="role-card role-student">
          <div className="role-tag">For students</div>
          <h3>Student Dashboard</h3>
          <p>
            Map your faculty's territory, claim cleaner streets, climb the clan
            leaderboard, and burn points at the rewards marketplace. Reporting
            is one tap - photo, location, done.
          </p>
          <div className="row" style={{ marginTop: 18, gap: 8 }}>
            <span className="tag tag-warning">Faculty clans</span>
            <span className="tag">0-5 scoring</span>
            <span className="tag tag-success">Rewards</span>
          </div>
        </Link>
      </section>

      <section
        className="grid"
        style={{
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          marginTop: 32,
        }}
      >
        <div className="card">
          <div className="card-title">Reports collected</div>
          <div className="kpi-value">{stats?.totalReports ?? "—"}</div>
          <div className="kpi-sub">across Leuven · last 7 days</div>
        </div>
        <div className="card">
          <div className="card-title">Live hotspots</div>
          <div className="kpi-value">{stats?.openHotspots ?? "—"}</div>
          <div className="kpi-sub">aggregated by traffic logic</div>
        </div>
        <div className="card">
          <div className="card-title">City avg cleanliness</div>
          <div className="kpi-value">
            {stats?.avgCleanliness?.toFixed(1) ?? "—"}
          </div>
          <div className="kpi-sub">0–100 AI score · all segments</div>
        </div>
        <div className="card">
          <div className="card-title">Top clan</div>
          <div className="kpi-value">
            {leaderboard?.[0]?.emoji ?? "🏆"} {leaderboard?.[0]?.shortCode ?? "—"}
          </div>
          <div className="kpi-sub">{leaderboard?.[0]?.points ?? 0} pts</div>
        </div>
      </section>

      <section className="card" style={{ marginTop: 32 }}>
        <div className="card-header">
          <div>
            <h2 style={{ fontSize: 22 }}>How it works</h2>
            <div className="muted" style={{ marginTop: 4 }}>
              From a single photo to a dispatched cleaning crew in under a
              minute.
            </div>
          </div>
        </div>
        <div className="feature-grid">
          {[
            {
              step: "01",
              title: "Snap & send",
              body: "One tap captures the photo and your location. No accounts, no PII - just a rotating session id.",
            },
            {
              step: "02",
              title: "Traffic logic",
              body: "Reports within ~40m collapse into one hotspot. Severity = mean rating, weighted by recency.",
            },
            {
              step: "03",
              title: "AI cleanliness score",
              body: "Each segment gets a live 0-100 score combining citizen ratings and image-based litter signals.",
            },
            {
              step: "04",
              title: "Planon dispatch",
              body: "When a hotspot crosses the threshold, a work order is sent to Planon with priority + crew routing.",
            },
            {
              step: "05",
              title: "Faculty clans",
              body: "Students earn points for their faculty. Cleaner territories climb the leaderboard.",
            },
            {
              step: "06",
              title: "Marketplace",
              body: "Points buy real perks - Velo passes, STUK tickets, Alma vouchers, library credit.",
            },
          ].map((s) => (
            <div className="card" key={s.step}>
              <div className="card-title">step {s.step}</div>
              <h4 style={{ marginTop: 6, fontSize: 16 }}>{s.title}</h4>
              <p
                className="muted"
                style={{ marginTop: 8, fontSize: 14, lineHeight: 1.5 }}
              >
                {s.body}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
