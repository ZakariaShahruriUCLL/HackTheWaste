import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { LEUVEN_CENTER } from "../components/Map/leafletSetup";

const TAGS = [
  "litter",
  "overflow",
  "graffiti",
  "broken-glass",
  "leaves",
  "packaging",
  "vomit",
  "cigarettes",
];

const RATING_DESC = [
  "Filthy",
  "Bad",
  "Messy",
  "Okay",
  "Good",
  "Pristine",
];

interface Coords {
  lat: number;
  lng: number;
}

export default function ReportPage() {
  const navigate = useNavigate();
  const faculties = useApi(() => api.faculties(), []);

  const [rating, setRating] = useState<number>(2);
  const [note, setNote] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [facultyCode, setFacultyCode] = useState<string>("");
  const [coords, setCoords] = useState<Coords>({
    lat: LEUVEN_CENTER[0],
    lng: LEUVEN_CENTER[1],
  });
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  // Auto-pick a default faculty once faculties load
  useEffect(() => {
    if (!facultyCode && faculties.data?.length) {
      setFacultyCode(faculties.data[0].shortCode);
    }
  }, [faculties.data, facultyCode]);

  const pseudoId = useMemo(() => {
    let id = sessionStorage.getItem("lg-pseudo");
    if (!id) {
      id = "pid-" + Math.random().toString(36).slice(2, 10);
      sessionStorage.setItem("lg-pseudo", id);
    }
    return id;
  }, []);

  const useGps = () => {
    if (!navigator.geolocation) {
      setError("Geolocation not available");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => setCoords({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => setError("Could not get your location - using Leuven centre"),
      { enableHighAccuracy: true, timeout: 8000 },
    );
  };

  const onPhoto = (file: File | null) => {
    if (!file) {
      setPhotoPreview(null);
      return;
    }
    const reader = new FileReader();
    reader.onload = () => setPhotoPreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const toggleTag = (tag: string) => {
    setTags((t) => (t.includes(tag) ? t.filter((x) => x !== tag) : [...t, tag]));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      // Image is sent as a fingerprint, not raw bytes (GDPR-friendly: only derived metric).
      const imageRef = photoPreview
        ? `local-photo-${await sha256(photoPreview)}`
        : undefined;
      await api.submitReport({
        lat: coords.lat,
        lng: coords.lng,
        rating,
        note,
        imageRef,
        signalTags: tags.join(","),
        facultyShortCode: facultyCode || undefined,
        reporterPseudoId: pseudoId,
      });
      navigate("/student?reported=1");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Submit failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 760 }}>
      <div className="spread" style={{ marginBottom: 12 }}>
        <div>
          <h1 style={{ fontSize: 26 }}>One-message report</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            Photo + location → an actionable hotspot in seconds.
          </div>
        </div>
        <Link to="/student" className="btn btn-ghost btn-sm">
          Back
        </Link>
      </div>

      <form className="card" onSubmit={submit}>
        <div className="form-grid">
          <div>
            <label>1 · How clean is this spot? ({RATING_DESC[rating]})</label>
            <div className="rating-stars" style={{ marginTop: 8 }}>
              {[0, 1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  type="button"
                  className="star-btn"
                  aria-pressed={rating === n}
                  onClick={() => setRating(n)}
                >
                  {n}
                </button>
              ))}
            </div>
            <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
              0 = filthy, 5 = sparkling
            </div>
          </div>

          <div>
            <label>2 · Snap a photo (optional)</label>
            <div
              className="dropzone"
              onClick={() => fileRef.current?.click()}
              style={{ cursor: "pointer", marginTop: 8 }}
            >
              {photoPreview ? (
                <img
                  src={photoPreview}
                  alt="preview"
                  style={{
                    maxWidth: "100%",
                    maxHeight: 220,
                    borderRadius: 10,
                  }}
                />
              ) : (
                <>
                  📸 Tap to attach a photo
                  <div style={{ fontSize: 12, marginTop: 6 }}>
                    Stored as a derived hash only - no raw image is sent to
                    backend.
                  </div>
                </>
              )}
            </div>
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              capture="environment"
              hidden
              onChange={(e) => onPhoto(e.target.files?.[0] ?? null)}
            />
          </div>

          <div>
            <label>3 · Where is it?</label>
            <div className="row" style={{ gap: 8, marginTop: 8 }}>
              <input
                type="number"
                step="0.0001"
                value={coords.lat}
                onChange={(e) =>
                  setCoords((c) => ({ ...c, lat: Number(e.target.value) }))
                }
              />
              <input
                type="number"
                step="0.0001"
                value={coords.lng}
                onChange={(e) =>
                  setCoords((c) => ({ ...c, lng: Number(e.target.value) }))
                }
              />
              <button type="button" className="btn btn-ghost btn-sm" onClick={useGps}>
                📍 Use GPS
              </button>
            </div>
          </div>

          <div>
            <label>4 · What did you see?</label>
            <div
              className="row"
              style={{ flexWrap: "wrap", gap: 6, marginTop: 8 }}
            >
              {TAGS.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => toggleTag(t)}
                  className="btn btn-ghost btn-sm"
                  style={{
                    background: tags.includes(t) ? "var(--primary)" : "white",
                    color: tags.includes(t) ? "white" : "var(--fg)",
                    borderColor: tags.includes(t)
                      ? "var(--primary)"
                      : "var(--border)",
                  }}
                >
                  #{t}
                </button>
              ))}
            </div>
            <textarea
              style={{ marginTop: 10 }}
              placeholder="Add an optional note (e.g. 'overflowing bin near tram stop')"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

          <div>
            <label>5 · Score it for your clan</label>
            <select
              style={{ marginTop: 8 }}
              value={facultyCode}
              onChange={(e) => setFacultyCode(e.target.value)}
            >
              {(faculties.data ?? []).map((f) => (
                <option key={f.id} value={f.shortCode}>
                  {f.emoji} {f.name}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <div
              style={{
                color: "var(--danger)",
                fontSize: 14,
                fontWeight: 600,
              }}
            >
              {error}
            </div>
          )}

          <div className="row" style={{ justifyContent: "space-between" }}>
            <div className="muted" style={{ fontSize: 12 }}>
              session id: <code>{pseudoId}</code> · GDPR-safe pseudonym
            </div>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Sending…" : "🚀 Submit report"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

async function sha256(s: string): Promise<string> {
  const enc = new TextEncoder().encode(s);
  const buf = await crypto.subtle.digest("SHA-256", enc);
  return Array.from(new Uint8Array(buf))
    .slice(0, 8)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}
