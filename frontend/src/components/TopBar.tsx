import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function TopBar() {
  const { pathname } = useLocation();
  const mode: "landing" | "pro" | "student" = pathname.startsWith("/pro")
    ? "pro"
    : pathname === "/" || pathname === "" || pathname === "/login" || pathname === "/signup"
      ? "landing"
      : "student";

  return (
    <header className="topbar">
      <NavLink to="/" className="brand">
        <span className="brand-mark">LG</span>
        <span>Leuven Go</span>
      </NavLink>

      <div className="row" style={{ gap: 6 }}>
        {mode === "landing" && (
          <nav className="nav">
            <NavLink to="/pro">For city teams</NavLink>
            <NavLink to="/student">For students</NavLink>
            <NavLink to="/feed">Feed</NavLink>
            <NavLink to="/report" className="btn btn-primary btn-sm">
              Report a spot
            </NavLink>
          </nav>
        )}

        {mode === "pro" && (
          <nav className="nav">
            <NavLink to="/pro" end>Overview</NavLink>
            <NavLink to="/pro/operations">Operations</NavLink>
            <NavLink to="/student" className="btn btn-ghost btn-sm">Switch view</NavLink>
          </nav>
        )}

        {mode === "student" && (
          <nav className="nav">
            <NavLink to="/student" end>Map</NavLink>
            <NavLink to="/student/leaderboard">Clans</NavLink>
            <NavLink to="/market">Rewards</NavLink>
            <NavLink to="/feed">Feed</NavLink>
            <NavLink to="/pro" className="btn btn-ghost btn-sm">Switch view</NavLink>
            <NavLink to="/report" className="btn btn-accent btn-sm">+ Report</NavLink>
          </nav>
        )}

        <AuthArea />
      </div>
    </header>
  );
}

function AuthArea() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    return (
      <div className="topbar-auth">
        <NavLink to="/login" className="btn btn-ghost btn-sm">Log in</NavLink>
        <NavLink to="/signup" className="btn btn-primary btn-sm">Sign up</NavLink>
      </div>
    );
  }

  return (
    <div className="topbar-auth">
      <span className="topbar-user">
        {user.facultyShortCode ? `${user.facultyShortCode} · ` : ""}
        {user.email.split("@")[0]}
      </span>
      <button
        className="btn btn-ghost btn-sm"
        onClick={() => void logout().then(() => navigate("/"))}
      >
        Log out
      </button>
    </div>
  );
}
