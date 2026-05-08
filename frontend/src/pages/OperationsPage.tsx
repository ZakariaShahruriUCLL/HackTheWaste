import { useState } from "react";
import { api } from "../api/client";
import { useApi } from "../hooks/useApi";
import { StatusTag } from "../components/StatusTag";
import { HotspotMap } from "../components/Map/HotspotMap";

export default function OperationsPage() {
  const orders = useApi(() => api.workOrders(), []);
  const hotspots = useApi(() => api.activeHotspots(), []);
  const [busyId, setBusyId] = useState<number | null>(null);

  const complete = async (id: number) => {
    setBusyId(id);
    try {
      await api.completeWorkOrder(id);
      orders.refetch();
      hotspots.refetch();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="container">
      <div className="spread" style={{ marginBottom: 16 }}>
        <div>
          <h1 style={{ fontSize: 28 }}>Crew operations queue</h1>
          <div className="muted" style={{ marginTop: 4 }}>
            Dispatched via Planon · routed by district + severity
          </div>
        </div>
      </div>

      <section
        className="grid"
        style={{
          gridTemplateColumns: "minmax(0, 1.2fr) minmax(0, 1fr)",
          gap: 16,
        }}
      >
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Active hotspots</div>
              <h3 style={{ fontSize: 18, marginTop: 4 }}>
                Targeted routing for cleaning crews & machinery
              </h3>
            </div>
          </div>
          <HotspotMap hotspots={hotspots.data ?? []} variant="pro" height={420} />
        </div>

        <div className="card">
          <div className="card-title">Work order queue</div>
          <div className="stack" style={{ marginTop: 12 }}>
            {(orders.data ?? []).map((w) => (
              <div
                key={w.id}
                style={{
                  border: "1px solid var(--pro-border)",
                  borderRadius: 12,
                  padding: 14,
                }}
              >
                <div className="spread">
                  <div>
                    <div
                      style={{
                        fontFamily: "ui-monospace, Menlo, monospace",
                        fontWeight: 700,
                      }}
                    >
                      {w.planonRef ?? `WO-${w.id}`}
                    </div>
                    <div className="muted" style={{ fontSize: 12 }}>
                      {new Date(w.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <div className="row" style={{ gap: 6 }}>
                    <StatusTag value={w.priority} />
                    <StatusTag value={w.status} />
                  </div>
                </div>
                <div style={{ marginTop: 8, fontSize: 14 }}>{w.summary}</div>
                <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
                  Crew: {w.crew}
                </div>
                {w.status !== "COMPLETED" && (
                  <button
                    className="btn btn-primary btn-sm"
                    style={{ marginTop: 10 }}
                    disabled={busyId === w.id}
                    onClick={() => complete(w.id)}
                  >
                    {busyId === w.id ? "Marking..." : "Mark completed"}
                  </button>
                )}
              </div>
            ))}
            {(orders.data?.length ?? 0) === 0 && (
              <div className="muted">No active work orders.</div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
