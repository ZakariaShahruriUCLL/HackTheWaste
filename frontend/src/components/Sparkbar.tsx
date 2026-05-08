import type { TimeBucket } from "../api/types";

export function Sparkbar({ data }: { data: TimeBucket[] }) {
  if (!data?.length) return <div className="muted">No data</div>;
  const max = Math.max(1, ...data.map((d) => d.count));
  return (
    <div className="sparkbar">
      {data.map((d) => (
        <div
          key={d.label}
          style={{ height: `${(d.count / max) * 100}%` }}
          title={`${d.label}: ${d.count}`}
        >
          <small>{d.label}</small>
        </div>
      ))}
    </div>
  );
}
