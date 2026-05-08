interface Props {
  value: string | undefined;
}

const MAP: Record<string, string> = {
  OPEN: "tag tag-warning",
  ESCALATED: "tag tag-danger",
  DISPATCHED: "tag tag-info",
  IN_PROGRESS: "tag tag-info",
  PENDING: "tag tag-warning",
  COMPLETED: "tag tag-success",
  RESOLVED: "tag tag-success",
  FAILED: "tag tag-danger",
  URGENT: "tag tag-danger",
  HIGH: "tag tag-warning",
  MEDIUM: "tag tag-info",
  LOW: "tag",
};

export function StatusTag({ value }: Props) {
  if (!value) return <span className="tag">—</span>;
  return <span className={MAP[value] ?? "tag"}>{value}</span>;
}
