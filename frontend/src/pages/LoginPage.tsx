import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? "/student";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Login failed";
      if (msg.startsWith("401")) setError("Invalid email or password");
      else setError(msg);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 60 }}>
      <div className="card" style={{ padding: 32 }}>
        <h2 style={{ marginBottom: 6 }}>Welcome back</h2>
        <p className="muted" style={{ marginBottom: 24, fontSize: 14 }}>
          Log in to your Leuven Go account
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
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
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
            disabled={loading}
            style={{ width: "100%", justifyContent: "center", marginTop: 4 }}
          >
            {loading ? "Logging in…" : "Log in"}
          </button>
        </form>

        <p style={{ marginTop: 20, textAlign: "center", fontSize: 14, color: "var(--fg-soft)" }}>
          No account yet?{" "}
          <Link to="/signup" style={{ color: "var(--primary)", fontWeight: 600 }}>
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
