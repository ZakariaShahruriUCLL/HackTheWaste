import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";
import { ScoreChip } from "../components/ScoreChip";
import type { TrashPhotoDto } from "../api/types";

export default function LandingPage() {
  const POLL = { pollMs: 10000 };
  const { data: stats } = useApi(() => api.stats(), [], POLL);
  const { data: leaderboard } = useApi(() => api.leaderboard(), [], POLL);
  const top = leaderboard?.[0];

  const [feedPhotos, setFeedPhotos] = useState<TrashPhotoDto[]>([]);
  const [feedLoading, setFeedLoading] = useState(true);

  useEffect(() => {
    api.feed(0, 6).then((r) => {
      setFeedPhotos(r.content);
      setFeedLoading(false);
    }).catch(() => setFeedLoading(false));
  }, []);

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

      {/* Live feed */}
      <section style={{ marginTop: 48 }}>
        <div className="spread" style={{ marginBottom: 16, alignItems: "flex-end" }}>
          <div>
            <h2 style={{ fontSize: 22, margin: 0 }}>Latest reports</h2>
            <p className="muted" style={{ fontSize: 13, marginTop: 4 }}>
              Real-time trash reports from across Leuven
            </p>
          </div>
          <Link to="/feed" style={{ fontSize: 14, color: "var(--primary)", fontWeight: 600 }}>
            See all →
          </Link>
        </div>

        {feedLoading ? (
          <div className="feed-grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="feed-card feed-skeleton" />
            ))}
          </div>
        ) : feedPhotos.length === 0 ? (
          <div className="card" style={{ textAlign: "center", padding: 40, color: "var(--fg-soft)" }}>
            <div style={{ fontSize: 40, marginBottom: 10 }}>🗑</div>
            <div style={{ fontWeight: 600 }}>No reports yet</div>
            <div style={{ fontSize: 13, marginTop: 4 }}>
              Submit a report via WhatsApp to see photos here.
            </div>
          </div>
        ) : (
          <div className="feed-grid">
            {feedPhotos.map((p) => (
              <LandingPhotoCard key={p.id} photo={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function LandingPhotoCard({ photo: p }: { photo: TrashPhotoDto }) {
  const [imgError, setImgError] = useState(false);

  return (
    <div className="feed-card">
      {p.photoUrl && !imgError ? (
        <img
          src={p.photoUrl}
          alt={`Trash report at ${p.segmentName ?? "Leuven"}`}
          className="feed-photo"
          onError={() => setImgError(true)}
          loading="lazy"
        />
      ) : (
        <div className="feed-placeholder">🗑</div>
      )}

      <div className="feed-meta">
        <div className="spread" style={{ alignItems: "flex-start" }}>
          <div style={{ minWidth: 0 }}>
            {p.facultyShortCode && (
              <span
                className="feed-clan"
                style={{ background: p.facultyColor ?? "#e5e7eb", color: "white" }}
              >
                {p.facultyEmoji} {p.facultyShortCode}
              </span>
            )}
            <div className="feed-username">{p.username}</div>
            {p.segmentName && (
              <div style={{ fontSize: 12, color: "var(--fg-soft)", marginTop: 2 }}>
                📍 {p.segmentName}
              </div>
            )}
          </div>
          {p.cleanlinessScore != null ? (
            <ScoreChip score={p.cleanlinessScore} />
          ) : (
            <span style={{ fontSize: 10, color: "var(--fg-soft)", border: "1px dashed var(--border)", borderRadius: 6, padding: "2px 6px" }}>
              AI pending
            </span>
          )}
        </div>
        {p.tags && (
          <div style={{ marginTop: 8, display: "flex", flexWrap: "wrap", gap: 4 }}>
            {p.tags.split(",").map((t) => (
              <span key={t} className="feed-tag">#{t.trim()}</span>
            ))}
          </div>
        )}
        <div style={{ marginTop: 8, fontSize: 11, color: "var(--fg-soft)" }}>
          {timeAgo(p.reportedAt)}
        </div>
      </div>
    </div>
  );
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
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
