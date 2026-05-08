import { createContext, useContext, useState, type ReactNode } from "react";
import type { AuthUser } from "../api/types";
import { api } from "../api/client";

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, facultyShortCode?: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

const STORAGE_KEY = "auth_user";

function loadStored(): AuthUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStored);
  const [loading, setLoading] = useState(false);

  function persist(u: AuthUser | null) {
    if (u) localStorage.setItem(STORAGE_KEY, JSON.stringify(u));
    else localStorage.removeItem(STORAGE_KEY);
    setUser(u);
  }

  async function login(email: string, password: string) {
    setLoading(true);
    try {
      persist(await api.login(email, password));
    } finally {
      setLoading(false);
    }
  }

  async function register(email: string, password: string, facultyShortCode?: string) {
    setLoading(true);
    try {
      persist(await api.register(email, password, facultyShortCode));
    } finally {
      setLoading(false);
    }
  }

  async function logout() {
    try { await api.logout(); } catch { /* ignore if token already gone */ }
    persist(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
