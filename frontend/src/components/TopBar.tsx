import { NavLink, useLocation } from "react-router-dom";

export function TopBar() {
  const { pathname } = useLocation();
  const mode: "landing" | "pro" | "student" = pathname.startsWith("/pro")
    ? "pro"
    : pathname === "/" || pathname === ""
      ? "landing"
      : "student";

  return (
    <header className="topbar">
      <NavLink to="/" className="brand">
        <span className="brand-mark">LG</span>
        <span>Leuven Go</span>
      </NavLink>

      {mode === "landing" && (
        <nav className="nav">
          <NavLink to="/pro">For city teams</NavLink>
          <NavLink to="/student">For students</NavLink>
          <NavLink to="/report" className="btn btn-primary btn-sm">
            Report a spot
          </NavLink>
        </nav>
      )}

      {mode === "pro" && (
        <nav className="nav">
          <NavLink to="/pro" end>
            Overview
          </NavLink>
          <NavLink to="/pro/operations">Operations</NavLink>
          <NavLink to="/" className="btn btn-ghost btn-sm">
            Switch view
          </NavLink>
        </nav>
      )}

      {mode === "student" && (
        <nav className="nav">
          <NavLink to="/student" end>
            Map
          </NavLink>
          <NavLink to="/student/leaderboard">Clans</NavLink>
          <NavLink to="/market">Rewards</NavLink>
          <NavLink to="/" className="btn btn-ghost btn-sm">
            Switch view
          </NavLink>
          <NavLink to="/report" className="btn btn-accent btn-sm">
            + Report
          </NavLink>
        </nav>
      )}
    </header>
  );
}
