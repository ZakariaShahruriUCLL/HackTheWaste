export function ScoreChip({ score }: { score: number }) {
  const v = score ?? 0;
  const bg =
    v >= 80
      ? "#16a34a"
      : v >= 60
        ? "#f59e0b"
        : v >= 40
          ? "#f97316"
          : "#dc2626";
  return (
    <span className="score-chip" style={{ background: bg }}>
      {v.toFixed(0)}
    </span>
  );
}
