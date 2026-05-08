import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import LandingPage from "./pages/LandingPage";
import ProfessionalDashboard from "./pages/ProfessionalDashboard";
import StudentDashboard from "./pages/StudentDashboard";
import ReportPage from "./pages/ReportPage";
import MarketplacePage from "./pages/MarketplacePage";
import OperationsPage from "./pages/OperationsPage";
import LeaderboardPage from "./pages/LeaderboardPage";
import { TopBar } from "./components/TopBar";

export default function App() {
  const location = useLocation();
  const path = location.pathname;
  const themeClass = path.startsWith("/pro")
    ? "theme-pro"
    : path.startsWith("/student") ||
        path.startsWith("/report") ||
        path.startsWith("/market") ||
        path.startsWith("/leaderboard")
      ? "theme-student"
      : "";

  return (
    <div className={`app-shell ${themeClass}`}>
      <TopBar />
      <main style={{ flex: 1 }}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/pro" element={<ProfessionalDashboard />} />
          <Route path="/pro/operations" element={<OperationsPage />} />
          <Route path="/student" element={<StudentDashboard />} />
          <Route path="/student/leaderboard" element={<LeaderboardPage />} />
          <Route path="/report" element={<ReportPage />} />
          <Route path="/market" element={<MarketplacePage />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  );
}
