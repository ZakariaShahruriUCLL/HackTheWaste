import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";
import { api } from "../api/client";

export default function SignupPage() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const { data: faculties } = useApi(() => api.faculties(), []);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [clan, setClan] = useState<string | undefined>(undefined);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    try {
      await register(email, password, clan);
      navigate(-1);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Sign up failed";
      if (msg.startsWith("409")) setError("Email is already registered");
      else if (msg.startsWith("400")) setError(msg.replace(/^\d{3}\s\w+\s*/, ""));
      else setError(msg);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 480, marginTop: 60 }}>
      <div className="card" style={{ padding: 32 }}>
        <h2 style={{ marginBottom: 6 }}>Create account</h2>
        <p className="muted" style={{ marginBottom: 24, fontSize: 14 }}>
          Join Leuven Go and start earning points for your clan
        </p>

        <form onSubmit={handleSubmit} className="form-grid">
          <div>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
              placeholder="At least 6 characters"
            />
          </div>

          <div>
            <label htmlFor="confirm">Confirm password</label>
            <input
              id="confirm"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              placeholder="Repeat your password"
            />
          </div>

          <div>
            <label style={{ display: "block", marginBottom: 8 }}>
              Clan{" "}
              <span className="muted" style={{ fontWeight: 400 }}>
                (optional — you can change this later)
              </span>
            </label>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {(faculties ?? []).map((f) => (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setClan((c) => (c === f.shortCode ? undefined : f.shortCode))}
                  className="btn btn-sm"
                  style={{
                    background: clan === f.shortCode ? f.color : "white",
                    color: clan === f.shortCode ? "white" : "#1f2937",
                    border: `1px solid ${f.color}`,
                    fontWeight: 600,
                  }}
                >
                  {f.emoji} {f.shortCode}
                </button>
              ))}
            </div>
            {clan && (
              <p style={{ marginTop: 8, fontSize: 13, color: "var(--fg-soft)" }}>
                Selected: <strong>{faculties?.find((f) => f.shortCode === clan)?.name}</strong>
              </p>
            )}
          </div>

          {/* Data consent */}
          <div
            style={{
              border: "1px solid var(--border)",
              borderRadius: 10,
              padding: "14px 16px",
              background: "var(--bg-elev, #f9fafb)",
            }}
          >
            <p style={{ fontWeight: 600, fontSize: 14, marginBottom: 8 }}>
              Data consent
            </p>
            <ul
              style={{
                fontSize: 13,
                color: "var(--fg-soft)",
                paddingLeft: 18,
                marginBottom: 12,
                lineHeight: 1.6,
              }}
            >
              <li>Your <strong>email and clan</strong> are stored to personalise your experience and credit your reports to the right team.</li>
              <li>Reports you submit (location, photos) are <strong>publicly visible</strong> in the Leuven Go feed and shared with Leuven city operations.</li>
              <li>Submitted photos are <strong>analysed by AI</strong> (Google Gemini) to generate a cleanliness score.</li>
              <li>Anonymised data may be used for <strong>research and predictive mapping</strong> to improve city cleanliness.</li>
              <li>You can request deletion of your account and data at any time.</li>
            </ul>
            <label style={{ display: "flex", alignItems: "flex-start", gap: 10, cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={consent}
                onChange={(e) => setConsent(e.target.checked)}
                style={{ marginTop: 2, flexShrink: 0, width: 16, height: 16, cursor: "pointer" }}
              />
              <span style={{ fontSize: 13 }}>
                I have read and agree to the data use described above.
              </span>
            </label>
          </div>

          {error && (
            <div
              style={{
                padding: "10px 14px",
                background: "#fee2e2",
                color: "#991b1b",
                borderRadius: 10,
                fontSize: 14,
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || !consent}
            style={{ width: "100%", justifyContent: "center", marginTop: 4 }}
          >
            {loading ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p style={{ marginTop: 20, textAlign: "center", fontSize: 14, color: "var(--fg-soft)" }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "var(--primary)", fontWeight: 600 }}>
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
